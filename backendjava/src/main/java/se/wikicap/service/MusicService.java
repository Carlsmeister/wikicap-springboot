package se.wikicap.service;

import org.springframework.stereotype.Service;
import se.wikicap.dto.MusicResponseDTO;

import java.util.concurrent.CompletableFuture;

@Service
public class MusicService {
    public CompletableFuture<MusicResponseDTO> getMusicByYear(int year) {
        //TODO: Implement method to fetch music data by year
        return null;
    }
}
