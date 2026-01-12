import { renderHighlights, renderMovies, renderSeries } from "../components/MediaSection.js";
import { renderWikiSection } from "../components/WikiSection.js";
import { renderNobel, clearNobel } from "../components/NobelSection.js";

const API_BASE = "http://127.0.0.1:8000";

const form = document.querySelector("#yearForm");
const input = document.querySelector("#yearInput");
const statusEl = document.querySelector("#status");
const resultsEl = document.querySelector("#results");

const entertainmentSection = document.querySelector("#entertainmentSection");
const highlightsSection = document.querySelector("#highlightsSection");
const movieSection = document.querySelector("#movieSection");
const seriesSection = document.querySelector("#seriesSection");

const wikiTpl = document.querySelector("#wikiCardTpl");
const wikiSection = document.querySelector("#wikiSection");
const wikiStatsEl = document.querySelector("#wikiStats");
const heroText = document.querySelector("#heroText");



const recapHeader = document.querySelector("#recapHeader");
const yearBadge = document.querySelector("#yearBadge");
const submitBtn = document.querySelector("#submitBtn");

const nobelSection = document.querySelector("#nobelSection");
const nobelGrid = document.querySelector("#nobelGrid");
const nobelTpl = document.querySelector("#nobelSection template#nobelCardTpl");
const statsEl = document.querySelector("#stats");

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

function clearResults() {
  resultsEl.innerHTML = "";
  highlightsSection.innerHTML = "";
  movieSection.innerHTML = "";
  seriesSection.innerHTML = "";
  recapHeader.classList.add("hidden");
  yearBadge.textContent = "";
}


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
  setStatus("Fetching data...", "loading");
  submitBtn.disabled = true;
  submitBtn.classList.add("opacity-70", "cursor-not-allowed");

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

