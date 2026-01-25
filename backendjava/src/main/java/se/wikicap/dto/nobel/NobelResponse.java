package se.wikicap.dto.nobel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NobelResponse(
        List<PrizeDTO> nobelPrizes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrizeDTO(
            LocalizedString category,
            LocalizedString categoryFullName,
            List<Laureate> laureates
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Laureate(
            @JsonProperty("id") String id,
            @JsonProperty("knownName") LocalizedString knownName,
            LocalizedString motivation,
            String wikiURL,
            String imageURL,
            String countryName,
            String countryFlag
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocalizedString(
            @JsonProperty("en") String en
    ) { }

}
