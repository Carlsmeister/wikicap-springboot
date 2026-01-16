package se.wikicap.client.music;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse;
import se.wikicap.dto.music.spotify.SpotifyTokenResponse;
import se.wikicap.dto.music.spotify.SpotifyTopTracksResponse;
import se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spotify client used to enrich ranked songs/artists with metadata.
 *
 * Features:
 * - Uses Spotify Client Credentials flow to get an access token
 * - Caches the token in memory until shortly before expiry
 * - Reuses an in-flight token request so concurrent calls don't trigger multiple token refreshes
 *
 * All methods return {@link Mono} and are intended to be used from service layers.
 */
@Component
public class MusicClient {

    private static final String SPOTIFY_SEARCH_BASE_URL = "https://api.spotify.com/v1";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com";

    @Value("${SPOTIFY_CLIENT_ID}")
    private String spotifyClientId;

    @Value("${SPOTIFY_CLIENT_SECRET}")
    private String spotifyClientSecret;

    private final WebClient spotifyApiClient;
    private final WebClient spotifyAuthClient;

    private final AtomicReference<TokenHolder> tokenHolder = new AtomicReference<>();
    private volatile Mono<String> activeTokenRequest;

    /**
     * Small in-memory representation of a Spotify access token.
     *
     * The token is treated as expired slightly before the official expiry time to
     * avoid edge cases where it expires during a request.
     */
    private record TokenHolder(String accessToken, Instant expiresAt) {
        boolean isExpired() {
            return !Instant.now().isAfter(expiresAt.minusSeconds(30));
        }
    }

    /**
     * Creates a new {@link MusicClient}.
     *
     * Uses separate {@link WebClient} instances for:
     * - Spotify Web API requests (search/top-tracks)
     * - Spotify Accounts requests (token)
     */
    public MusicClient() {
        this.spotifyApiClient = WebClient.builder()
                .baseUrl(SPOTIFY_SEARCH_BASE_URL)
                .build();

        this.spotifyAuthClient = WebClient.builder()
                .baseUrl(SPOTIFY_TOKEN_URL)
                .build();
    }

    /**
     * Returns a Spotify access token using the Client Credentials flow.
     *
     * Behavior:
     * - If a valid token is cached, returns it immediately.
     * - If the cached token is missing/expired, requests a new token.
     * - If a token request is already in-flight, reuses that request for all callers.
     *
     * @return a {@link Mono} emitting the access token string
     * @see SpotifyTokenResponse
     */
    public Mono<String> getSpotifyAccessToken() {
        TokenHolder current = tokenHolder.get();
        if (current != null && current.isExpired()) {
            return Mono.just(current.accessToken());
        }

        return Mono.defer(() -> {
            TokenHolder latest = tokenHolder.get();
            if (latest != null && latest.isExpired()) {
                return Mono.just(latest.accessToken());
            }

            synchronized (this) {
                if (activeTokenRequest != null) {
                    return activeTokenRequest;
                }

                String auth = spotifyClientId + ":" + spotifyClientSecret;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("grant_type", "client_credentials");

                activeTokenRequest = spotifyAuthClient.post()
                        .uri("/api/token")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(form)
                        .retrieve()
                        .bodyToMono(SpotifyTokenResponse.class)
                        .map(token -> {
                            TokenHolder newToken = new TokenHolder(
                                    token.accessToken(),
                                    Instant.now().plusSeconds(token.expiresIn())
                            );
                            tokenHolder.set(newToken);
                            return token.accessToken();
                        })
                        .doFinally(signalType -> {
                            synchronized (this) {
                                activeTokenRequest = null;
                            }
                        })
                        .cache();

                return activeTokenRequest;
            }
        });
    }

    /**
     * Searches Spotify for a track by title and artist.
     *
     * Query format:
     * track:"{title}" artist:"{artist}"
     *
     * Notes:
     * - Returns up to one result (limit=1)
     * - Uses {@link #getSpotifyAccessToken()} to authenticate
     *
     * @param title track title
     * @param artist artist name
     * @return a {@link Mono} emitting a {@link SpotifyTrackSearchResponse}
     */
    public Mono<SpotifyTrackSearchResponse> searchTrack(String title, String artist) {
        String query = String.format("track:\"%s\" artist:\"%s\"", title, artist);
        return getSpotifyAccessToken()
                .flatMap(token -> spotifyApiClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search")
                                .queryParam("q", query)
                                .queryParam("type", "track")
                                .queryParam("limit", 1)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(SpotifyTrackSearchResponse.class));
    }

    /**
     * Searches Spotify for an artist by name.
     *
     * Notes:
     * - Returns up to one result (limit=1)
     * - Uses {@link #getSpotifyAccessToken()} to authenticate
     *
     * @param name artist name
     * @return a {@link Mono} emitting a {@link SpotifyArtistSearchResponse}
     */
    public Mono<SpotifyArtistSearchResponse> searchArtist(String name) {
        return getSpotifyAccessToken()
                .flatMap(token -> spotifyApiClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search")
                                .queryParam("q", "artist:\"" + name + "\"")
                                .queryParam("type", "artist")
                                .queryParam("limit", 1)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(SpotifyArtistSearchResponse.class));
    }

    /**
     * Fetches the top tracks for a Spotify artist.
     *
     * Notes:
     * - Uses {@link #getSpotifyAccessToken()} to authenticate
     *
     * @param artistId Spotify artist ID
     * @return a {@link Mono} emitting a {@link SpotifyTopTracksResponse}
     */
    public Mono<SpotifyTopTracksResponse> getArtistTopTracks(String artistId) {
        return getSpotifyAccessToken()
                .flatMap(token -> spotifyApiClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/artists/{id}/top-tracks")
                                .build(artistId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(SpotifyTopTracksResponse.class));
    }
}

