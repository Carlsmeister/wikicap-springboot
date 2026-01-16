package se.wikicap.dto.music;

import se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse;
import se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse;

import java.util.List;

/**
 * Aggregated music response for a specific year.
 *
 * This DTO is used by music endpoints to return:
 * - ranked tracks enriched with Spotify metadata
 * - ranked artists enriched with Spotify metadata and top tracks
 *
 * @param year the year the data belongs to
 * @param topTracks list of top tracks (ranked) for the year
 * @param topArtists list of top artists (ranked) for the year
 * @param source description of contributing sources (e.g. "Wikipedia + Spotify")
 */
public record MusicResponse(
        int year,
        List<TrackDTO> topTracks,
        List<ArtistDTO> topArtists,
        String source
) {

    /**
     * Ranked track entry.
     *
     * @param rank 1-based display rank
     * @param participants display string for chart participants (artist/featured artists)
     * @param trackData Spotify track data returned by {@link SpotifyTrackSearchResponse}
     */
    public record TrackDTO(
            Integer rank,
            String participants,
            SpotifyTrackSearchResponse.TrackItem trackData
    ) {
    }

    /**
     * Ranked artist entry.
     *
     * @param rank 1-based display rank
     * @param artistData Spotify artist data returned by {@link SpotifyArtistSearchResponse}
     * @param topTracks a list of top tracks for this artist (Spotify)
     */
    public record ArtistDTO(
            Integer rank,
            SpotifyArtistSearchResponse.ArtistItem artistData,
            List<SpotifyTrackSearchResponse.TrackItem> topTracks
    ) {
    }
}
