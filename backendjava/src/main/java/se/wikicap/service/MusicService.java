package se.wikicap.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.wikicap.client.music.MusicClient;
import se.wikicap.dto.music.MusicResponse;
import se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse;

import java.util.List;

@Service
public class MusicService {

    private final MusicClient musicClient;
    private final MusicRankProvider rankProvider;

    public MusicService(MusicClient musicClient, MusicRankProvider rankProvider) {
        this.musicClient = musicClient;
        this.rankProvider = rankProvider;
    }

    /**
     * Fetch music data for a specific year.
     * Uses Wikipedia for rankings and Spotify for metadata enrichment.
     *
     * @param year The year to fetch music data for
     * @return Mono<MusicResponse> containing enriched music data
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

    private List<MusicRankProvider.RankedSong> deduplicateAndLimitSongs(List<MusicRankProvider.RankedSong> songs) {
        // Use a map to keep only the first time a song appears
        java.util.Map<String, MusicRankProvider.RankedSong> uniqueSongs = new java.util.LinkedHashMap<>();
        for (var s : songs) {
            String key = (s.title() + "|" + s.displayName()).toLowerCase();
            uniqueSongs.putIfAbsent(key, s);
        }
        return uniqueSongs.values().stream()
                .limit(10)
                .toList();
    }

    private List<MusicRankProvider.RankedArtist> sortAndLimitArtists(List<MusicRankProvider.RankedArtist> artists) {
        return artists.stream()
                .sorted((a, b) -> {
                    // Primary sort: Most weeks at #1. Secondary: Earliest rank (tie-breaker)
                    int scoreDiff = Integer.compare(b.occurrenceCount(), a.occurrenceCount());
                    return scoreDiff != 0 ? scoreDiff : Integer.compare(a.rank(), b.rank());
                })
                .limit(10)
                .toList();
    }

    private Mono<MusicResponse.TrackDTO> enrichSong(MusicRankProvider.RankedSong rankedSong, int displayRank) {
        return musicClient.searchTrack(rankedSong.title(), rankedSong.primaryArtist())
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

    private MusicResponse.TrackDTO createEmptySong(MusicRankProvider.RankedSong rankedSong, int displayRank) {
        // Create a minimal Spotify TrackItem for fallback
        var emptyTrack = new se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse.TrackItem(
                null, rankedSong.title(), 0, null,
                new se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse.Album("Unknown", "Unknown", List.of()),
                List.of(new se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse.ArtistRef(null, rankedSong.primaryArtist()))
        );
        return new MusicResponse.TrackDTO(displayRank, rankedSong.displayName(), emptyTrack);
    }

    private Mono<MusicResponse.ArtistDTO> enrichArtist(MusicRankProvider.RankedArtist rankedArtist, int displayRank) {
        return musicClient.searchArtist(rankedArtist.primaryArtist())
                .flatMap(response -> {
                    if (response.artists().items().isEmpty()) {
                        return Mono.just(createEmptyArtist(rankedArtist, displayRank));
                    }
                    var item = response.artists().items().getFirst();
                    return musicClient.getArtistTopTracks(item.id())
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

    private MusicResponse.ArtistDTO createEmptyArtist(MusicRankProvider.RankedArtist rankedArtist, int displayRank) {
        var emptyArtist = new se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse.ArtistItem(
                null, rankedArtist.primaryArtist(), List.of(), 0,
                new se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse.Followers(0),
                null, List.of()
        );
        return new MusicResponse.ArtistDTO(displayRank, emptyArtist, List.of());
    }

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
