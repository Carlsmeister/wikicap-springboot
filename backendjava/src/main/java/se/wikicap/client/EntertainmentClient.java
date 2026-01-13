package se.wikicap.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import se.wikicap.dto.*;

/**
 * Client for interacting with The Movie Database (TMDB) API.
 * Provides methods to fetch movies, TV series, and search functionality.
 *
 * API Documentation: https://developers.themoviedb.org/3
 */
@Component
public class EntertainmentClient {

    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";

    @Value("${TMDB_API_KEY}")
    private String TMDB_API_KEY;
    private final WebClient webClient;

    public EntertainmentClient() {
        this.webClient = WebClient.builder()
                .baseUrl(TMDB_BASE_URL)
                .build();
    }

    /**
     * Fetch top-rated movies for a specific release year from TMDB.
     *
     * Filters movies by:
     * - Primary release year matching the specified year
     * - Vote average ≥ 7.0
     * - Vote count ≥ 1000
     * - Sorted by vote count (descending)
     *
     * @param year The primary release year to filter by
     * @return TMBDMovieResponse containing list of movies and pagination info
     */
    public TMBDMovieResponse fetchTopMoviesByYear(int year) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("primary_release_year", year)
                        .queryParam("sort_by", "vote_count.desc")
                        .queryParam("vote_average.gte", 7)
                        .queryParam("vote_count.gte", 1000)
                        .build())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + TMDB_API_KEY)
                .retrieve()
                .bodyToMono(TMBDMovieResponse.class)
                .block();
    }

    /**
     * Fetch top-rated TV series that aired during a specific year from TMDB.
     *
     * Filters series by:
     * - Air date between January 1 and December 31 of the specified year
     * - Vote average ≥ 7.0
     * - Vote count ≥ 1000
     * - Sorted by popularity (descending)
     *
     * @param year The year to filter series by (based on air date)
     * @return TMBDSeriesResponse containing list of TV series and pagination info
     */
    public TMBDSeriesResponse fetchTopSeriesByYear(int year) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/tv")
                        .queryParam("air_date.gte", year + "-01-01")
                        .queryParam("air_date.lte", year + "-12-31")
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("vote_count.gte", 1000)
                        .queryParam("vote_average.gte", 7)
                        .queryParam("include_null_first_air_dates", false)
                        .build())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + TMDB_API_KEY)
                .retrieve()
                .bodyToMono(TMBDSeriesResponse.class)
                .block();
    }

    /**
     * Search TMDB for a movie by title with optional year filtering.
     * Returns the first result (best match) or null if no results found.
     *
     * @param title The movie title to search for
     * @param year Optional release year to narrow results (can be null)
     * @return First movie result or null if no results found
     */
    public TMBDMovieDTO searchMovieByTitle(String title, Integer year) {
        TMBDMovieResponse response = webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/search/movie")
                            .queryParam("query", title)
                            .queryParam("include_adult", false);
                    if (year != null) {
                        builder.queryParam("year", year);
                    }
                    return builder.build();
                })
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + TMDB_API_KEY)
                .retrieve()
                .bodyToMono(TMBDMovieResponse.class)
                .block();

        return (response != null && response.getResults() != null && !response.getResults().isEmpty())
                ? response.getResults().getFirst()
                : null;
    }

    public AcademyAwardDTO fetchAwards(int year) {
        // TODO: Implement awards fetching logic
        // Note: Academy Awards are not part of TMDB API
        return new AcademyAwardDTO();
    }
}
