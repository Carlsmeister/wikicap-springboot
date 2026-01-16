package se.wikicap.dto.entertainment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response wrapper for TMDB person search API.
 * Contains the list of person results and pagination information.
 */
@Setter
@Getter
public class TMBDPersonResponse {
    private String source = "TMDB";
    private List<TMBDPerson> results;
    private Integer page;

    @JsonProperty("total_results")
    private Integer totalResults;

    @JsonProperty("total_pages")
    private Integer totalPages;

    public TMBDPersonResponse() {
    }

}

