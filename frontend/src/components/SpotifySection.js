const API_BASE = "http://127.0.0.1:8000";

const spotifySection = document.querySelector("#spotifySection");
const spotifyGrid = document.querySelector("#spotifyGrid");
const spotifyCardTpl = document.querySelector("#spotifyCardTpl");


async function fetchSpotifySongs(year) {
    const res = await fetch(`${API_BASE}/api/v1/year/${year}/songs`);
    if (!res.ok) return null;
    const data = await res.json();
    return data.topSongs ?? [];
  }

function clearSpotify() {
    spotifyGrid.innerHTML = "";
    spotifySection.classList.add("hidden");
}

/**
 * Renders card for Spotify songs
 * @param {Array} songs an array of song objects
 */

function renderSpotifySongs(songs) {
    clearSpotify();
    if (!songs || songs.length === 0) return;

    spotifySection.classList.remove("hidden");

    for (const song of songs) {
        const card = spotifyCardTpl.content.firstElementChild.cloneNode(true);

        card.querySelector(".song-title").textContent = song.title;
        card.querySelector(".song-artist").textContent = song.artist;
        card.querySelector(".song-album").textContent = "📀 Album: " + song.album;
        card.querySelector(".song-release-date").textContent = "📅 Release Date: " + song.releaseDate;

        const songLink = card.querySelector(".song-link");
        songLink.href = song.spotifyUrl;

        const albumImage = card.querySelector(".album-image");
        albumImage.href = song.spotifyUrl;

        const image = card.querySelector("img");
        if (image) {
            image.src = song.image || "src/img/spotify_logo.png";
        }

        spotifyGrid.appendChild(card);

        }
    }

document.getElementById("yearForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const year = document.getElementById("yearInput").value.trim();
  if (!year) return;

  const songs = await fetchSpotifySongs(year);
  renderSpotifySongs(songs);
});