package se.wikicap.dto.entertainment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Wrapper DTO for TMDB TV series API response containing list of results.
 */
@Setter
@Getter
public class TMBDSeriesResponse {
    private String source = "TMDB";
    private int year;
    private List<TMBDSerie> results;
    private Integer page;

    @JsonProperty("total_results")
    private Integer totalResults;

    @JsonProperty("total_pages")
    private Integer totalPages;

    public TMBDSeriesResponse() {
    }

}

