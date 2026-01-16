package se.wikicap.dto.entertainment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Wrapper DTO for TMDB movie API response containing list of top movies.
 */
@Setter
@Getter
public class TMBDMovieResponse {
    private String source = "TMDB";
    private int year;
    private List<TMBDMovie> results;
    private Integer page;

    @JsonProperty("total_results")
    private Integer totalResults;

    @JsonProperty("total_pages")
    private Integer totalPages;

    public TMBDMovieResponse() {
    }

}

