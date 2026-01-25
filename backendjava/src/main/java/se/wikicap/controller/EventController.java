package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.wikicap.dto.EventResponse;
import se.wikicap.service.EventService;

/**
 * REST controller for event endpoints.
 *
 * Base path: /api/v1/years
 *
 * Delegates to {@link EventService} for Wikipedia event aggregation.
 */
@RestController
@RequestMapping("/api/v1/years")
public class EventController {

    private final EventService eventService;

    /**
     * Creates a new {@link EventController}.
     *
     * @param eventService service that provides event aggregation logic
     */
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Returns historical events for a year.
     *
     * Path: GET /api/v1/years/{year}/events
     *
     * @param year year to fetch
     * @return a {@link Mono} emitting an {@link EventResponse}
     */
    @GetMapping("/{year}/events")
    public Mono<EventResponse> getEventsByYear(@PathVariable int year) {
        return eventService.getEventsByYear(year);
    }
}
