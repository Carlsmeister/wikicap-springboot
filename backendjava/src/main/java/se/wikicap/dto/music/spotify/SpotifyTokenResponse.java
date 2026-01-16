package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Spotify Accounts service for the client-credentials token request.
 *
 * Maps the subset of fields returned by:
 * POST /api/token
 *
 * Used by {@link se.wikicap.client.music.MusicClient#getSpotifyAccessToken()}.
 *
 * @param accessToken bearer token used in Authorization header
 * @param tokenType token type returned by Spotify (typically "Bearer")
 * @param expiresIn lifetime in seconds
 */
public record SpotifyTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
}
