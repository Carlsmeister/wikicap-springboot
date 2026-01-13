package se.wikicap.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data Transfer Object for Yearly Summary Response")
public class YearResponseDTO {

    @Schema (description = "The year for which the summary is provided", example = "2023")
    private int year;

    @Schema (description = "Music related data for the year")
    private MusicResponseDTO music;

    @Schema (description = "Entertainment related data for the year")
    private EntertainmentResponseDTO entertainment;

    @Schema (description = "Significant events for the year")
    private EventResponseDTO events;

    @Schema (description = "Nobel Prize related data for the year")
    private NobelResponseDTO nobel;

    @Schema (description = "Academy Awards (Oscar) related data for the year")
    private AcademyAwardResponseDTO academyAwards;

    public YearResponseDTO(MusicResponseDTO music, EntertainmentResponseDTO entertainment, EventResponseDTO events, NobelResponseDTO nobel, AcademyAwardResponseDTO academyAwards) {
        this.music = music;
        this.entertainment = entertainment;
        this.events = events;
        this.nobel = nobel;
        this.academyAwards = academyAwards;
    }

    public MusicResponseDTO getMusic() {
        return music;
    }

    public EntertainmentResponseDTO getEntertainment() {
        return entertainment;
    }

    public EventResponseDTO getEvents() {
        return events;
    }

    public NobelResponseDTO getNobel() {
        return nobel;
    }

    public AcademyAwardResponseDTO getAcademyAwards() {
        return academyAwards;
    }

}
