package se.wikicap.dto.entertainment;

import lombok.Getter;
import lombok.Setter;

/**
 * DTOs for Academy Awards API responses.
 * Different endpoints return different structures.
 */
public class AcademyAwardDTO {

    /**
     * Edition DTO - returned by /oscars/editions?year=X
     * Example: {"id": 73, "name": "73rd Academy Awards", "edition": 73, "year": 2000}
     */
    @Setter
    @Getter
    public static class Edition {
        private Integer id;
        private String name;
        private Integer edition;
        private Integer year;
    }

    /**
     * Category DTO - returned by /oscars/{id}/categories
     * Example: {"id": 4576, "name": "Actor In A Leading Role"}
     */
    @Setter
    @Getter
    public static class Category {
        private Integer id;
        private String name;
    }

    /**
     * Nominee DTO - returned by /oscars/{id}/categories/{id}/nominees
     * Example: {"id": 2740, "name": "Marlon Brando", "more": "The Godfather...", "note": "...", "winner": true}
     */
    @Setter
    @Getter
    public static class Nominee {
        private Integer id;
        private String name;
        private String more;
        private String note;
        private Boolean winner;
    }
}
