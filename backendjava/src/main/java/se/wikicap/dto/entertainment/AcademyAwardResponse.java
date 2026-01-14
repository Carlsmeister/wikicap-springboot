package se.wikicap.dto.entertainment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Response wrapper for Academy Awards API data.
 * Matches the Python backend structure with bestActor, bestActress, and bestPicture.
 */
@Setter
@Getter
public class AcademyAwardResponse {
    private Integer year;
    private OscarsData oscars;
    private String source = "The Unofficial Awards API";

    public AcademyAwardResponse() {
        this.oscars = new OscarsData();
    }

    /**
     * Container for the three main Oscar categories.
     */
    @Setter
    @Getter
    public static class OscarsData {
        @JsonProperty("bestActor")
        private ActorAward bestActor;

        @JsonProperty("bestActress")
        private ActorAward bestActress;

        @JsonProperty("bestPicture")
        private PictureAward bestPicture;
    }

    /**
     * Award data for Best Actor / Best Actress.
     * Includes the winner's name, movie, and profile image from TMDB.
     */
    @Setter
    @Getter
    public static class ActorAward {
        private String name;
        private String movie;
        private String image;

        private Integer id;
        private String more;
        private String note;
        private Boolean winner;
    }

    /**
     * Award data for Best Picture.
     * Includes the movie title and poster from TMDB.
     */
    @Setter
    @Getter
    public static class PictureAward {
        private String title;
        private String poster;

        private Integer id;
        private String more;
        private String note;
        private Boolean winner;
    }
}

