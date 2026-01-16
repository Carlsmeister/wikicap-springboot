package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response model for Spotify artist top tracks.
 */
public record SpotifyTopTracksResponse(
        @JsonProperty("tracks") List<SpotifyTrackSearchResponse.TrackItem> tracks
) {}
