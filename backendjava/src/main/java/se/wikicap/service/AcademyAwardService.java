package se.wikicap.service;

import org.springframework.stereotype.Service;
import se.wikicap.dto.AcademyAwardResponseDTO;

import java.util.concurrent.CompletableFuture;

@Service
public class AcademyAwardService {

    public CompletableFuture<AcademyAwardResponseDTO> getAcademyAwardsByYear(int year) {
        //TODO: Implement method to fetch Academy Awards data for the given year
        return null;
    }
}
