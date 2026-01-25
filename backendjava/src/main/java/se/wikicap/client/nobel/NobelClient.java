package se.wikicap.client.nobel;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.wikicap.dto.nobel.LaureateResponse;
import se.wikicap.dto.nobel.NobelResponse;

@Component
public class NobelClient {

    private static final String USER_AGENT = "WikiCapSpringBoot/1.0 (WikiCap course project; contact: carl.lundholm@example.com)";

    private final WebClient nobelClient;

    public NobelClient() {

        this.nobelClient = WebClient.builder()
                .baseUrl("https://api.nobelprize.org/2.1")
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    /**
     * Fetch Nobel Prize data for a specific year as a typed DTO.
     * @param year The Nobel Prize year (e.g., 2020)
     * @return A Mono emitting a NobelResponse object
     */
    public Mono<NobelResponse> fetchNobelPrizesByYear(int year) {
        return nobelClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/nobelPrizes")
                        .queryParam("nobelPrizeYear", year)
                        .build())
                .retrieve()
                .bodyToMono(NobelResponse.class);
    }

    /**
     * Fetch laureate metadata as a typed DTO.
     * @param laureateId The laureate ID (e.g., "1" for Alfred Nobel)
     * @return A Mono emitting an array of LaureateDTO objects
     */
    public Mono<LaureateResponse.LaureateDTO[]> fetchLaureateMetadata(String laureateId) {
        return nobelClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/laureates")
                        .queryParam("ID", laureateId)
                        .build())
                .retrieve()
                .bodyToMono(LaureateResponse.LaureatesResponse.class)
                .map(r -> r.laureates());
    }

    /**
     * Convenience method: extract the laureate's Wikipedia URL (English) from the metadata.
     * @param laureateId The laureate ID
     * @return A Mono emitting the English Wikipedia URL or an empty string
     */
    public Mono<String> fetchLaureateWikipediaUrl(String laureateId) {
        return fetchLaureateMetadata(laureateId)
                .map(arr -> {
                    if (arr.length == 0 || arr[0] == null || arr[0].wikipedia() == null) return "";
                    var url = arr[0].wikipedia().english();
                    return url == null ? "" : url;
                })
                .defaultIfEmpty("");
    }
}