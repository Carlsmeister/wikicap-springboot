package se.wikicap.service;

import org.springframework.stereotype.Service;
import se.wikicap.dto.EventResponseDTO;

import java.util.concurrent.CompletableFuture;

@Service
public class EventService {
    public CompletableFuture<EventResponseDTO> getEventsByYear(int year) {
        //TODO: Implement fetching events by year
        return null;
    }
}
