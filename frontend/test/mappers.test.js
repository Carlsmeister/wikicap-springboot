import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  toEventsByMonth,
  toNobelPrizes,
  toHighlights,
  toTopMovies,
  toTopSeries,
  toTopSongs,
  toTopArtists,
  toTopTracksByArtist,
} from "../src/js/mappers.js";

const load = (name) => JSON.parse(readFileSync(new URL(`./fixtures/${name}`, import.meta.url)));
const events = load("1994-events.json");
const nobel = load("1994-nobel.json");
const entertainment = load("1994-entertainment.json");
const music = load("1994-music.json");

test("toEventsByMonth prefixes dated events with their date", () => {
  const item = events.eventsByMonth.January[0];
  assert.equal(toEventsByMonth(events).January[0], `${item.date} - ${item.description}`);
});

test("toEventsByMonth leaves undated events without a prefix", () => {
  const index = events.eventsByMonth.January.findIndex((e) => e.date === "");
  assert.notEqual(index, -1);
  assert.equal(toEventsByMonth(events).January[index], events.eventsByMonth.January[index].description);
});

test("toNobelPrizes groups laureates by English category name", () => {
  const prizes = toNobelPrizes(nobel);
  assert.deepEqual(Object.keys(prizes), nobel.nobelPrizes.map((p) => p.category.en));
  const source = nobel.nobelPrizes[0];
  const laureate = source.laureates[0];
  assert.deepEqual(prizes[source.category.en][0], {
    name: laureate.knownName.en,
    motivation: laureate.motivation.en,
    image: laureate.imageURL,
  });
});

test("toNobelPrizes keeps a laureate with no known name", () => {
  const input = structuredClone(nobel);
  delete input.nobelPrizes[0].laureates[0].knownName;
  const prizes = toNobelPrizes(input);
  assert.equal(prizes[input.nobelPrizes[0].category.en][0].name, undefined);
});

test("toNobelPrizes keeps a prize with no laureates as an empty category", () => {
  const input = structuredClone(nobel);
  delete input.nobelPrizes[0].laureates;
  assert.deepEqual(toNobelPrizes(input)[input.nobelPrizes[0].category.en], []);
});

test("toHighlights returns the oscars when all three categories exist", () => {
  assert.deepEqual(toHighlights(entertainment), { oscars: entertainment.academyAwards.oscars });
});

test("toHighlights returns null when an Oscar category is missing", () => {
  const input = structuredClone(entertainment);
  delete input.academyAwards.oscars.bestActress;
  assert.equal(toHighlights(input), null);
});

test("toTopMovies renames TMDB fields and rounds the rating", () => {
  const movie = entertainment.movies.results[0];
  const [first] = toTopMovies(entertainment);
  assert.equal(first.title, movie.title);
  assert.equal(first.votes, movie.vote_count);
  assert.equal(first.poster, movie.poster_path);
  assert.equal(first.rating, Math.round(movie.vote_average * 10) / 10);
});

test("toTopSeries maps name and first air date", () => {
  const show = entertainment.series.results[0];
  const [first] = toTopSeries(entertainment);
  assert.equal(first.title, show.name);
  assert.equal(first.release_date, show.first_air_date);
});

test("toTopSongs flattens Spotify track data", () => {
  const track = music.topTracks[0].trackData;
  assert.deepEqual(toTopSongs(music)[0], {
    title: track.name,
    artist: track.artists[0].name,
    album: track.album.name,
    release_date: track.album.release_date,
    spotify_url: track.external_urls.spotify,
    image: track.album.images[0].url,
  });
});

test("toTopSongs skips ranks with no Spotify match", () => {
  const input = structuredClone(music);
  // Java's no-match fallback, as built by MusicService.createEmptySong.
  input.topTracks.push({
    rank: 99,
    participants: "Unmatched Artist",
    trackData: {
      id: null,
      name: "Unmatched Song",
      popularity: 0,
      external_urls: null,
      album: { name: "Unknown", release_date: "Unknown", images: [] },
      artists: [{ id: null, name: "Unmatched Artist" }],
    },
  });
  const matched = input.topTracks.filter((t) => t.trackData?.external_urls?.spotify).length;
  assert.equal(matched, input.topTracks.length - 2); // the fallback plus the fixture's null trackData
  const songs = toTopSongs(input);
  assert.equal(songs.length, matched);
  assert.ok(!songs.some((s) => s.title === "Unmatched Song"));
});

test("toTopArtists takes the first image or an empty string", () => {
  const index = music.topArtists.findIndex((a) => a.artistData.images.length === 0);
  assert.notEqual(index, -1);
  const artists = toTopArtists(music);
  assert.equal(artists[0].image, music.topArtists[0].artistData.images[0].url);
  assert.equal(artists[index].image, "");
});

test("toTopTracksByArtist pairs each artist with its track titles", () => {
  const source = music.topArtists[0];
  assert.deepEqual(toTopTracksByArtist(music)[0], {
    artist: source.artistData.name,
    top_tracks: source.topTracks.map((t) => ({ title: t.name })),
  });
});

// Java's no-match fallback, as built by MusicService.createEmptyArtist.
const unmatchedArtist = () => ({
  rank: 99,
  artistData: {
    id: null,
    name: "Unmatched Artist",
    genres: [],
    popularity: 0,
    followers: { total: 0 },
    external_urls: null,
    images: [],
  },
  topTracks: [],
});

test("toTopArtists skips artists with no Spotify match", () => {
  const input = structuredClone(music);
  input.topArtists.push(unmatchedArtist());
  const artists = toTopArtists(input);
  assert.equal(artists.length, music.topArtists.length);
  assert.ok(!artists.some((a) => a.name === "Unmatched Artist"));
});

test("toTopTracksByArtist skips artists with no Spotify match", () => {
  const input = structuredClone(music);
  input.topArtists.push(unmatchedArtist());
  const byArtist = toTopTracksByArtist(input);
  assert.equal(byArtist.length, music.topArtists.length);
  assert.ok(!byArtist.some((a) => a.artist === "Unmatched Artist"));
});

test("toTopTracksByArtist keeps at most five tracks per artist", () => {
  const input = structuredClone(music);
  const source = input.topArtists[0];
  source.topTracks = Array.from({ length: 7 }, (_, i) => ({ name: `Track ${i + 1}` }));
  assert.deepEqual(toTopTracksByArtist(input)[0].top_tracks, [1, 2, 3, 4, 5].map((n) => ({ title: `Track ${n}` })));
});
