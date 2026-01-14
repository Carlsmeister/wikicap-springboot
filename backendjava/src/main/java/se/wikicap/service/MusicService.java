package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import se.wikicap.dto.MusicResponseDTO;

@Service
public class MusicService {

    /**
     * Fetch music data for a specific year.
     *
     * @param year The year to fetch music data for
     * @return Mono<MusicResponseDTO> containing music data
     */
    public Mono<MusicResponseDTO> getMusicByYear(int year) {
        //TODO: Implement method to fetch music data by year
        return Mono.empty();  // Return empty Mono instead of null
    }
}
