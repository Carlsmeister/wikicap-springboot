package se.wikicap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntertainmentResponseDTO {

    @Schema (description = "Movies released in the specified year")
    TMBDMovieResponse movies;

    @Schema (description = "TV series released in the specified year")
    TMBDSeriesResponse series;

    @Schema (description = "Academy Awards held in the specified year")
    AcademyAwardDTO academyAwards;

    public EntertainmentResponseDTO() {
    }

    public EntertainmentResponseDTO(TMBDMovieResponse movies, TMBDSeriesResponse series, AcademyAwardDTO academyAwards) {
        this.movies = movies;
        this.series = series;
        this.academyAwards = academyAwards;
    }

}
