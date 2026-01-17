package se.wikicap.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.wikicap.client.music.SpotifyClient;
import se.wikicap.dto.music.MusicResponse;
import se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse;

import java.util.List;

/**
 * Music aggregation service.
 *
 * Combines:
 * - Rankings (songs + artists) from {@link MusicRankProvider} (e.g. Wikipedia scraping)
 * - Metadata enrichment from {@link SpotifyClient} (Spotify search + artist top tracks)
 *
 * Public API methods return reactive types ({@link Mono}) and are safe to call from
 * WebFlux/Spring MVC controllers.
 */
@Service
public class MusicService {

    private final SpotifyClient spotifyClient;
    private final MusicRankProvider rankProvider;

    /**
     * Creates a new {@link MusicService}.
     *
     * @param spotifyClient   client for fetching music metadata (Spotify)
     * @param rankProvider  provider for fetching ranked songs/artists for a given year
     */
    public MusicService(SpotifyClient spotifyClient, MusicRankProvider rankProvider) {
        this.spotifyClient = spotifyClient;
        this.rankProvider = rankProvider;
    }

    /**
     * Fetches aggregated music data for a year.
     *
     * Flow:
     * 1) Fetch top songs and top artists for {@code year} from {@link #rankProvider}
     * 2) Deduplicate/limit songs and sort/limit artists
     * 3) Enrich each song/artist with Spotify metadata via {@link #spotifyClient}
     *
     * Caching:
     * The result is cached by year. A second request for the same year should return from cache
     * (no external calls) as long as the cache entry is still valid.
     *
     * Error handling:
     * If Spotify APIs fail for an individual item, the item is returned in a fallback "Unknown"
     * form rather than failing the entire year response.
     *
     * @param year the year to fetch music data for
     * @return a {@link Mono} emitting a {@link MusicResponse} containing enriched songs and artists
     */
    @Cacheable(value = "music", key = "#year")
    public Mono<MusicResponse> getMusicByYear(int year) {
        Mono<List<MusicResponse.TrackDTO>> songsMono = rankProvider.getTopSongs(year)
                .map(this::deduplicateAndLimitSongs)
                .flatMapMany(Flux::fromIterable)
                .index() // Maintain order (0, 1, 2...)
                .flatMapSequential(tuple -> enrichSong(tuple.getT2(), tuple.getT1().intValue() + 1))
                .collectList();

        Mono<List<MusicResponse.ArtistDTO>> artistsMono = rankProvider.getTopArtists(year)
                .map(this::sortAndLimitArtists)
                .flatMapMany(Flux::fromIterable)
                .index()
                .flatMapSequential(tuple -> enrichArtist(tuple.getT2(), tuple.getT1().intValue() + 1))
                .collectList();

        return Mono.zip(songsMono, artistsMono)
                .map(tuple -> new MusicResponse(year, tuple.getT1(), tuple.getT2(), "Wikipedia + Spotify"));
    }

    /**
     * Deduplicates ranked songs while preserving the first occurrence and original order.
     *
     * Wikipedia tables can contain duplicate tracks (same song appearing in different releases).
     * This method keeps the first occurrence per (title + displayName) pair.
     *
     * @param songs a list of ranked songs
     * @return a de-duplicated list limited to the top 10
     */
    private List<MusicRankProvider.RankedSong> deduplicateAndLimitSongs(List<MusicRankProvider.RankedSong> songs) {
        java.util.Map<String, MusicRankProvider.RankedSong> uniqueSongs = new java.util.LinkedHashMap<>();
        for (var s : songs) {
            String key = (s.title() + "|" + s.displayName()).toLowerCase();
            uniqueSongs.putIfAbsent(key, s);
        }
        return uniqueSongs.values().stream()
                .limit(10)
                .toList();
    }

    /**
     * Sorts artists by "impact" and limits the list.
     *
     * Sort order:
     * - Primary: the number of occurrences/weeks a lead artist appears
     * - Secondary (tie-breaker): lowest rank (earlier/better appearance)
     *
     * @param artists ranked artists
     * @return sorted/limited list of artists (top 10)
     */
    private List<MusicRankProvider.RankedArtist> sortAndLimitArtists(List<MusicRankProvider.RankedArtist> artists) {
        return artists.stream()
                .sorted((a, b) -> {
                    int scoreDiff = Integer.compare(b.occurrenceCount(), a.occurrenceCount());
                    return scoreDiff != 0 ? scoreDiff : Integer.compare(a.rank(), b.rank());
                })
                .limit(10)
                .toList();
    }

    /**
     * Enriches a ranked song with Spotify search results.
     *
     * If Spotify doesn't return any results (or the request fails), a minimal fallback
     * representation is returned.
     *
     * @param rankedSong ranked song from the ranking provider
     * @param displayRank 1-based rank used for display (preserves list order)
     * @return enriched track DTO
     */
    private Mono<MusicResponse.TrackDTO> enrichSong(MusicRankProvider.RankedSong rankedSong, int displayRank) {
        return spotifyClient.searchTrack(rankedSong.title(), rankedSong.primaryArtist())
                .map(response -> {
                    if (response.tracks().items().isEmpty()) {
                        return createEmptySong(rankedSong, displayRank);
                    }
                    var item = response.tracks().items().get(0);
                    return new MusicResponse.TrackDTO(
                            displayRank,
                            rankedSong.displayName(),
                            item
                    );
                })
                .onErrorResume(e -> Mono.just(createEmptySong(rankedSong, displayRank)));
    }

    /**
     * Builds a fallback song payload when Spotify search fails or returns no hits.
     *
     * @param rankedSong ranked song
     * @param displayRank rank to display
     * @return track DTO with "Unknown" album data and minimal artist info
     */
    private MusicResponse.TrackDTO createEmptySong(MusicRankProvider.RankedSong rankedSong, int displayRank) {
        var emptyTrack = new se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse.TrackItem(
                null, rankedSong.title(), 0, null,
                new se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse.Album("Unknown", "Unknown", List.of()),
                List.of(new se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse.ArtistRef(null, rankedSong.primaryArtist()))
        );
        return new MusicResponse.TrackDTO(displayRank, rankedSong.displayName(), emptyTrack);
    }

    /**
     * Enriches a ranked artist with Spotify artist data and the artist's top tracks.
     *
     * If Spotify search fails (or returns no hits), a minimal fallback artist payload is returned.
     *
     * @param rankedArtist ranked artist from the ranking provider
     * @param displayRank 1-based rank used for display (preserves list order)
     * @return enriched artist DTO
     */
    private Mono<MusicResponse.ArtistDTO> enrichArtist(MusicRankProvider.RankedArtist rankedArtist, int displayRank) {
        return spotifyClient.searchArtist(rankedArtist.primaryArtist())
                .flatMap(response -> {
                    if (response.artists().items().isEmpty()) {
                        return Mono.just(createEmptyArtist(rankedArtist, displayRank));
                    }
                    var item = response.artists().items().getFirst();
                    return spotifyClient.getArtistTopTracks(item.id())
                            .map(topTracksResponse -> {
                                List<se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse.TrackItem> topSongs = topTracksResponse.tracks().stream()
                                        .limit(10)
                                        .toList();

                                return new MusicResponse.ArtistDTO(
                                        displayRank,
                                        item,
                                        topSongs
                                );
                            });
                })
                .onErrorResume(e -> Mono.just(createEmptyArtist(rankedArtist, displayRank)));
    }

    /**
     * Builds a fallback artist payload when Spotify search fails or returns no hits.
     *
     * @param rankedArtist ranked artist
     * @param displayRank rank to display
     * @return artist DTO with minimal fields set
     */
    private MusicResponse.ArtistDTO createEmptyArtist(MusicRankProvider.RankedArtist rankedArtist, int displayRank) {
        var emptyArtist = new se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse.ArtistItem(
                null, rankedArtist.primaryArtist(), List.of(), 0,
                new se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse.Followers(0),
                null, List.of()
        );
        return new MusicResponse.ArtistDTO(displayRank, emptyArtist, List.of());
    }

    /**
     * Returns the year's top artists enriched with Spotify artist metadata.
     *
     * This is a narrower response than {@link #getMusicByYear(int)} and is useful
     * for endpoints that only render artists.
     *
     * @param year year to fetch artists for
     * @return a {@link Mono} emitting a Spotify-like artist search response containing the top artists
     */
    public Mono<SpotifyArtistSearchResponse> getTopArtistsByYear(int year) {
        return rankProvider.getTopArtists(year)
                .map(this::sortAndLimitArtists)
                .flatMapMany(Flux::fromIterable)
                .index()
                .flatMapSequential(tuple -> enrichArtist(tuple.getT2(), tuple.getT1().intValue() + 1))
                .collectList()
                .map(artists -> {
                    List<SpotifyArtistSearchResponse.ArtistItem> artistItems = artists.stream()
                            .map(MusicResponse.ArtistDTO::artistData)
                            .toList();
                    return new SpotifyArtistSearchResponse(new SpotifyArtistSearchResponse.Artists(artistItems));
                });
    }

    /**
     * Returns the year's top tracks enriched with Spotify track metadata.
     *
     * This is a narrower response than {@link #getMusicByYear(int)} and is useful
     * for endpoints that only render tracks.
     *
     * @param year year to fetch tracks for
     * @return a {@link Mono} emitting a {@link MusicResponse} containing only track data
     */
    public Mono<MusicResponse> getTopTracksByYear(int year) {
        return rankProvider.getTopSongs(year)
                .map(this::deduplicateAndLimitSongs)
                .flatMapMany(Flux::fromIterable)
                .index()
                .flatMapSequential(tuple -> enrichSong(tuple.getT2(), tuple.getT1().intValue() + 1))
                .collectList()
                .map(tracks -> new MusicResponse(year, tracks, List.of(), "Wikipedia"));
    }
}
