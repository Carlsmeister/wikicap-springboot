package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.wikicap.dto.music.MusicResponse;
import se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse;
import se.wikicap.service.MusicService;

@RestController
@RequestMapping("/api/v1/years")
public class MusicController {

    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/{year}/music")
    public Mono<MusicResponse> getMusicByYear(@PathVariable int year) {
        return musicService.getMusicByYear(year);
    }

    @GetMapping("/{year}/music/artists")
    public Mono<SpotifyArtistSearchResponse> getTopArtistsByYear(@PathVariable int year) {
        return musicService.getTopArtistsByYear(year);
    }

    @GetMapping("/{year}/music/tracks")
    public Mono<MusicResponse> getTopTracksByYear(@PathVariable int year) {
        return musicService.getTopTracksByYear(year);
    }
}

