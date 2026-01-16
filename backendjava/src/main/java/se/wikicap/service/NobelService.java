package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import se.wikicap.dto.NobelResponse;

@Service
public class NobelService {

    /**
     * Fetch Nobel Prize data for a specific year.
     *
     * @param year The year to fetch Nobel Prize data for
     * @return Mono<NobelResponse> containing Nobel Prize data
     */
    public Mono<NobelResponse> getNobelByYear(int year) {
        //TODO: Implement Nobel data fetching logic
        return Mono.empty();  // Return empty Mono instead of null
    }
}
