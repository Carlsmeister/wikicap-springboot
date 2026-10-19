// Maps the Java backend's JSON to the shapes the components read (the old Python API's shapes).

export function toEventsByMonth(events) {
  return Object.fromEntries(
    Object.entries(events.eventsByMonth).map(([month, items]) => [
      month,
      items.map(({ date, description }) => (date ? `${date} - ${description}` : description)),
    ]),
  );
}

export function toNobelPrizes(nobel) {
  return Object.fromEntries(
    nobel.nobelPrizes.map((prize) => [
      prize.category.en,
      prize.laureates.map((l) => ({
        name: l.knownName?.en,
        motivation: l.motivation?.en,
        image: l.imageURL ?? "",
      })),
    ]),
  );
}

export function toHighlights(entertainment) {
  const oscars = entertainment.academyAwards?.oscars;
  return oscars?.bestPicture && oscars.bestActor && oscars.bestActress ? { oscars } : null;
}

const toScreenItem = (title, release_date, item) => ({
  title,
  rating: Math.round(item.vote_average * 10) / 10,
  votes: item.vote_count,
  poster: item.poster_path,
  release_date,
});

export function toTopMovies(entertainment) {
  return entertainment.movies.results.map((m) => toScreenItem(m.title, m.release_date, m));
}

export function toTopSeries(entertainment) {
  return entertainment.series.results.map((s) => toScreenItem(s.name, s.first_air_date, s));
}

export function toTopSongs(music) {
  return music.topTracks
    .filter((t) => t.trackData)
    .map(({ trackData: t }) => ({
      title: t.name,
      artist: t.artists[0].name,
      album: t.album.name,
      release_date: t.album.release_date,
      // Java's no-match fallback sends external_urls: null (MusicService.createEmptySong).
      spotify_url: t.external_urls?.spotify,
      image: t.album.images[0]?.url ?? "",
    }));
}

export function toTopArtists(music) {
  return music.topArtists
    .filter((a) => a.artistData)
    .map(({ artistData: a }) => ({ name: a.name, image: a.images[0]?.url ?? "" }));
}

export function toTopTracksByArtist(music) {
  return music.topArtists
    .filter((a) => a.artistData)
    .map((a) => ({ artist: a.artistData.name, top_tracks: a.topTracks.map((t) => ({ title: t.name })) }));
}