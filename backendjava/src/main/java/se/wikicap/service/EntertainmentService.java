package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import se.wikicap.client.EntertainmentClient;
import se.wikicap.dto.entertainment.AcademyAwardResponse;
import se.wikicap.dto.entertainment.EntertainmentResponse;
import se.wikicap.dto.entertainment.TMBDMovieResponse;
import se.wikicap.dto.entertainment.TMBDSerieDTO;
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
     * Fetch entertainment data (movies, series, awards) for a specific year.
     * Makes 3 API calls in PARALLEL (non-blocking) for maximum performance.
     *
     * @param year The year to fetch entertainment data for
     * @return Mono<EntertainmentResponse> containing all entertainment data
     */
    public Mono<EntertainmentResponse> getEntertainmentByYear(int year) {
        Mono<TMBDMovieResponse> movies = entertainmentClient.fetchTopMoviesByYear(year);
        Mono<TMBDSeriesResponse> series = entertainmentClient.fetchTopSeriesByYear(year)
                .map(response -> rankAndFilterSeries(response, year));
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

    /**
     * Rank series by custom scoring algorithm and filter to top 8.
     * Combines popularity and rating with age penalty.
     *
     * @param seriesResponse The raw series response from TMDB
     * @param year The year being queried
     * @return Filtered and ranked series response
     */
    private TMBDSeriesResponse rankAndFilterSeries(TMBDSeriesResponse seriesResponse, int year) {
        if (seriesResponse.getResults() == null || seriesResponse.getResults().isEmpty()) {
            return seriesResponse;
        }

        var rankedSeries = seriesResponse.getResults().stream()
                .sorted((s1, s2) -> Double.compare(calculateSeriesScore(s2, year), calculateSeriesScore(s1, year)))
                .limit(8)
                .toList();

        seriesResponse.setResults(rankedSeries);
        return seriesResponse;
    }

    /**
     * Calculate ranking score for a TV series.
     * Formula: (popularity * 0.6 + normalized_rating * 100 * 0.4) * age_penalty
     *
     * @param serie The series DTO
     * @param year The year being queried
     * @return Composite score for ranking
     */
    private double calculateSeriesScore(TMBDSerieDTO serie, int year) {
        double popularity = serie.getPopularity() != null ? serie.getPopularity() : 0.0;
        double rating = serie.getVoteAverage() != null ? serie.getVoteAverage() / 10.0 : 0.0;

        double baseScore = (popularity * 0.6) + (rating * 100 * 0.4);
        return baseScore * calculateAgePenalty(serie.getFirstAirDate(), year);
    }

    /**
     * Calculate age-based penalty multiplier for TV series ranking.
     * Applies progressive penalties to older shows to prioritize recent content.
     *
     * @param firstAirDate First air date in YYYY-MM-DD format
     * @param year The year being queried
     * @return Penalty multiplier between 0.5 and 1.0
     */
    private double calculateAgePenalty(String firstAirDate, int year) {
        if (firstAirDate == null || firstAirDate.length() < 4) {
            return 1.0;
        }

        try {
            int startYear = Integer.parseInt(firstAirDate.substring(0, 4));
            int age = year - startYear;

            if (age <= 2) return 1.0;
            if (age <= 5) return 0.9;
            if (age <= 10) return 0.8;
            if (age <= 20) return 0.65;
            return 0.5;
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

}

