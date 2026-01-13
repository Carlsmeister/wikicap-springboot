package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.wikicap.dto.*;
import se.wikicap.service.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/year")
public class YearController {

    private final MusicService musicSerivce;
    private final EntertainmentService entertainmentService;
    private final EventService eventService;
    private final NobelService nobelService;
    private final AcademyAwardService academyAwardService;

    public YearController(MusicService musicService, EntertainmentService entertainmentService, EventService eventService, NobelService nobelService, AcademyAwardService academyAwardService) {
        this.musicSerivce = musicService;
        this.entertainmentService = entertainmentService;
        this.eventService = eventService;
        this.nobelService = nobelService;
        this.academyAwardService = academyAwardService;

    }

    @GetMapping("/{year}")
    public YearResponseDTO getYear(@PathVariable int year) {
        CompletableFuture<MusicResponseDTO> musicFuture = musicSerivce.getMusicByYear(year);
        CompletableFuture<EntertainmentResponseDTO> entertainmentFuture = entertainmentService.getEntertainmentByYear(year);
        CompletableFuture<EventResponseDTO> eventFuture = eventService.getEventsByYear(year);
        CompletableFuture<NobelResponseDTO> nobelFuture = nobelService.getNobelByYear(year);
        CompletableFuture<AcademyAwardResponseDTO> academyAwardFuture = academyAwardService.getAcademyAwardsByYear(year);

        CompletableFuture.allOf(musicFuture, entertainmentFuture, eventFuture, nobelFuture, academyAwardFuture).join();

        try {
            MusicResponseDTO music = musicFuture.get();
            EntertainmentResponseDTO entertainment = entertainmentFuture.get();
            EventResponseDTO events = eventFuture.get();
            NobelResponseDTO nobel = nobelFuture.get();
            AcademyAwardResponseDTO academyAwards = academyAwardFuture.get();

            return new YearResponseDTO(year, music, entertainment, events, nobel, academyAwards);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch year data", e);
        }
    }
}
