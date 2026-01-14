package se.wikicap.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.wikicap.dto.entertainment.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for interacting with The Movie Database (TMDB) API.
 * Provides methods to fetch movies, TV series, and search functionality.
 * All methods are fully reactive (non-blocking) and return Mono types.
 *
 * API Documentation:
 *      - https://developers.themoviedb.org/3
 *      - https://theawards.vercel.app/
 */
@Component
public class EntertainmentClient {

    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    private static final String ACADEMY_AWARDS_BASE_URL = "https://theawards.vercel.app/api";

    @Value("${TMDB_API_KEY}")
    private String TMDB_API_KEY;
    private final WebClient tmbdClient;
    private final WebClient awardsClient;

    public EntertainmentClient() {
        this.tmbdClient = WebClient.builder()
                .baseUrl(TMDB_BASE_URL)
                .build();

        this.awardsClient = WebClient.builder()
                .baseUrl(ACADEMY_AWARDS_BASE_URL)
                .build();
    }

    /**
     * Fetch top-rated movies for a specific release year from TMDB.
     * Non-blocking - returns immediately with a Mono.
     *
     * Filters movies by:
     * - Primary release year matching the specified year
     * - Vote average ≥ 7.0
     * - Vote count ≥ 1000
     * - Sorted by vote count (descending)
     *
     * @param year The primary release year to filter by
     * @return Mono<TMBDMovieResponse> containing list of movies and pagination info
     */
    public Mono<TMBDMovieResponse> fetchTopMoviesByYear(int year) {
        return tmbdClient.get()
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
                .bodyToMono(TMBDMovieResponse.class);
    }

    /**
     * Fetch top-rated TV series that aired during a specific year from TMDB.
     * Non-blocking - returns immediately with a Mono.
     *
     * Filters series by:
     * - Air date between January 1 and December 31 of the specified year
     * - Vote average ≥ 7.0
     * - Vote count ≥ 1000
     * - Sorted by popularity (descending)
     *
     * @param year The year to filter series by (based on air date)
     * @return Mono<TMBDSeriesResponse> containing list of TV series and pagination info
     */
    public Mono<TMBDSeriesResponse> fetchTopSeriesByYear(int year) {
        return tmbdClient.get()
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
                .bodyToMono(TMBDSeriesResponse.class);
    }

    /**
     * Search TMDB for a movie by title with optional year filtering.
     * Returns the first result (best match) or null if no results found.
     * Non-blocking - returns immediately with a Mono.
     *
     * @param title The movie title to search for
     * @param year Optional release year to narrow results (can be null)
     * @return Mono<TMBDMovieDTO> with first movie result or empty
     */
    public Mono<TMBDMovieDTO> searchMovieByTitle(String title, Integer year) {
        return tmbdClient.get()
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
                .map(response -> response.getResults() != null && !response.getResults().isEmpty()
                        ? response.getResults().getFirst()
                        : null);
    }

    /**
     * Search TMDB for a person (actor, director, etc.) by name.
     * Returns the first result (best match) or null if no results found.
     * Non-blocking - returns immediately with a Mono.
     *
     * @param name The person's name to search for
     * @return Mono<TMBDPersonDTO> with first person result or empty
     */
    public Mono<TMBDPersonDTO> searchPersonByName(String name) {
        return tmbdClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/person")
                        .queryParam("query", name)
                        .queryParam("include_adult", false)
                        .build())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + TMDB_API_KEY)
                .retrieve()
                .bodyToMono(TMBDPersonResponse.class)
                .map(response -> response.getResults() != null && !response.getResults().isEmpty()
                        ? response.getResults().getFirst()
                        : null);
    }

    /**
     * Fetch Academy Awards data for a specific year.
     * Non-blocking - returns immediately with a Mono.
     * Fetches edition, all categories, and nominees for Best Picture, Best Actor, and Best Actress.
     * All nominee API calls are made in PARALLEL for maximum performance.
     *
     * @param year The year to fetch awards for
     * @return Mono<AcademyAwardResponse> containing awards data
     */
    public Mono<AcademyAwardResponse> fetchAwards(int year) {
        return awardsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/oscars/editions")
                        .queryParam("year", year)
                        .build())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToFlux(AcademyAwardDTO.Edition.class)
                .next()
                .flatMap(edition -> {
                    Mono<List<AcademyAwardDTO.Category>> categoriesMono = fetchCategoriesForEdition(edition.getId());

                    System.out.println("Fetched edition: " + edition.getEdition() + " for year: " + year + " and categories are: " + categoriesMono);

                    return categoriesMono.flatMap(categories -> {
                        if (categories.isEmpty()) {
                            AcademyAwardResponse response = new AcademyAwardResponse();
                            response.setEdition(edition);
                            return Mono.just(response);
                        }

                        List<String> desiredCategories = List.of(
                                "Best Picture",
                                "Actor In A Leading Role",
                                "Actress In A Leading Role"
                        );

                        List<AcademyAwardDTO.Category> relevantCategories = categories.stream()
                                .filter(cat -> desiredCategories.stream()
                                        .anyMatch(desired -> cat.getName().contains(desired)))
                                .toList();

                        if (relevantCategories.isEmpty()) {
                            AcademyAwardResponse response = new AcademyAwardResponse();
                            response.setEdition(edition);
                            response.setCategories(categories);
                            return Mono.just(response);
                        }

                        // Fetch nominees for all relevant categories IN PARALLEL
                        // Create a Mono for each category that fetches nominees and groups them together
                        List<Mono<AcademyAwardResponse.CategoryWithNominees>> categoryWithNomineesFetches =
                            relevantCategories.stream()
                                .map(category -> fetchNomineesForCategory(edition.getId(), category.getId())
                                    .map(nominees -> new AcademyAwardResponse.CategoryWithNominees(category, nominees)))
                                .toList();

                        // Wait for all category+nominee fetches to complete
                        return Mono.zip(categoryWithNomineesFetches, results -> {
                                    List<AcademyAwardResponse.CategoryWithNominees> awards = new ArrayList<>();
                                    for (Object result : results) {
                                        @SuppressWarnings("unchecked")
                                        AcademyAwardResponse.CategoryWithNominees categoryWithNominees =
                                            (AcademyAwardResponse.CategoryWithNominees) result;
                                        awards.add(categoryWithNominees);
                                    }
                                    return awards;
                                })
                                .map(awards -> {
                                    AcademyAwardResponse response = new AcademyAwardResponse();
                                    response.setEdition(edition);
                                    response.setCategories(categories);
                                    response.setAwards(awards);  // Now grouped by category!
                                    return response;
                                });
                    });
                })
                .onErrorResume(error -> {
                    System.err.println("Error fetching Academy Awards: " + error.getMessage());
                    return Mono.just(new AcademyAwardResponse());
                });
    }

    /**
     * Helper method to fetch all categories for a specific edition.
     *
     * @param editionId The edition ID (e.g., 73 for 2000)
     * @return Mono<List<Category>> containing all award categories for this edition
     */
    private Mono<List<AcademyAwardDTO.Category>> fetchCategoriesForEdition(Integer editionId) {
        return awardsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/oscars/editions/" + editionId + "/categories")
                        .build())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToFlux(AcademyAwardDTO.Category.class)
                .collectList();
    }

    /**
     * Helper method to fetch nominees for a specific category.
     *
     * @param editionId The edition ID (e.g., 73 for 2000)
     * @param categoryId The category ID (e.g., 3636 for Best Picture)
     * @return Mono<List<Nominee>> containing all nominees for this category
     */
    private Mono<List<AcademyAwardDTO.Nominee>> fetchNomineesForCategory(Integer editionId, Integer categoryId) {
        return awardsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/oscars/editions/" + editionId + "/categories/" + categoryId + "/nominees")
                        .build())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToFlux(AcademyAwardDTO.Nominee.class)
                .collectList();
    }
}
