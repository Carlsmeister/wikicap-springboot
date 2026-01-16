package se.wikicap.service;

import reactor.core.publisher.Mono;
import java.util.List;

public interface MusicRankProvider {
    /**
     * Get a list of top songs for a specific year.
     * @param year The year to fetch rankings for.
     * @return Mono containing a list of song titles and artists.
     */
    Mono<List<RankedSong>> getTopSongs(int year);

    /**
     * Get a list of top artists for a specific year.
     * @param year The year to fetch rankings for.
     * @return Mono containing a list of artist names.
     */
    Mono<List<RankedArtist>> getTopArtists(int year);

    record RankedSong(String title, String displayName, String primaryArtist, int rank) {}
    record RankedArtist(String displayName, String primaryArtist, int rank, int occurrenceCount) {}
}
