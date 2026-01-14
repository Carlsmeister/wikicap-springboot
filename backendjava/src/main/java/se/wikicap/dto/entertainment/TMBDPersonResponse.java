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

    private Integer page;

    private List<TMBDPersonDTO> results;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("total_results")
    private Integer totalResults;

    public TMBDPersonResponse() {
    }

}

