package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.wikicap.client.music.SpotifyClient;

import java.util.List;

/**
 * Response model for Spotify artist top tracks.
 *
 * Maps the subset of fields returned by:
 * GET /v1/artists/{id}/top-tracks
 *
 * Used by {@link SpotifyClient#getArtistTopTracks(String)}.
 *
 * @param tracks list of top tracks (Spotify track objects)
 */
public record SpotifyTopTracksResponse(
        @JsonProperty("tracks") List<SpotifyTrackSearchResponse.TrackItem> tracks
) {
}
