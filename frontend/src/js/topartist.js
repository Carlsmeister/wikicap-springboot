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
const tpl = wikiTpl;

const recapHeader = document.querySelector("#recapHeader");
const yearBadge = document.querySelector("#yearBadge");
const submitBtn = document.querySelector("#submitBtn");

const ArtistSection = document.querySelector("#ArtistSection");
const ArtistGrid = document.querySelector("#ArtistGrid");
const ArtistTpl = document.querySelector("#ArtistCardTpl");
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

  function clearTopArtist() {
  if (ArtistGrid) ArtistGrid.innerHTML = "";
  if (ArtistSection) ArtistSection.classList.add("hidden");
  if (statsEl) statsEl.textContent = "";
}

  // Fetch top artist of the year.
  async function fetchArtistOfTheYear(year) {
    const res = await fetch(`${API_BASE}/api/v1/year/${year}/billboard/artist`);
    if (!res.ok) return {};
    const data = await res.json();
    return data ?? {};
  }

  function renderTopArtist(artistData) {
    clearTopArtist();


    if (!artistData || !ArtistTpl || !ArtistGrid || !ArtistSection) return;

    

    ArtistSection.classList.remove("hidden");

    artistData.artists.forEach((artistName, index) => {
      const node = ArtistTpl.content.firstElementChild.cloneNode(true);


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

      // const img = node.querySelector("img");
      const nameEl = node.querySelector(".name");
      
      nameEl.textContent = artistName;

    //   const imgUrl = topArtist.image || "";
    //   if (imgUrl) {
    //     img.src = imgUrl;
    //     img.alt = `Portrait of ${topArtist.name}`;
    //   } else {
    //     img.src = "https://www.billboard.com/lists/year-end-hot-100-number-one-songs/";
    //     img.alt = "Billboard Top Songs by Year";
    //   }
      ArtistGrid.appendChild(node);
      observer.observe(node);
    });
  }

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

    console.log(ArtistData);
    
    
    renderTopArtist(ArtistData);
  } catch (err) {
    console.error(err);
    setStatus("Kunde inte hämta Billboard-data.", "error");
    clearTopArtist();
  }

  try {
    const data = await fetchYear(year);

    const eventsByMonth = data?.events_by_month ?? {};
    const entries = Object.entries(eventsByMonth);
    const hasEvents = Object.keys(eventsByMonth).length > 0;

    if (!hasEvents) {
      setStatus(`Found no events for ${year}.`, "error");
      return;
    }

    // Update hero text
    heroText.textContent = `The year was ${year}`;
    //const entries = Object.entries(eventsByMonth);


    entries.forEach(([month, events], i) => {
      renderMonthCard({ month, year, events, index: i });
    });

    setStatus("");
  } catch (err) {
    console.error(err);
    setStatus("Could not fetch data. Is the backend running on 127.0.0.1:8000?", "error");
  } finally {
    submitBtn.disabled = false;
    submitBtn.classList.remove("opacity-70", "cursor-not-allowed");
  }



});

