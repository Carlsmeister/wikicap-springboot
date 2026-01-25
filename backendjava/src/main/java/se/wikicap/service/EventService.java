package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import se.wikicap.dto.EventResponse;

@Service
public class EventService {

    /**
     * Fetch historical events for a specific year.
     *
     * @param year The year to fetch events for
     * @return Mono<EventResponse> containing events data
     */
    public Mono<EventResponse> getEventsByYear(int year) {
        //TODO: Implement fetching events by year
        return Mono.just(new EventResponse());  // Return default empty response
    }
}
