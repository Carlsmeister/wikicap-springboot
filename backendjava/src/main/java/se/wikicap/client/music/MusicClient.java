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

    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpiresAt;

    public MusicClient() {
        this.spotifyApiClient = WebClient.builder()
                .baseUrl(SPOTIFY_SEARCH_BASE_URL)
                .build();

        this.spotifyAuthClient = WebClient.builder()
                .baseUrl(SPOTIFY_TOKEN_URL)
                .build();
    }

    /**
     * Get a Spotify access token using the client credentials flow.
     * Token is cached in-memory until shortly before expiry.
     */
    public Mono<String> getSpotifyAccessToken() {
        if (cachedAccessToken != null && cachedAccessTokenExpiresAt != null) {
            if (Instant.now().isBefore(cachedAccessTokenExpiresAt.minusSeconds(30))) {
                return Mono.just(cachedAccessToken);
            }
        }

        String auth = spotifyClientId + ":" + spotifyClientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        return spotifyAuthClient.post()
                .uri("/api/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(SpotifyTokenResponse.class)
                .map(token -> {
                    this.cachedAccessToken = token.accessToken();
                    this.cachedAccessTokenExpiresAt = Instant.now().plusSeconds(token.expiresIn());
                    return token.accessToken();
                });
    }

    /**
     * Search Spotify for a specific track by name and artist.
     * @param title The track title
     * @param artist The artist name
     * @return Mono<SpotifyTrackSearchResponse>
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
     * Search Spotify for a specific artist by name.
     * @param name The artist name
     * @return Mono<SpotifyArtistSearchResponse>
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
     * Get the top tracks for a specific artist.
     * @param artistId The Spotify artist ID
     * @return Mono<SpotifyTopTracksResponse>
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

