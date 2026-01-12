import { renderHighlights, renderMovies, renderSeries } from "../components/MediaSection.js";
import { renderWikiSection } from "../components/WikiSection.js";

/** Base URL for the backend API */
const API_BASE = "http://127.0.0.1:8000";

/** The form used to submit a year for lookup 
 * @type {HTMLFormElement}
 */
const form = document.querySelector("#yearForm");

/** Input field where the user enters a year
 * @type {HTMLInputElement}
 */
const input = document.querySelector("#yearInput");

/** Element used to display loading or status messages 
 * @type {HTMLElement}
*/
const statusEl = document.querySelector("#status");

/** Container for all generated results.
 * @type {HTMLElement}
 */
const resultsEl = document.querySelector("#results");

/** Section showing the year's highlights.
 * @type {HTMLElement}
 */
const highlightsSection = document.querySelector("#highlightsSection");

/** Section containing movie results.
 * @type {HTMLElement}
 */
const movieSection = document.querySelector("#movieSection");

/** Section containing series results.
 * @type {HTMLElement}
 */
const seriesSection = document.querySelector("#seriesSection");

/** Template element for Wikipedia-style cards.
 * @type {HTMLTemplateElement}
 */
const wikiTpl = document.querySelector("#wikiCardTpl");

/** Main hero text element at the top of the page.
 * @type {HTMLElement}
 */
const heroText = document.querySelector("#heroText");

/** Alias for the wiki card template.
 * @type {HTMLTemplateElement}
 */
const tpl = wikiTpl;

/** Header element for the recap section.
 * @type {HTMLElement}
 */
const recapHeader = document.querySelector("#recapHeader");

/** Badge displaying tyhe selected year. 
 * @type {HTMLElement}
 */
const yearBadge = document.querySelector("#yearBadge");

/** Submit button for triggering the year lookup.
 * @type {HTMLButtonElement}
 */
const submitBtn = document.querySelector("#submitBtn");

/** Section containing artist-related content.
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
const statsEl = document.querySelector("#stats");

/** Cleanup function for the auto-reveal animation of artist cards.
 * If this variable holds a function (not null), calling it will stop the animation.
 */
let cleanupArtistAutoReveal = null;

/** Sets up the auto‑reveal animation for artist cards.
 * Shows a few cards immediately, then reveals the rest one by one.
 * Replaces any previous auto‑reveal setup by running its cleanup function. */
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

/** IntersectionObserver that reveals elements,
 *  when they become visible on the screen. 
 *  
 * When at least 15% of an element is in view,
 * the observer removes the "hidden" classes and adds a "visible" class.
 * After an element has been revealed, it is no longer observed.
 *
 * This makes sure each element only animates once when the user scrolls. */
const observer = new IntersectionObserver(entries => {
  entries.forEach(entry => {
    if (!entry.isIntersecting) return;

    entry.target.classList.remove(
      "opacity-0",
      "blur-sm",
      "-translate-x-10",
      "translate-x-10"
    );

    entry.target.classList.add("is-visible");


    observer.unobserve(entry.target);
  });
},
{  threshold: 0.15}

    );

/** Clears the current "Top Artist" view.
 * Removes any running auto‑reveal animation, empties the artist grid,
 * hides the artist section, and resets the stats text.
 */  
  function clearTopArtist() {
  //if (cleanupArtistPin) cleanupArtistPin();  
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

  let topTracksByArtist = new Map();
  try {
    const topSongsData = await fetchBillboardTopSong(year, 5);
    topTracksByArtist = new Map(
      (topSongsData.artist ?? []).map(a => [a.artist, a.toptracks ?? []])
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

    
    const toptracks = topTracksByArtist.get(artistName) ?? [];
    renderTopSongs(toptracks, node);

    ArtistGrid.appendChild(node);
  }

  setupArtistAutoReveal({
    initialVisible: 3,
    delayMs: 1000,
    durationMs: 1200
  });

  requestAnimationFrame(() => {
    const position = ArtistSection.getBoundingClientRect().top + window.scrollY;
    const offset = 220;
    window.scrollTo({ top: position - offset, behavior: "smooth" });
  });
}

  /** Fetches the top songs for a given year from the Billboard API.
   *
   * Sends a request to the backend and returns the parsed JSON response.
   * If the request fails, the function throws an error instead of returning fallback data.
  */  
  async function fetchBillboardTopSong(year, limit=5) {
    const res = await fetch(
      `${API_BASE}/api/v1/year/${year}/billboard/artist/top-songs?limit=${limit}`);
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

/**
 * Updates the status element with a message and a visual style.
 *
 * Clears any previous status content and then displays a new message
 * based on the given status type. Supports three kinds of messages:
 * - "loading": shows a spinner and a loading text
 * - "error": shows the text in a red tone
 * - "success": shows the text in a green tone
 * - "info" (default): shows the text in a neutral color
 *
 * @param {string} text - The message to display.
 * @param {"info"|"loading"|"error"|"success"} [kind="info"] - The type of status message.
 */
function setStatus(text, kind = "info") {
  statusEl.className = "text-center";
  statusEl.innerHTML = "";

  if (kind === "loading") {
    statusEl.innerHTML = `
      <div class="status-row">
        <div class="loader" aria-label="Loading"></div>
        <span class="status-text">${text}</span>
      </div>
    `;
    return;
  }

  const span = document.createElement("span");
  span.className = "text-sm";

  if (kind === "error") span.classList.add("text-red-300");
  else if (kind === "success") span.classList.add("text-emerald-200");
  else span.classList.add("text-slate-300");

  span.textContent = text;
  statusEl.appendChild(span);
}

/**
 * Clears all result sections from the page.
 *
 * Empties the main results area, highlights, movie and series sections,
 * hides the recap header, and resets the year badge. Used before rendering
 * new data to ensure the UI starts from a clean state.
 */
function clearResults() {
  resultsEl.innerHTML = "";
  highlightsSection.innerHTML = "";
  movieSection.innerHTML = "";
  seriesSection.innerHTML = "";
  recapHeader.classList.add("hidden");
  yearBadge.textContent = "";
}

/**
 * Renders a single month card and adds it to the results section.
 *
 * The card is cloned from a template, styled based on its index
 * (alternating left/right reveal animations), and filled with the
 * month title and its list of events. Each event becomes a list item.
 *
 * A small transition delay is applied based on the card’s position
 * to create a staggered reveal effect. The card is then observed by
 * the IntersectionObserver so it animates when scrolled into view.
 *
 * @param {Object} params - Data used to build the month card.
 * @param {string} params.month - The month name (e.g., "January").
 * @param {number} params.year - The year the month belongs to.
 * @param {string[]} params.events - List of event descriptions.
 * @param {number} params.index - Position of the card in the sequence.
 */
function renderMonthCard({ month, year, events, index }) {
  const node = tpl.content.firstElementChild.cloneNode(true);

  const isOdd = index % 2 === 0;
  node.classList.add(isOdd ? "justify-start" : "justify-end");

  const card = node.querySelector(".component-card");

  card.classList.add(isOdd ? "reveal-left" : "reveal-right");

  card.style.transitionDelay = `${index * 80}ms`;
  card.dataset.reveal = isOdd ? "left" : "right";

  const title = node.querySelector(".monthTitle");
  const chip = node.querySelector(".monthChip");
  const list = node.querySelector(".monthList");

  title.textContent = `${month} ${year}`;
  title.classList.add(isOdd ? "text-cyan-200" : "text-purple-200");

  for (const e of events) {
    const li = document.createElement("li");
    li.textContent = `• ${e}`;
    li.className = "leading-relaxed";
    list.appendChild(li);
  }

  resultsEl.appendChild(node);
  observer.observe(card);
}

/**
 * Fetches all data for a given year from the API.
 *
 * Builds the request URL, sends the fetch call, and returns the parsed
 * JSON response. If the server responds with a non‑OK status, the function
 * throws an error so the caller can handle it.
 *
 * @param {number|string} year - The year to request data for.
 * @returns {Promise<Object>} The parsed year data from the API.
 * @throws {Error} If the API responds with a non‑OK status.
 */
async function fetchYear(year) {
  const url = `${API_BASE}/api/v1/year/${encodeURIComponent(year)}`;

  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  return await response.json();
}


form.addEventListener("submit", async (e) => {
  e.preventDefault();


  const raw = input.value.trim();
  const year = Number(raw);


  clearTopArtist();
  clearResults();
  setStatus("Fetching data...", "loading");
  submitBtn.disabled = true;
  submitBtn.classList.add("opacity-70", "cursor-not-allowed");

  try {
    const ArtistData = await fetchArtistOfTheYear(year);
    await renderTopArtist(ArtistData,year);

    console.log(ArtistData);
    
     // Update hero text
      heroText.textContent = `Top artists of the year ${year}`

    setStatus("");
  } catch (err) {
    console.error(err);
    setStatus("Could not fetch data. Is the backend running on 127.0.0.1:8000?", "error");
    clearTopArtist();
  } finally {
    submitBtn.disabled = false;
    submitBtn.classList.remove("opacity-70", "cursor-not-allowed");
  }
});
