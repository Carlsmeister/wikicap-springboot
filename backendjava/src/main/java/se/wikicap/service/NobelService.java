package se.wikicap.service;

import org.springframework.stereotype.Service;
import se.wikicap.dto.NobelResponseDTO;

import java.util.concurrent.CompletableFuture;

@Service
public class NobelService {
    public CompletableFuture<NobelResponseDTO> getNobelByYear(int year) {
        //TODO: Implement Nobel data fetching logic
        return null;
    }
}
