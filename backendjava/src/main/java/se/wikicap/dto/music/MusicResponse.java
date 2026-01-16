package se.wikicap.dto.music;

import se.wikicap.dto.music.spotify.SpotifyArtistSearchResponse;
import se.wikicap.dto.music.spotify.SpotifyTrackSearchResponse;

import java.util.List;

public record MusicResponse(
    int year,
    List<TrackDTO> topTracks,
    List<ArtistDTO> topArtists,
    String source
) {
    public record TrackDTO(
        Integer rank,
        String participants,
        SpotifyTrackSearchResponse.TrackItem trackData
    ) {}

    public record ArtistDTO(
        Integer rank,
        SpotifyArtistSearchResponse.ArtistItem artistData,
        List<SpotifyTrackSearchResponse.TrackItem> topTracks
    ) {}
}
