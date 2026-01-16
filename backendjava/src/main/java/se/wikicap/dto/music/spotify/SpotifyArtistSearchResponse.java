package se.wikicap.dto.music.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Partial response model for Spotify search(type=artist).
 */
public record SpotifyArtistSearchResponse(
        @JsonProperty("artists") Artists artists
) {
    public record Artists(
            @JsonProperty("items") List<ArtistItem> items
    ) {
    }

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

    public record Followers(
            @JsonProperty("total") Integer total
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

