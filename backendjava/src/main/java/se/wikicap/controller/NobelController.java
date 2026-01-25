package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.wikicap.dto.nobel.NobelResponse;
import se.wikicap.service.NobelService;

@RestController
@RequestMapping("api/v1/years")
public class NobelController {

    private final NobelService nobelService;

    public NobelController(NobelService nobelService) {
        this.nobelService = nobelService;
    }

    @GetMapping("/{year}/nobel")
    public Mono<NobelResponse> getNobelByYear(@PathVariable int year) {

        return nobelService.getNobelPrizesByYear(year);
    }

}
