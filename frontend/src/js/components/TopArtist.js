const API_BASE = "http://127.0.0.1:8000";

/** Section containing the top artists view.
 * @type {HTMLElement}
 */
const ArtistSection = document.querySelector("#ArtistSection");

/** Grid container for artist cards.
 * @type {HTMLElement}
 */
const ArtistGrid = document.querySelector("#ArtistGrid");

/** Template for rendering individual artist cards.
 * @type {HTMLTemplateElement}
 */
const ArtistTpl = document.querySelector("#ArtistCardTpl");

/** Element displaying statistics for the selected year.
 * @type {HTMLElement}
 */
const statsEl = document.querySelector("#ArtistStats");

/** Cleanup function for the auto-reveal animation of artist cards.
 * If this variable holds a function (not null), calling it will stop the animation.
 */
let cleanupArtistAutoReveal = null;

/** Sets up the auto‑reveal animation for artist cards.
 * Shows a few cards immediately, then reveals the rest one by one.
 * Replaces any previous auto‑reveal setup by running its cleanup function.
 * @param {Object} options
 * @param {number} [options.initialVisible=3] - Number of cards shown immediately.
 * @param {number} [options.delayMs=1000] - Delay between reveals in milliseconds.
 * @param {number} [options.durationMs=1100] - Transition duration for each card.
 */ 
function setupArtistAutoReveal({
  initialVisible = 3,
  delayMs = 1000, //styr att det blir 1 sekund mellan varje nytt kort. 
  durationMs = 1100, //styr så att animationen tar 1,1 sek. (långsammare)

} = {}) {
  if (cleanupArtistAutoReveal) cleanupArtistAutoReveal();

  const cards = Array.from(document.querySelectorAll("#ArtistGrid .pin-card"));
  if (cards.length === 0) return;


  function showCard(card) {
    card.classList.remove("opacity-0", "translate-y-6", "blur-sm", "pointer-events-none");
    card.classList.add("opacity-100", "translate-y-0", "blur-0");
  }

  // Sätt långsammare transition (override på varje kort)
    cards.forEach(card => {
    card.style.transitionDuration = `${durationMs}ms`;
  });

  // Visa första "radens" kort direkt
  const firstCount = Math.min(initialVisible, cards.length);
  for (let index = 0; index < firstCount; index++) showCard(cards[index]);

  //Visa upp resterande kort automatiskt. 
  let index = firstCount;
  const timer = setInterval(() => {
    if (index >= cards.length) {
      clearInterval(timer);
      return;
    }
    showCard(cards[index]);
    index++;
  }, delayMs);

  cleanupArtistAutoReveal = () => {
    clearInterval(timer);
    cleanupArtistAutoReveal = null;
  };

}


/** Clears the current "Top Artist" view.
 * Removes any running auto‑reveal animation, empties the artist grid,
 * hides the artist section, and resets the stats text.
 */  
  function clearTopArtist() { 
  if (cleanupArtistAutoReveal) cleanupArtistAutoReveal();
  if (ArtistGrid) ArtistGrid.innerHTML = "";
  if (ArtistSection) ArtistSection.classList.add("hidden");
  if (statsEl) statsEl.textContent = "";
}

/** Fetches the top artist for a given year from the API.
 *
 * Sends a request to the backend and returns the parsed JSON data.
 * If the request fails (for example due to a network error or a 404),
 * the function returns an empty object instead of throwing an error.
 *
 * @param {number} year - The year to fetch artist data for.
 * @returns {Promise<Object>} The artist data, or an empty object if the request fails. 
 * */
  async function fetchArtistOfTheYear(year) {
    const res = await fetch(`${API_BASE}/api/v1/year/${year}/billboard/artist`);
    if (!res.ok) return {};
    const data = await res.json();
    return data ?? {};
  }
/**
 * Renders the "Top Artist" section for a given year.
 *
 * Clears any previous artist view, fetches top tracks for each artist,
 * builds up to six artist cards from the template, and inserts them
 * into the artist grid. Each card gets a placeholder image until the
 * real image loads. If an image fails to load, the placeholder remains.
 *
 * After rendering, the cards are revealed with an auto‑reveal animation,
 * and the page scrolls smoothly to the artist section.
 *
 * @param {Object} artistData - Data containing artists and optional images.
 * @param {number} year - The selected year to fetch top songs for.
 */  
async function renderTopArtist(artistData, year) {
  clearTopArtist();

  if (!artistData || !ArtistTpl || !ArtistGrid || !ArtistSection) return;

  ArtistSection.classList.remove("hidden");

  const yearLabel = ArtistSection.querySelector(".artist-year-label");
  if (yearLabel) yearLabel.textContent = year;

  let topTracksByArtist = new Map();
  try {
    const topSongsData = await fetchBillboardTopSong(year, 5);
    topTracksByArtist = new Map(
      (topSongsData.artists ?? []).map(a => [a.artist, a.top_tracks ?? []])
    );
  } catch (err) {
    console.error("Top songs fetch failed:", err);
  }

  const artistsList = artistData.artists_with_images ?? artistData.artists ?? [];
  const maxArtistsCards = 6;

  for (let index = 0; index < Math.min(maxArtistsCards, artistsList.length); index++) {
    const artistEntry = artistsList[index];

    const artistName =
      typeof artistEntry === "string" ? artistEntry : (artistEntry?.name ?? "");

    const imgUrl =
      typeof artistEntry === "string" ? "" : (artistEntry?.image ?? "");

    const node = ArtistTpl.content.firstElementChild.cloneNode(true);

    node.classList.add(
      "pin-card",
      "opacity-0",
      "translate-y-6",
      "blur-sm",
      "pointer-events-none",
      "transition-all",
      "duration-500",
      "will-change-transform"
    );

    node.querySelector(".name").textContent = artistName;


  const img = node.querySelector("img");
  const placeholder =
    "data:image/svg+xml;utf8," +
    encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" width="128" height="128">
        <rect width="100%" height="100%" rx="18" ry="18" fill="#E5E7EB"/>
        <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
              font-family="Arial" font-size="14" fill="#111827">
          No Image
        </text>
      </svg>
    `);


    if (img) {
      img.loading = "lazy";
      img.src = placeholder;
      img.alt = `Portrait of ${artistName}`;

      img.onerror = () => {
        img.onerror = null; // undvik ev loop
        img.src = placeholder;
      };

      const cleanUrl =
        imgUrl && imgUrl !== "null" && imgUrl !== "None" ? imgUrl : "";

      if (cleanUrl) img.src = cleanUrl;
    }

    
    const topTracks = topTracksByArtist.get(artistName) ?? [];
    renderTopSongs(topTracks, node);

    ArtistGrid.appendChild(node);
  }

  setupArtistAutoReveal({
    initialVisible: 3,
    delayMs: 1000,
    durationMs: 1200
  });

  // requestAnimationFrame(() => {
  //   const position = ArtistSection.getBoundingClientRect().top + window.scrollY;
  //   const offset = 220;
  //   window.scrollTo({ top: position - offset, behavior: "smooth" });
  // });
}

  /** Fetches the top songs for a given year from the Billboard API.
   *
   * Sends a request to the backend and returns the parsed JSON response.
   * If the request fails, the function throws an error instead of returning fallback data.
   * 
   * @param {number} year - The year to fetch songs for.
   * @param {number} [limit=5] - Maximum number of songs to return.
   * @returns {Promise<Object>} Parsed JSON response from the API.
  */  
  async function fetchBillboardTopSong(year) {
    const res = await fetch(
      `${API_BASE}/api/v1/year/${year}/billboard/artist/top-songs`);
    if (!res.ok) {
      throw new Error("Failed to fetch top songs");
    }
      return await res.json();
  }

  /**
   * Renders a list of songs inside a given container.
   * The function clears any previous content,
   * adds a heading, and creates a numbered list of song titles.
   *
   * Each item in `songs` can be either a string or an object with a `title` property.
   * Empty or invalid entries are skipped.
   *
   * @param {Array<string|Object>} songs - The songs to display.
   * @param {HTMLElement} container - The element where the list should be rendered.
   */  
  function renderTopSongs(songs, container) {
    if (!songs || songs.length === 0) return;

    const listOfSongs = container.querySelector(".songs") ?? container;
    listOfSongs.innerHTML = ""; 

    const heading = document.createElement("p");
    heading.className = "mt-2 text-xs font-semibold tracking-wide uppercase text-slate-900";
    heading.textContent = "All-time Top Songs";
    listOfSongs.appendChild(heading);

    const ol = document.createElement("ol");
    ol.className = "mt-2 text-sm list-decimal pl-0 m-0";

    ol.style.listStyleType = "decimal";
    ol.style.listStylePosition = "inside";

    songs.forEach(song => {
      const title = typeof song === "string" ? song : song?.title;
      if (!title) return;

      const li = document.createElement("li");
      li.textContent = title;
      ol.appendChild(li);
    });
     listOfSongs.appendChild(ol);
  }
    
/** Fetches and renders the top artists for a given year.
 * @param {number|string} year - The year to retrieve artist data for.
 * @returns {Promise<void>}
 */
export async function runTopArtist(year) {
  const artistData = await fetchArtistOfTheYear(year);
  await renderTopArtist(artistData, year);
}


