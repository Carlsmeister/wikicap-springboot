package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import se.wikicap.client.EntertainmentClient;
import se.wikicap.dto.entertainment.AcademyAwardResponse;
import se.wikicap.dto.entertainment.EntertainmentResponse;
import se.wikicap.dto.entertainment.TMBDMovieResponse;
import se.wikicap.dto.entertainment.TMBDSeriesResponse;

/**
 * Service for aggregating se.wikicap.dto.entertainment data from multiple sources.
 * Uses reactive programming for non-blocking, parallel API calls.
 */
@Service
public class EntertainmentService {

    private final EntertainmentClient entertainmentClient;

    public EntertainmentService(EntertainmentClient entertainmentClient) {
        this.entertainmentClient = entertainmentClient;
    }

    /**
     * Fetch se.wikicap.dto.entertainment data (movies, series, awards) for a specific year.
     * Makes 3 API calls in PARALLEL (non-blocking) for maximum performance.
     *
     * @param year The year to fetch se.wikicap.dto.entertainment data for
     * @return Mono<EntertainmentResponse> containing all se.wikicap.dto.entertainment data
     */
    public Mono<EntertainmentResponse> getEntertainmentByYear(int year) {
        Mono<TMBDMovieResponse> movies = entertainmentClient.fetchTopMoviesByYear(year);
        Mono<TMBDSeriesResponse> series = entertainmentClient.fetchTopSeriesByYear(year);
        Mono<AcademyAwardResponse> awards = entertainmentClient.fetchAwards(year);

        return Mono.zip(movies, series, awards)
                .map(tuple -> {
                    EntertainmentResponse response = new EntertainmentResponse();
                    response.setMovies(tuple.getT1());
                    response.setSeries(tuple.getT2());
                    response.setAcademyAwards(tuple.getT3());
                    return response;
                });
    }
}
