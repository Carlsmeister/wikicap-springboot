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

    private final MusicService musicService;
    private final EntertainmentService entertainmentService;
    private final EventService eventService;
    private final NobelService nobelService;

    public YearController(MusicService musicService, EntertainmentService entertainmentService, EventService eventService, NobelService nobelService) {
        this.musicService = musicService;
        this.entertainmentService = entertainmentService;
        this.eventService = eventService;
        this.nobelService = nobelService;
    }

    @GetMapping("/{year}")
    public YearResponseDTO getYear(@PathVariable int year) {
        CompletableFuture<MusicResponseDTO> musicFuture = musicService.getMusicByYear(year);
        CompletableFuture<EntertainmentResponseDTO> entertainmentFuture = entertainmentService.getEntertainmentByYear(year);
        CompletableFuture<EventResponseDTO> eventFuture = eventService.getEventsByYear(year);
        CompletableFuture<NobelResponseDTO> nobelFuture = nobelService.getNobelByYear(year);

        CompletableFuture.allOf(musicFuture, entertainmentFuture, eventFuture, nobelFuture).join();

        try {
            MusicResponseDTO music = musicFuture.get();
            EntertainmentResponseDTO entertainment = entertainmentFuture.get();
            EventResponseDTO events = eventFuture.get();
            NobelResponseDTO nobel = nobelFuture.get();

            return new YearResponseDTO(year, music, entertainment, events, nobel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch year data", e);
        }
    }

    @GetMapping("{year}/entertainment")
    public CompletableFuture<EntertainmentResponseDTO> getEntertainmentByYear(@PathVariable int year) {
        return entertainmentService.getEntertainmentByYear(year);
    }
}
