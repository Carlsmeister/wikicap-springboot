package se.wikicap.dto.entertainment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO representing a person (actor, director, etc.) from TMDB API.
 */
@Setter
@Getter
public class TMBDPersonDTO {

    // Getters and Setters
    private Long id;
    private String name;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    private String gender;
    private Double popularity;

    @JsonProperty("profile_path")
    private String profilePath;

    @JsonProperty("known_for")
    private List<KnownForItem> knownFor;

    public TMBDPersonDTO() {
    }

    /**
     * Inner class representing movies/TV shows the person is known for
     */
    @Setter
    @Getter
    public static class KnownForItem {
        private Long id;
        private String title;
        private String name;

        @JsonProperty("media_type")
        private String mediaType;

        @JsonProperty("release_date")
        private String releaseDate;

        @JsonProperty("first_air_date")
        private String firstAirDate;

        public KnownForItem() {
        }

    }
}

