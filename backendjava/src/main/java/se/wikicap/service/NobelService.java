package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.wikicap.client.nobel.NobelClient;
import se.wikicap.client.nobel.WikipediaNobelMetaClient;
import se.wikicap.dto.nobel.NobelResponse;

@Service
public class NobelService {

    private final NobelClient nobelClient;
    private final WikipediaNobelMetaClient wikipediaClient;

    public NobelService(NobelClient nobelClient, WikipediaNobelMetaClient wikipediaClient) {
        this.nobelClient = nobelClient;
        this.wikipediaClient = wikipediaClient;
    }

    /**
     * Fetch Nobel Prize data for a specific year and enrich each laureate with a Wikipedia URL.
     * @param year The Nobel Prize year (e.g., 2020)
     * @return A Mono emitting a NobelResponse object with enriched laureate data
     */
    public Mono<NobelResponse> getNobelPrizesByYear(int year) {
        return nobelClient.fetchNobelPrizesByYear(year)
                .flatMap(this::enrichResponseWithWikipediaUrls);
    }

    /**
     * Enrich each laureate in the NobelResponse with their Wikipedia URL.
     * @param response The original NobelResponse
     * @return A Mono emitting the enriched NobelResponse
     */
    private Mono<NobelResponse> enrichResponseWithWikipediaUrls(NobelResponse response) {
        if (response == null || response.nobelPrizes() == null) {
            return Mono.justOrEmpty(response);
        }

        return Flux.fromIterable(response.nobelPrizes())
                .flatMap(this::enrichPrize)
                .collectList()
                .map(NobelResponse::new);
    }

    /**
     * Enrich each laureate in a PrizeDTO with their Wikipedia URL.
     * @param prize The original PrizeDTO
     * @return A Mono emitting the enriched PrizeDTO
     */
    private Mono<NobelResponse.PrizeDTO> enrichPrize(NobelResponse.PrizeDTO prize) {
        if (prize == null || prize.laureates() == null) {
            return Mono.justOrEmpty(prize);
        }

        return Flux.fromIterable(prize.laureates())
                .flatMap(this::enrichLaureateWithWikipediaData)
                .collectList()
                .map(enrichedLaureates -> new NobelResponse.PrizeDTO(
                        prize.category(),
                        prize.categoryFullName(),
                        enrichedLaureates
                ));
    }

    /**
     * Enrich a single laureate with Wikipedia data: URL, image, country name, and flag.
     * @param laureate The original Laureate
     * @return A Mono emitting the enriched Laureate
     */
    private Mono<NobelResponse.Laureate> enrichLaureateWithWikipediaData(NobelResponse.Laureate laureate) {
        if (laureate == null || laureate.id() == null || laureate.id().isBlank()) {
            return Mono.justOrEmpty(laureate);
        }

        // If already fully enriched, return as is
        if (laureate.wikiURL() != null && !laureate.wikiURL().isBlank() &&
            laureate.imageURL() != null && !laureate.imageURL().isBlank() &&
            laureate.countryName() != null && !laureate.countryName().isBlank() &&
            laureate.countryFlag() != null && !laureate.countryFlag().isBlank()) {
            return Mono.just(laureate);
        }

        return getLaureateWikipediaUrl(laureate.id())
                .flatMap(wikiUrl -> {
                    if (wikiUrl.isBlank()) {
                        return Mono.just(laureate);
                    }

                    // Fetch additional metadata from Wikipedia
                    return wikipediaClient.fetchWikipediaMetadata(wikiUrl)
                            .map(metadata -> new NobelResponse.Laureate(
                                    laureate.id(),
                                    laureate.knownName(),
                                    laureate.motivation(),
                                    wikiUrl,
                                    metadata.imageURL() != null && !metadata.imageURL().isBlank() ? metadata.imageURL() : laureate.imageURL(),
                                    metadata.countryName() != null && !metadata.countryName().isBlank() ? metadata.countryName() : laureate.countryName(),
                                    metadata.countryFlag() != null && !metadata.countryFlag().isBlank() ? metadata.countryFlag() : laureate.countryFlag()
                            ))
                            .defaultIfEmpty(new NobelResponse.Laureate(
                                    laureate.id(),
                                    laureate.knownName(),
                                    laureate.motivation(),
                                    wikiUrl,
                                    laureate.imageURL(),
                                    laureate.countryName(),
                                    laureate.countryFlag()
                            ));
                })
                .doOnError(e -> System.out.println("Error enriching laureate " + laureate.id() + " with Wikipedia data: " + e.getMessage()))
                .onErrorReturn(laureate);
    }

    /**
     * Convenience passthrough: get the English Wikipedia URL for a laureate id.
     * @param laureateId The laureate ID
     * @return A Mono emitting the English Wikipedia URL or an empty string
     */
    public Mono<String> getLaureateWikipediaUrl(String laureateId) {
        return nobelClient.fetchLaureateWikipediaUrl(laureateId);
    }
}
