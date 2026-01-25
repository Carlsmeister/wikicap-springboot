package se.wikicap.dto.nobel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTOs for Nobel laureate API responses.
 *
 * Used for /laureates endpoint which returns { "laureates": [LaureateDTO] }
 */
public final class LaureateResponse {

    private LaureateResponse() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocalizedString(
            @JsonProperty("en") String en
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(
            LocalizedString countryNow
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            Location location
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(
            Place place
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Person(
            String filename,
            Event birth
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Organization(
            Place headquarters
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LaureateDTO(
            @JsonProperty("id") String id,
            Person laureateIfPerson,
            Organization laureateIfOrg,
            Wikipedia wikipedia
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Wikipedia(
            @JsonProperty("english") String english
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LaureatesResponse(
            LaureateDTO[] laureates
    ) {
    }
}
