package se.wikicap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import se.wikicap.dto.entertainment.EntertainmentResponse;
import se.wikicap.dto.music.MusicResponse;
import se.wikicap.dto.nobel.NobelResponse;

@Schema(description = "Data Transfer Object for Yearly Summary Response")
public class YearResponse {

    @Getter
    @Schema (description = "The year for which the summary is provided", example = "2023")
    private int year;

    @Getter
    @Schema (description = "Music related data for the year")
    private MusicResponse music;

    @Getter
    @Schema (description = "Entertainment related data for the year")
    private EntertainmentResponse entertainment;

    @Getter
    @Schema (description = "Significant events for the year")
    private EventResponse events;

    @Getter
    @Schema (description = "Nobel Prize related data for the year")
    private NobelResponse nobel;

    public YearResponse(int year, MusicResponse music, EntertainmentResponse entertainment, EventResponse events, NobelResponse nobel) {
        this.year = year;
        this.music = music;
        this.entertainment = entertainment;
        this.events = events;
        this.nobel = nobel;
    }

}
