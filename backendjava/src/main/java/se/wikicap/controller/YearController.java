package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.wikicap.dto.*;
import se.wikicap.dto.entertainment.EntertainmentResponse;
import se.wikicap.service.*;

/**
 * REST controller for year-based data aggregation.
 * All endpoints are fully reactive (non-blocking) for maximum performance.
 */
@RestController
@RequestMapping("/api/v1/year")
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
     * @return Mono<YearResponseDTO> with all year data (music, entertainment, events, nobel)
     */
    @GetMapping("/{year}")
    public Mono<YearResponseDTO> getYear(@PathVariable int year) {
        Mono<MusicResponseDTO> musicMono = musicService.getMusicByYear(year);
        Mono<EntertainmentResponse> entertainmentMono = entertainmentService.getEntertainmentByYear(year);
        Mono<EventResponseDTO> eventsMono = eventService.getEventsByYear(year);
        Mono<NobelResponseDTO> nobelMono = nobelService.getNobelByYear(year);

        return Mono.zip(musicMono, entertainmentMono, eventsMono, nobelMono)
                .map(tuple -> new YearResponseDTO(
                        year,
                        tuple.getT1(),  // music
                        tuple.getT2(),  // entertainment
                        tuple.getT3(),  // events
                        tuple.getT4()   // nobel
                ));
    }

    /**
     * Get se.wikicap.dto.entertainment data (movies, series, awards) for a specific year.
     *
     * @param year The year to fetch se.wikicap.dto.entertainment data for
     * @return Mono<EntertainmentResponse> with movies, series, and awards
     */
    @GetMapping("/{year}/entertainment")
    public Mono<EntertainmentResponse> getEntertainmentByYear(@PathVariable int year) {
        return entertainmentService.getEntertainmentByYear(year);
    }
}
