package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.wikicap.dto.*;
import se.wikicap.dto.entertainment.EntertainmentResponse;
import se.wikicap.dto.music.MusicResponse;
import se.wikicap.service.*;

/**
 * REST controller for year-based data aggregation.
 * All endpoints are fully reactive (non-blocking) for maximum performance.
 */
@RestController
@RequestMapping("/api/v1/years")
public class YearController {

    private final MusicService musicService;
    private final EntertainmentService entertainmentService;
    private final EventService eventService;
    private final NobelService nobelService;

    public YearController(MusicService musicService, EntertainmentService entertainmentService,
                         EventService eventService, NobelService nobelService) {
        this.musicService = musicService;
        this.entertainmentService = entertainmentService;
        this.eventService = eventService;
        this.nobelService = nobelService;
    }

    /**
     * Get all aggregated data for a specific year.
     * Makes 4 API calls in PARALLEL (non-blocking).
     *
     * @param year The year to fetch data for
     * @return Mono<YearResponse> with all year data (music, entertainment, events, nobel)
     */
    @GetMapping("/{year}")
    public Mono<YearResponse> getYear(@PathVariable int year) {
        Mono<MusicResponse> musicMono = musicService.getMusicByYear(year);
        Mono<EntertainmentResponse> entertainmentMono = entertainmentService.getEntertainmentByYear(year);
        Mono<EventResponse> eventsMono = eventService.getEventsByYear(year);
        Mono<NobelResponse> nobelMono = nobelService.getNobelByYear(year);

        return Mono.zip(musicMono, entertainmentMono, eventsMono, nobelMono)
                .map(tuple -> new YearResponse(
                        year,
                        tuple.getT1(),  // music
                        tuple.getT2(),  // entertainment
                        tuple.getT3(),  // events
                        tuple.getT4()   // nobel
                ));
    }
}
