package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import se.wikicap.dto.EventResponseDTO;

@Service
public class EventService {

    /**
     * Fetch historical events for a specific year.
     *
     * @param year The year to fetch events for
     * @return Mono<EventResponseDTO> containing events data
     */
    public Mono<EventResponseDTO> getEventsByYear(int year) {
        //TODO: Implement fetching events by year
        return Mono.empty();  // Return empty Mono instead of null
    }
}
