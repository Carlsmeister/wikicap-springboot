import {renderMovies, renderSeries} from "../components/MediaSection.js";

const API_BASE = "http://127.0.0.1:8000";

const form = document.querySelector("#yearForm");
const input = document.querySelector("#yearInput");
const statusEl = document.querySelector("#status");
const resultsEl = document.querySelector("#results");
const movieSection = document.querySelector("#movieSection");
const seriesSection = document.querySelector("#seriesSection");
const wikiTpl = document.querySelector("#wikiCardTpl");
const heroText = document.querySelector("#heroText");

const recapHeader = document.querySelector("#recapHeader");
const yearBadge = document.querySelector("#yearBadge");
const submitBtn = document.querySelector("#submitBtn");

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
  movieSection.innerHTML = "";
  seriesSection.innerHTML = "";
  recapHeader.classList.add("hidden");
  yearBadge.textContent = "";
}

function renderMonthCard({ month, year, events, index }) {
  const node = wikiTpl.content.firstElementChild.cloneNode(true);

  // Alternate left/right
  const isOdd = index % 2 === 0; // 0-based: 0=left, 1=right, ...
  node.classList.add(isOdd ? "justify-start" : "justify-end");

  const card = node.querySelector(".component-card");
  card.classList.add(isOdd ? "slide-in-blurred-left-normal" : "slide-in-blurred-right-normal");

  const title = node.querySelector(".monthTitle");
  const chip = node.querySelector(".monthChip");
  const list = node.querySelector(".monthList");

  title.textContent = `${month} ${year}`;
  title.classList.add(isOdd ? "text-cyan-200" : "text-purple-200");

  chip.textContent = `${events.length} events`;

  for (const e of events) {
    const li = document.createElement("li");
    li.textContent = `• ${e}`;
    li.className = "leading-relaxed";
    list.appendChild(li);
  }

  resultsEl.appendChild(node);
}

async function fetchYear(year) {
  const wikiUrl = `${API_BASE}/api/year/${encodeURIComponent(year)}`;
  const mediaUrl = `${API_BASE}/api/v1/year/${encodeURIComponent(year)}`;

  const [wikiRes, mediaRes] = await Promise.all([
    fetch(wikiUrl),
    fetch(mediaUrl)
  ]);

  if (!wikiRes.ok) throw new Error(`Wiki API error: ${wikiRes.status}`);
  if (!mediaRes.ok) throw new Error(`Media API error: ${mediaRes.status}`);

  const wikiData = await wikiRes.json();
  const mediaData = await mediaRes.json();

  return { ...wikiData, ...mediaData };
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

  if (!raw || Number.isNaN(year) || year < 1 || year > 9999) {
    setStatus("Please enter a valid year (e.g. 1997).", "error");
    return;
  }

  clearResults();
  setStatus("Fetching data...", "loading");
  submitBtn.disabled = true;
  submitBtn.classList.add("opacity-70", "cursor-not-allowed");

  try {
    const data = await fetchYear(year);

    const eventsByMonth = data?.events_by_month ?? {};
    const entries = Object.entries(eventsByMonth).filter(([, arr]) => Array.isArray(arr) && arr.length);

    if (entries.length === 0) {
      setStatus(`Found no events for ${year}.`, "error");
      return;
    }

    heroText.textContent = String(`The year was ${year}`);

    // Show header badge
    recapHeader.classList.remove("hidden");
    recapHeader.classList.add("flex");
    yearBadge.textContent = String(year);

    // Render
    entries.forEach(([month, events], i) => {
      renderMonthCard({ month, year, events, index: i });
    });

    if (data.movies?.topMovies) {
      const sortedMovies = [...data.movies.topMovies].sort((a, b) => b.rating - a.rating);
      movieSection.innerHTML = renderMovies(sortedMovies);
    }

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

