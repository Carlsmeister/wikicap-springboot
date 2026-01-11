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

function renderSpotifySongs(songs, year) {
    clearSpotify();
    if (!songs || songs.length === 0) return;
    document.querySelector(".spotify-year-label").textContent = year;

    spotifySection.classList.remove("hidden"); //Tar bort attributet "hidden" för att visa sektionen

    for (const song of songs) {  //Skapar ett kort för varje låt i Songs
        const card = spotifyCardTpl.content.firstElementChild.cloneNode(true);

        card.querySelector(".song-title").textContent = song.title;
        card.querySelector(".song-artist").textContent = song.artist;
        card.querySelector(".song-album").textContent = "📀 Album: " + song.album;
        card.querySelector(".song-release-date").textContent = "📅 Release Date: " + song.releaseDate;

        const songLink = card.querySelector(".song-link");
        songLink.href = song.spotifyUrl;

        const albumImage = card.querySelector(".spotify-album-image");
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
        renderSpotifySongs(songs, year);

        observeSpotifyCards();
    });
    

    function observeSpotifyCards() { //Observer för att fadea in/out korten vid scroll
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add("opacity-100");  // Gör kortet synligt
                entry.target.classList.remove("opacity-0");
            } else {
                entry.target.classList.add("opacity-0");
                entry.target.classList.remove("opacity-100");
            }
            
            });
        }, { threshold: 0.6 //Hur mycket av kortet som måste vara synligt för att trigga observern
    });
    const spotifyCards = document.querySelectorAll(".spotify-card-wrapper");

    spotifyCards.forEach(card => {
        observer.observe(card);
    });

}
    