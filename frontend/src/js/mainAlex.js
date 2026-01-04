import { renderHighlights, renderMovies, renderSeries } from "../components/MediaSection.js";
import { renderWikiSection } from "../components/WikiSection.js";

const API_BASE = "http://127.0.0.1:8000";

const form = document.querySelector("#yearForm");
const input = document.querySelector("#yearInput");
const statusEl = document.querySelector("#status");
const resultsEl = document.querySelector("#results");
const highlightsSection = document.querySelector("#highlightsSection");
const movieSection = document.querySelector("#movieSection");
const seriesSection = document.querySelector("#seriesSection");
const wikiTpl = document.querySelector("#wikiCardTpl");
const heroText = document.querySelector("#heroText");

const recapHeader = document.querySelector("#recapHeader");
const yearBadge = document.querySelector("#yearBadge");
const submitBtn = document.querySelector("#submitBtn");

const MIN_YEAR = 1900;
const MAX_YEAR = new Date().getFullYear();

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

if (!form || !input || !statusEl || !resultsEl || !wikiTpl || !recapHeader || !yearBadge || !submitBtn) {
  console.error("Missing DOM element(s):", {
    form, input, statusEl, resultsEl, tpl: wikiTpl, recapHeader, yearBadge, submitBtn
  });
  throw new Error("HTML is missing one or more IDs that the script requires.");
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const raw = input.value.trim();
  const year = Number(raw);

  if (!raw || Number.isNaN(year) || year < MIN_YEAR || year > MAX_YEAR) {
    setStatus(`Skriv ett giltigt år (${MIN_YEAR}-${MAX_YEAR}).`, "error");
    return;
  }

  clearResults();
  setStatus("Fetching data...", "loading");
  submitBtn.disabled = true;
  submitBtn.classList.add("opacity-70", "cursor-not-allowed");

  try {
    const data = await fetchYear(year);

    // Check if we have events data
    const eventsByMonth = data?.events_by_month ?? {};
    const hasEvents = Object.keys(eventsByMonth).length > 0;

    if (!hasEvents) {
      setStatus(`Found no events for ${year}.`, "error");
      return;
    }

    // Update hero text
    heroText.textContent = `The year was ${year}`;

    // Show header badge
    recapHeader.classList.remove("hidden");
    recapHeader.classList.add("flex");
    yearBadge.textContent = String(year);

    // Render wiki events using the component
    const wikiFragment = renderWikiSection(eventsByMonth, year, wikiTpl);
    resultsEl.appendChild(wikiFragment);

    // Render movie highlights
    if (data.movie_highlights) {
      highlightsSection.innerHTML = renderHighlights(data.movie_highlights);
    }

    // Render top movies
    if (data.movies?.topMovies) {
      const sortedMovies = [...data.movies.topMovies].sort((a, b) => b.rating - a.rating);
      movieSection.innerHTML = renderMovies(sortedMovies);
    }

    // Render top series
    if (data.series?.topSeries) {
      const sortedSeries = [...data.series.topSeries].sort((a, b) => b.rating - a.rating);
      seriesSection.innerHTML = renderSeries(sortedSeries);
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

