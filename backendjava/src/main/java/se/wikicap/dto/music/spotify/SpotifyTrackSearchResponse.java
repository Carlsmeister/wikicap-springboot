package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.wikicap.client.music.SpotifyClient;

import java.util.List;

/**
 * Partial response model for Spotify track search.
 *
 * This record maps the subset of fields returned by:
 * GET /v1/search?type=track
 *
 * Used by {@link SpotifyClient#searchTrack(String, String)}.
 */
public record SpotifyTrackSearchResponse(
        @JsonProperty("tracks") Tracks tracks
) {

    /**
     * Wrapper object for the tracks list.
     *
     * @param items search results
     */
    public record Tracks(
            @JsonProperty("items") List<TrackItem> items
    ) {
    }

    /**
     * A Spotify track result.
     *
     * @param id Spotify track ID
     * @param name track name
     * @param popularity Spotify popularity score (0-100)
     * @param externalUrls external links
     * @param album album info (name, release date, images)
     * @param artists contributing artists (Spotify IDs and names)
     */
    public record TrackItem(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("popularity") Integer popularity,
            @JsonProperty("external_urls") ExternalUrls externalUrls,
            @JsonProperty("album") Album album,
            @JsonProperty("artists") List<ArtistRef> artists
    ) {
    }

    /**
     * Spotify album summary.
     *
     * @param name album name
     * @param releaseDate album release date string (format depends on precision)
     * @param images album art images
     */
    public record Album(
            @JsonProperty("name") String name,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("images") List<Image> images
    ) {
    }

    /**
     * Minimal artist reference embedded in a track result.
     *
     * @param id Spotify artist ID
     * @param name artist name
     */
    public record ArtistRef(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {
    }

    /**
     * External URLs returned by Spotify.
     *
     * @param spotify link to Spotify track page
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
