import { renderHighlights, renderMovies, renderSeries } from "../components/MediaSection.js";
import { runTopArtist } from "../components/TopArtist.js";
import { renderWikiSection } from "../components/WikiSection.js";
import { renderNobel, clearNobel } from "./components/NobelSection.js";

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
const entertainmentSection = document.querySelector("#entertainmentSection");

/** Section containing Nobel Prize-related content.
 * @type {HTMLElement}
 */
const nobelSection = document.querySelector("#nobelSection");

/** Grid container where Nobel winner cards are rendered.
 * @type {HTMLElement}
 */
const nobelGrid = document.querySelector("#nobelGrid");

/** Template used for cloning individual Nobel winner cards.
 * @type {HTMLTemplateElement}
 */
const nobelTpl = document.querySelector("#nobelSection template#nobelCardTpl");

/** Element displaying statistics or status messages for the Nobel section.
 * @type {HTMLElement}
 */
const statsEl = document.querySelector("#stats");

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
  
  /**
 * Clears all Nobel‑related content from the page.
 *
 * - Empties the grid that displays Nobel results.
 * - Hides the Nobel section so it’s not visible to the user.
 * - Resets the statistics element by removing any previous text.
 */
  function clearNobel() {
  if (nobelGrid) nobelGrid.innerHTML = "";
  if (nobelSection) nobelSection.classList.add("hidden");
  if (statsEl) statsEl.textContent = "";
}
/**
 * Renders a list of Nobel Prize winners into the UI.
 *
 * - Clears any previously displayed Nobel content.
 * - Transforms the incoming `nobelData` object into a flat list of winners.
 * - Reveals the Nobel section and inserts one card per winner using a template.
 * - Applies left/right reveal animations based on each card’s index.
 * - Fills each card with the winner’s name, category, motivation, and image
 *   (falling back to a default Nobel medal image when needed).
 * - Observes each card with an IntersectionObserver to trigger animations.
 *
 * The function exits early if required DOM elements or data are missing.
 */
  function renderNobel(nobelData) {
    clearNobel();


    if (!nobelData || !nobelTpl || !nobelGrid || !nobelSection) return;

    const winners = Object.entries(nobelData).flatMap(
      ([category, people]) =>
        people.map(p => ({...p, category}))
    );

    if (winners.length === 0) return;

    nobelSection.classList.remove("hidden");

    winners.forEach((winner, index) => {
      const node = nobelTpl.content.firstElementChild.cloneNode(true);


      const isLeft = index % 2 === 0;
      node.classList.add("reveal",
        "opacity-0",
        "transition-all",
        "duration-700",
        "ease-out",
        "blur-sm",
        isLeft ? "-translate-x-10" : "translate-x-10"
      );

      node.dataset.reveal = isLeft ? "left" : "right";

      const img = node.querySelector("img");
      const nameEl = node.querySelector(".name");
      const categoryEl = node.querySelector(".category");
      const motivationEl = node.querySelector(".motivation");

      nameEl.textContent = winner.name ?? "Unknown";
      categoryEl.textContent = winner.category ?? "Unknown Category";
      motivationEl.textContent = winner.motivation ?? "";

      const imgUrl = winner.image || "";
      if (imgUrl) {
        img.src = imgUrl;
        img.alt = `Portrait of ${winner.name}`;
      } else {
        img.src = "https://upload.wikimedia.org/wikipedia/en/e/ed/Nobel_Prize.png";
        img.alt = "Nobel Prize Medal";
      }
      nobelGrid.appendChild(node);
      observer.observe(node);
    });
  }


/**
 * Updates the status area with a message or a loading indicator.
 *
 * - Resets the status element before displaying new content.
 * - When `kind` is `"loading"`, shows an animated infinity‑loop loader
 *   together with the provided text.
 * - For `"error"`, `"success"`, or `"info"`, creates a styled text node
 *   and appends it to the status element.
 *
 * The function ensures consistent formatting and styling for all status updates.
 */  
function setStatus(text, kind = "info") {
  statusEl.className = "text-center";
  statusEl.innerHTML = "";

  if (kind === "loading") {
    statusEl.innerHTML = `
      <div class="status-flex">
        <div class="loader" aria-label="Loading">
          <svg class="infinite-svg" viewBox="0 0 200 100" aria-hidden="true">
          <defs>
            <linearGradient id="wcGrad" x1="0" y1="0" x2="200" y2="0" gradientUnits="userSpaceOnUse">
              <stop offset="0%" stop-color="var(--apricot)"/>
              <stop offset="50%" stop-color="var(--vermilion)"/>
              <stop offset="100%" stop-color="var(--primary)"/>
            </linearGradient>
          </defs>
          <path class="infinity-track" d="M20,50 C40,20 80,20 100,50 C120,80 160,80 180,50 C160,20 120,20 100,50 C80,80 40,80 20,50" />

            <path class="infinity-path" stroke="url(#wcGrad)" d="M20,50 C40,20 80,20 100,50 C120,80 160,80 180,50 C160,20 120,20 100,50 C80,80 40,80 20,50" />
          </svg>
        </div>
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
  const list = node.querySelector(".monthList");

  title.textContent = `${month} ${year}`;
  title.classList.add(isOdd ? "text-amber-200" : "text-amber-400");



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

  clearNobel({ nobelGrid, nobelSection, statsEl});
  clearResults();
  setStatus("", "loading");
  submitBtn.disabled = true;
  submitBtn.classList.add("opacity-70", "cursor-not-allowed");

  try {
    await runTopArtist(year);
  } catch (err) {
    console.error("TopArtist failed:", err);
  }

  try {
    const data = await fetchYear(year);

    if (data?.nobel_prizes) {
      renderNobel(
        data.nobel_prizes,
        { nobelSection, nobelGrid, nobelTpl, statsEl },
        observer
      );
    }

    const eventsByMonth = data?.events_by_month ?? {};
    const entries = Object.entries(eventsByMonth);
    const hasEvents = Object.keys(eventsByMonth).length > 0;

    if (!hasEvents) {
      setStatus(`Found no events for ${year}.`, "error");
      return;
    }

    heroText.textContent = `The year was ${year}`;

    const wikiFragment = renderWikiSection(eventsByMonth, year, wikiTpl);
    resultsEl.appendChild(wikiFragment);

    resultsEl
      .querySelectorAll(".component-card.reveal, .component-card.reveal-left, .component-card.reveal-right")
      .forEach(el => observer.observe(el));

    if (data.movie_highlights && data.movies?.top_movies && data.series?.top_series) {

      entertainmentSection.classList.remove("hidden");

      highlightsSection.innerHTML = renderHighlights(data.movie_highlights, year);
      movieSection.innerHTML = renderMovies(listSorter(data.movies.top_movies, "rating"));
      seriesSection.innerHTML = renderSeries(listSorter(data.series.top_series, "rating"));

      setTimeout(() => {
        const movieCards = movieSection.querySelectorAll('.movie-card.reveal');
        const seriesCards = seriesSection.querySelectorAll('.series-card.reveal');

        movieCards.forEach(card => observer.observe(card));
        seriesCards.forEach(card => observer.observe(card));
      }, 0);

    }

    setStatus("");
  } catch (err) {
    console.error(err);
    setStatus("Could not fetch data. Is the backend running on 127.0.0.1:8000?", "error");
  } finally {
    submitBtn.disabled = false;
    submitBtn.classList.remove("opacity-70", "cursor-not-allowed");
  }
});

/**
 * Generic insertion sort function for sorting arrays by a numeric property in descending order
 * @param {Array} arr - Array to sort
 * @param {string} property - Property name to sort by, e.g rating, votes etc.
 * @returns {Array} - Sorted array (new array, doesn't mutate original)
 */
function listSorter(arr, property) {
  const sorted = [...arr];

  for (let i = 1; i < sorted.length; i++) {
    for (let j = i; j > 0 && sorted[j][property] > sorted[j - 1][property]; j--) {
      [sorted[j], sorted[j - 1]] = [sorted[j - 1], sorted[j]];
    }
  }
  return sorted;
}

