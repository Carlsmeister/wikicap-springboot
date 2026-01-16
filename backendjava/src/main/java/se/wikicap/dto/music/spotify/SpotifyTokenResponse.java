package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Spotify Accounts service for the client-credentials token request.
 */
public record SpotifyTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
}

