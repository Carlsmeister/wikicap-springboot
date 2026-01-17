package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.wikicap.client.music.SpotifyClient;

import java.util.List;

/**
 * Partial response model for Spotify artist search.
 *
 * This record maps the subset of fields returned by:
 * GET /v1/search?type=artist
 *
 * Used by {@link SpotifyClient#searchArtist(String)}.
 */
public record SpotifyArtistSearchResponse(
        @JsonProperty("artists") Artists artists
) {

    /**
     * Wrapper object for the artists list.
     *
     * @param items search results
     */
    public record Artists(
            @JsonProperty("items") List<ArtistItem> items
    ) {
    }

    /**
     * A Spotify artist result.
     *
     * @param id Spotify artist ID
     * @param name artist name
     * @param genres list of genres
     * @param popularity Spotify popularity score (0-100)
     * @param followers follower count container
     * @param externalUrls external links
     * @param images artist images
     */
    public record ArtistItem(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("genres") List<String> genres,
            @JsonProperty("popularity") Integer popularity,
            @JsonProperty("followers") Followers followers,
            @JsonProperty("external_urls") ExternalUrls externalUrls,
            @JsonProperty("images") List<Image> images
    ) {
    }

    /**
     * Spotify followers container.
     *
     * @param total total follower count
     */
    public record Followers(
            @JsonProperty("total") Integer total
    ) {
    }

    /**
     * External URLs returned by Spotify.
     *
     * @param spotify link to Spotify artist page
     */
    public record ExternalUrls(
            @JsonProperty("spotify") String spotify
    ) {
    }

    /**
     * Image resource returned by Spotify.
     *
     * @param url image URL
     * @param height image height
     * @param width image width
     */
    public record Image(
            @JsonProperty("url") String url,
            @JsonProperty("height") Integer height,
            @JsonProperty("width") Integer width
    ) {
    }
}
