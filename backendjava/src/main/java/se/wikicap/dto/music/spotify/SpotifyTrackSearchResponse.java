package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Partial response model for Spotify search(type=track).
 */
public record SpotifyTrackSearchResponse(
        @JsonProperty("tracks") Tracks tracks
) {
    public record Tracks(
            @JsonProperty("items") List<TrackItem> items
    ) {
    }

    public record TrackItem(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("popularity") Integer popularity,
            @JsonProperty("external_urls") ExternalUrls externalUrls,
            @JsonProperty("album") Album album,
            @JsonProperty("artists") List<ArtistRef> artists
    ) {
    }

    public record Album(
            @JsonProperty("name") String name,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("images") List<Image> images
    ) {
    }

    public record ArtistRef(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {
    }

    public record ExternalUrls(
            @JsonProperty("spotify") String spotify
    ) {
    }

    public record Image(
            @JsonProperty("url") String url,
            @JsonProperty("height") Integer height,
            @JsonProperty("width") Integer width
    ) {
    }
}

