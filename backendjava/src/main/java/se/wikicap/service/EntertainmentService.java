package se.wikicap.service;

import org.springframework.stereotype.Service;
import se.wikicap.dto.*;
import se.wikicap.client.EntertainmentClient;

import java.util.concurrent.CompletableFuture;

@Service
public class EntertainmentService {

    private final EntertainmentClient entertainmentClient;

    public EntertainmentService(EntertainmentClient entertainmentClient) {
        this.entertainmentClient = entertainmentClient;
    }

    public CompletableFuture<EntertainmentResponseDTO> getEntertainmentByYear(int year) {
        return CompletableFuture.supplyAsync(() -> {
            EntertainmentResponseDTO entertainmentData = new EntertainmentResponseDTO();

            TMBDMovieResponse movies = entertainmentClient.fetchTopMoviesByYear(year);
            TMBDSeriesResponse series = entertainmentClient.fetchTopSeriesByYear(year);
            AcademyAwardDTO academyAwards = entertainmentClient.fetchAwards(year);

             entertainmentData.setMovies(movies);
             entertainmentData.setSeries(series);
             entertainmentData.setAcademyAwards(academyAwards);

            return entertainmentData;
        });
    }
}
