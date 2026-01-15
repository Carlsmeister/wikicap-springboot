package se.wikicap.dto.entertainment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntertainmentResponse {

    @Schema (description = "Movies released in the specified year")
    TMBDMovieResponse movies;

    @Schema (description = "TV series released in the specified year")
    TMBDSeriesResponse series;

    @Schema (description = "Academy Awards held in the specified year")
    AcademyAwardResponse academyAwards;

    public EntertainmentResponse() {
    }

    public EntertainmentResponse(TMBDMovieResponse movies, TMBDSeriesResponse series, AcademyAwardResponse academyAwards) {
        this.movies = movies;
        this.series = series;
        this.academyAwards = academyAwards;
    }

}
