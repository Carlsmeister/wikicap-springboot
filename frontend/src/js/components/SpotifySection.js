const API_BASE = "http://127.0.0.1:8000";

const spotifySection = document.querySelector("#spotifySection");
const spotifyGrid = document.querySelector("#spotifyGrid");
const spotifyCardTpl = document.querySelector("#spotifyCardTpl");


/**
 * Fetches the top Spotify songs for a given year from the API.
 * @async - To prevent blocking behavior
 * @param {number} year - The year for which to fetch top songs.
 * @returns {Promise<Array>} A promise that resolves to an array of top songs, or an empty array if the request fails.
 */
async function fetchSpotifySongs(year) {
    const res = await fetch(`${API_BASE}/api/v1/year/${year}/songs`);
    if (!res.ok) return null;
    const data = await res.json();
    return data.top_songs ?? [];
  }

/**
 * Clears the Spotify section by removing all grid content and hides the section.
 * @function clearSpotify
 * @returns {void}
 */
function clearSpotify() {
    spotifyGrid.innerHTML = "";
    spotifySection.classList.add("hidden");
}

/**
 * Renders card for Spotify songs
 * @function renderSpotifySongs
 * @param {Array} songs - An array of song objects
 * @param {number} year - The year associated with the songs
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
        card.querySelector(".song-release-date").textContent = "📅 Release Date: " + song.release_date;

        const songLink = card.querySelector(".song-link");
        songLink.href = song.spotify_url;

        const albumImage = card.querySelector(".spotify-album-image");
        albumImage.href = song.spotify_url;

        const image = card.querySelector("img");
        if (image) {
            image.src = song.image || "src/img/spotify_logo.png";
        }

        spotifyGrid.appendChild(card); //Lägger till kortet i grid
        }
    }

    document.getElementById("yearForm").addEventListener("submit", async (e) => { //Listens for the submit button
        e.preventDefault();
        const year = document.getElementById("yearInput").value.trim();
        if (!year) return;
        
        const songs = await fetchSpotifySongs(year);
        renderSpotifySongs(songs, year);

        observeSpotifyCards();
    });
    

    /**
     * Observes Spotify cards and applies fade in/out effects based on scroll position.
     * Uses the Intersection Observer API to detect when cards enter or leave the viewport,
     * toggling opacity classes to create a smooth fade effect.
     * 
     * @function observeSpotifyCards
     * @returns {void}
     * 
     */
    function observeSpotifyCards() { //Observer for fading in/out cards on scroll
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add("opacity-100");  // Makes the card visible
                entry.target.classList.remove("opacity-0");
            } else {
                entry.target.classList.add("opacity-0");  //Hides the card again when scrolling out of view
                entry.target.classList.remove("opacity-100");
            }
            
            });
        }, { threshold: 0.6 //How much of the card to be visible before triggering the observer
    });
    const spotifyCards = document.querySelectorAll(".spotify-card-wrapper");

    spotifyCards.forEach(card => {
        observer.observe(card);
    });

}
