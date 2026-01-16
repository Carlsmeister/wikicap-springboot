package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.wikicap.dto.music.MusicResponse;
import se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse;
import se.wikicap.service.MusicService;

/**
 * REST controller for music endpoints.
 *
 * Base path: /api/v1/years
 *
 * Delegates to {@link MusicService} for aggregation and enrichment.
 */
@RestController
@RequestMapping("/api/v1/years")
public class MusicController {

    private final MusicService musicService;

    /**
     * Creates a new {@link MusicController}.
     *
     * @param musicService service that provides music aggregation logic
     */
    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    /**
     * Returns aggregated music data for a year.
     *
     * Path: GET /api/v1/years/{year}/music
     *
     * @param year year to fetch
     * @return a {@link Mono} emitting a {@link MusicResponse}
     */
    @GetMapping("/{year}/music")
    public Mono<MusicResponse> getMusicByYear(@PathVariable int year) {
        return musicService.getMusicByYear(year);
    }

    /**
     * Returns the top artists for a year.
     *
     * Path: GET /api/v1/years/{year}/music/artists
     *
     * @param year year to fetch
     * @return a {@link Mono} emitting a {@link SpotifyArtistSearchResponse}
     */
    @GetMapping("/{year}/music/artists")
    public Mono<SpotifyArtistSearchResponse> getTopArtistsByYear(@PathVariable int year) {
        return musicService.getTopArtistsByYear(year);
    }

    /**
     * Returns the top tracks for a year.
     *
     * Path: GET /api/v1/years/{year}/music/tracks
     *
     * @param year year to fetch
     * @return a {@link Mono} emitting a {@link MusicResponse} (tracks only)
     */
    @GetMapping("/{year}/music/tracks")
    public Mono<MusicResponse> getTopTracksByYear(@PathVariable int year) {
        return musicService.getTopTracksByYear(year);
    }
}
