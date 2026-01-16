package se.wikicap.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.wikicap.dto.entertainment.*;

import java.util.List;

/**
 * Client for interacting with The Movie Database (TMDB) API and the Awards API.
 * Provides methods to fetch movies, TV series, and Oscars winners, and enriches
 * Oscars winner data with TMDB images (posters and profile photos).
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
    private final WebClient tmdbClient;
    private final WebClient awardsClient;

    public EntertainmentClient() {
        this.tmdbClient = WebClient.builder()
                .baseUrl(TMDB_BASE_URL)
                .build();

        this.awardsClient = WebClient.builder()
                .baseUrl(ACADEMY_AWARDS_BASE_URL)
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
     * @return Mono<TMBDMovieResponse> containing list of movies and pagination info
     */
    public Mono<TMBDMovieResponse> fetchTopMoviesByYear(int year) {
        return tmdbClient.get()
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
                .map(response -> {
                    response.setYear(year);
                    return response;
                });
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
     * @return Mono<TMBDSeriesResponse> containing list of TV series and pagination info
     */
    public Mono<TMBDSeriesResponse> fetchTopSeriesByYear(int year) {
        return tmdbClient.get()
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
                .map(response -> {
                    response.setYear(year);
                    return response;
                });
    }

    /**
     * Search TMDB for a movie by title with optional year filtering.
     * Returns the first result (best match) or empty Mono if no results found.
     *
     * @param title The movie title to search for
     * @param year Optional release year to narrow results (can be null)
     * @return Mono<TMBDMovie> with first movie result or empty
     */
    public Mono<TMBDMovie> searchMovieByTitle(String title, Integer year) {
        return tmdbClient.get()
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
                .flatMap(response -> {
                    if (response.getResults() != null && !response.getResults().isEmpty()) {
                        return Mono.just(response.getResults().getFirst());
                    }
                    return Mono.empty();
                });
    }

    /**
     * Search TMDB for a person (actor, director, etc.) by name.
     * Returns the first result (best match) or empty Mono if no results found.
     *
     * @param name The person's name to search for
     * @return Mono<TMBDPerson> with first person result or empty
     */
    public Mono<TMBDPerson> searchPersonByName(String name) {
        return tmdbClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/person")
                        .queryParam("query", name)
                        .queryParam("include_adult", false)
                        .build())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + TMDB_API_KEY)
                .retrieve()
                .bodyToMono(TMBDPersonResponse.class)
                .flatMap(response -> {
                    if (response.getResults() != null && !response.getResults().isEmpty()) {
                        return Mono.just(response.getResults().getFirst());
                    }
                    return Mono.empty();
                });
    }

    /**
     * Fetch Academy Awards data for a specific year.
     * Fetches edition, all categories, and nominees for bestActor, bestActress, bestPicture.
     * Enriches data with TMDB images (posters and profile photos).
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
                .bodyToFlux(AcademyAward.Edition.class)
                .next()
                .flatMap(edition -> fetchCategoriesForEdition(edition.getId())
                        .flatMap(categories -> {
                            AcademyAward.Category bestPictureCat = categories.stream()
                                    .filter(cat -> cat.getName().equals("Best Picture"))
                                    .findFirst().orElse(null);

                            AcademyAward.Category bestActorCat = categories.stream()
                                    .filter(cat -> cat.getName().equals("Actor In A Leading Role"))
                                    .findFirst().orElse(null);

                            AcademyAward.Category bestActressCat = categories.stream()
                                    .filter(cat -> cat.getName().equals("Actress In A Leading Role"))
                                    .findFirst().orElse(null);

                            Mono<AcademyAwardResponse.PictureAward> bestPictureMono =
                                    bestPictureCat != null
                                            ? processBestPicture(edition.getId(), bestPictureCat.getId(), year)
                                            : Mono.just(new AcademyAwardResponse.PictureAward());

                            Mono<AcademyAwardResponse.ActorAward> bestActorMono =
                                    bestActorCat != null
                                            ? processPerson(edition.getId(), bestActorCat.getId())
                                            : Mono.just(new AcademyAwardResponse.ActorAward());

                            Mono<AcademyAwardResponse.ActorAward> bestActressMono =
                                    bestActressCat != null
                                            ? processPerson(edition.getId(), bestActressCat.getId())
                                            : Mono.just(new AcademyAwardResponse.ActorAward());

                            return Mono.zip(bestPictureMono, bestActorMono, bestActressMono)
                                    .map(tuple -> {
                                        AcademyAwardResponse response = new AcademyAwardResponse();
                                        response.setYear(year);
                                        response.getOscars().setBestPicture(tuple.getT1());
                                        response.getOscars().setBestActor(tuple.getT2());
                                        response.getOscars().setBestActress(tuple.getT3());
                                        return response;
                                    });
                        }))
                .onErrorResume(error -> {
                    System.err.println("Error fetching Academy Awards: " + error.getMessage());
                    AcademyAwardResponse response = new AcademyAwardResponse();
                    response.setYear(year);
                    return Mono.just(response);
                });
    }

    /**
     * Fallback build when TMDB does not return a movie match.
     *
     * The returned instance always contains nominee metadata from the Awards API.
     * The poster is intentionally left unset (null) so callers can distinguish between
     * "found but missing poster" vs. "not found" if they need to.
     *
     * @param winner the Awards API nominee marked as winner (must not be null)
     * @param movieTitle the winner movie title from the Awards API
     * @return a populated {@code PictureAward} without a poster
     */
    private static AcademyAwardResponse.PictureAward buildPictureAwardFallback(AcademyAward.Nominee winner,
                                                                               String movieTitle) {
        AcademyAwardResponse.PictureAward award = new AcademyAwardResponse.PictureAward();
        award.setTitle(movieTitle);
        award.setId(winner.getId());
        award.setMore(winner.getMore());
        award.setNote(winner.getNote());
        award.setWinner(winner.getWinner());
        return award;
    }

    /**
     * Fallback build when TMDB does not return a person match.
     *
     * The returned instance always contains nominee metadata from the Awards API.
     * The profile image path is intentionally left unset (null).
     *
     * @param winner the Awards API nominee marked as winner (must not be null)
     * @param personName the winner person name from the Awards API
     * @param movieTitle extracted movie title from the Awards API nominee "more" field
     * @return a populated {@code ActorAward} without an image
     */
    private static AcademyAwardResponse.ActorAward buildActorAwardFallback(AcademyAward.Nominee winner,
                                                                           String personName,
                                                                           String movieTitle) {
        AcademyAwardResponse.ActorAward award = new AcademyAwardResponse.ActorAward();
        award.setName(personName);
        award.setMovie(movieTitle);
        award.setId(winner.getId());
        award.setMore(winner.getMore());
        award.setNote(winner.getNote());
        award.setWinner(winner.getWinner());
        return award;
    }

    /**
     * Process best picture winner and enrich with TMDB poster image.
     * @param editionId The edition ID (e.g., 73 for 2000)
     * @param categoryId The category ID (e.g., 3636 for Best Picture)
     * @param year The award year to help narrow movie search
     * @return Mono<PictureAward>
     */
    private Mono<AcademyAwardResponse.PictureAward> processBestPicture(Integer editionId, Integer categoryId, int year) {
        return fetchNomineesForCategory(editionId, categoryId)
                .flatMap(nominees -> {
                    AcademyAward.Nominee winner = nominees.stream()
                            .filter(n -> n.getWinner() != null && n.getWinner())
                            .findFirst()
                            .orElse(null);

                    if (winner == null) {
                        return Mono.just(new AcademyAwardResponse.PictureAward());
                    }

                    String movieTitle = winner.getName();

                    return searchMovieByTitle(movieTitle, year)
                            .map(movie -> {
                                AcademyAwardResponse.PictureAward award = buildPictureAwardFallback(winner, movieTitle);
                                award.setPoster(movie.getPosterPath());
                                return award;
                            })
                            .defaultIfEmpty(buildPictureAwardFallback(winner, movieTitle));
                });
    }

    /**
     * Process best actor/actress winner and enrich with TMDB profile image.
     * @param editionId The edition ID (e.g., 73 for 2000)
     * @param categoryId The category ID (e.g., 3636 for Best Picture)
     * @return Mono<ActorAward>
     */
    private Mono<AcademyAwardResponse.ActorAward> processPerson(Integer editionId, Integer categoryId) {
        return fetchNomineesForCategory(editionId, categoryId)
                .flatMap(nominees -> {
                    AcademyAward.Nominee winner = nominees.stream()
                            .filter(n -> n.getWinner() != null && n.getWinner())
                            .findFirst()
                            .orElse(null);

                    if (winner == null) {
                        return Mono.just(new AcademyAwardResponse.ActorAward());
                    }

                    String personName = winner.getName();
                    String movieTitle = extractMovieTitle(winner.getMore());

                    return searchPersonByName(personName)
                            .map(person -> {
                                AcademyAwardResponse.ActorAward award = buildActorAwardFallback(winner, personName, movieTitle);
                                award.setImage(person.getProfilePath());
                                return award;
                            })
                            .defaultIfEmpty(buildActorAwardFallback(winner, personName, movieTitle));
                });
    }

    /**
     * Extract clean movie title from Awards API 'more' field.
     * Example: "Joker {Arthur Fleck}" -> "Joker"
     */
    private String extractMovieTitle(String moreField) {
        if (moreField == null || moreField.isEmpty()) {
            return "";
        }
        return moreField.replaceAll("\\s*\\{.*?}\\s*", "").trim();
    }

    /**
     * Helper method to fetch all categories for a specific edition.
     *
     * @param editionId The edition ID (e.g., 73 for 2000)
     * @return Mono<List<Category>> containing all award categories for this edition
     */
    private Mono<List<AcademyAward.Category>> fetchCategoriesForEdition(Integer editionId) {
        return awardsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/oscars/editions/" + editionId + "/categories")
                        .build())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToFlux(AcademyAward.Category.class)
                .collectList();
    }

    /**
     * Helper method to fetch nominees for a specific category.
     *
     * @param editionId The edition ID (e.g., 73 for 2000)
     * @param categoryId The category ID (e.g., 3636 for Best Picture)
     * @return Mono<List<Nominee>> containing all nominees for this category
     */
    private Mono<List<AcademyAward.Nominee>> fetchNomineesForCategory(Integer editionId, Integer categoryId) {
        return awardsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/oscars/editions/" + editionId + "/categories/" + categoryId + "/nominees")
                        .build())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToFlux(AcademyAward.Nominee.class)
                .collectList();
    }
}

