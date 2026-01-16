package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.wikicap.dto.entertainment.AcademyAwardResponse;
import se.wikicap.dto.entertainment.EntertainmentResponse;
import se.wikicap.dto.entertainment.TMBDMovieResponse;
import se.wikicap.dto.entertainment.TMBDSeriesResponse;
import se.wikicap.service.EntertainmentService;

/**
 * REST controller for entertainment-based data aggregation.
 * All endpoints are fully reactive (non-blocking) for maximum performance.
 */
@RestController
@RequestMapping("/api/v1/years")
public class EntertainmentController {

    private final EntertainmentService entertainmentService;

    public EntertainmentController(EntertainmentService entertainmentService) {

        this.entertainmentService = entertainmentService;
    }

    /**
     * Get entertainment data (movies, series, awards) for a specific year.
     *
     * @param year The year to fetch entertainment data for
     * @return Mono<EntertainmentResponse> with movies, series, and awards
     */
    @GetMapping("/{year}/entertainment")
    public Mono<EntertainmentResponse> getEntertainmentByYear(@PathVariable int year) {
        return entertainmentService.getEntertainmentByYear(year);
    }

    /**
     * Get movie data for a specific year.
     *
     * @param year The year to fetch movie data for
     * @return Mono<TMBDMovieResponse> containing all movie data
     */
    @GetMapping("/{year}/entertainment/movies")
    public Mono<TMBDMovieResponse> getMoviesByYear(@PathVariable int year) {
        return entertainmentService.getMoviesByYear(year);
    }

    /**
     * Get series data for a specific year.
     *
     * @param year The year to fetch series data for
     * @return Mono<TMBDSeriesResponse> containing all series data
     */
    @GetMapping("/{year}/entertainment/series")
    public Mono<TMBDSeriesResponse> getSeriesByYear(@PathVariable int year) {
        return entertainmentService.getSeriesByYear(year);
    }

    /**
     * Get awards data for a specific year.
     *
     * @param year The year to fetch awards data for
     * @return Mono<AcademyAwardResponse> containing all award data
     */
    @GetMapping("/{year}/entertainment/awards")
    public Mono<AcademyAwardResponse> getAwardsByYear(@PathVariable int year) {
        return entertainmentService.getAwardsByYear(year);
    }

}
