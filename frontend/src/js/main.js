import { renderHighlights, renderMovies, renderSeries } from "./components/MediaSection.js";
import { runTopArtist } from "./components/TopArtist.js";
import { renderWikiSection } from "./components/WikiSection.js";
import { renderNobel, clearNobel } from "./components/NobelSection.js";
import { fetchSection, isValidYear } from "./api.js";
import { toEventsByMonth, toNobelPrizes, toHighlights, toTopMovies, toTopSeries } from "./mappers.js";

const form = document.querySelector("#yearForm"); // Form element for year input
const input = document.querySelector("#yearInput"); // Input field for year input
const statusEl = document.querySelector("#status"); // Status display element
const resultsEl = document.querySelector("#results"); // Main results area

const entertainmentSection = document.querySelector("#entertainmentSection"); // Entertainment section container
const highlightsSection = document.querySelector("#highlightsSection"); // Movie highlights section container
const movieSection = document.querySelector("#movieSection"); // Movie section container
const seriesSection = document.querySelector("#seriesSection"); // Series section container

const wikiTpl = document.querySelector("#wikiCardTpl"); // Template for rendering wiki cards
const heroText = document.querySelector("#heroText"); // Hero text element

const recapHeader = document.querySelector("#recapHeader"); // Recap header element
const yearBadge = document.querySelector("#yearBadge"); // Year badge element
const submitBtn = document.querySelector("#submitBtn"); // Submit button

const nobelSection = document.querySelector("#nobelSection"); // Nobel prize section container
const nobelGrid = document.querySelector("#nobelGrid"); // Nobel prize grid container
const nobelTpl = document.querySelector("#nobelSection template#nobelCardTpl"); // Template for rendering nobel prize cards
const statsEl = document.querySelector("#stats"); // Stats element

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
  entertainmentSection.classList.add("hidden");
  heroText.textContent = "";
  recapHeader.classList.add("hidden");
  yearBadge.textContent = "";
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const raw = input.value.trim();
  if (!isValidYear(raw)) {
    setStatus("Please enter a valid year.", "error");
    return;
  }
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
    const [events, nobel, entertainment] = await Promise.allSettled([
      fetchSection(year, "events"),
      fetchSection(year, "nobel"),
      fetchSection(year, "entertainment"),
    ]);

    if (events.status === "rejected" && nobel.status === "rejected" && entertainment.status === "rejected") {
      console.error(events.reason);
      setStatus("Could not fetch data. Is the backend running on 127.0.0.1:8080?", "error");
      return;
    }

    // Each section maps and renders on its own, so one unexpected shape does not blank the others.
    if (nobel.status === "fulfilled") {
      try {
        renderNobel(
          toNobelPrizes(nobel.value),
          { nobelSection, nobelGrid, nobelTpl, statsEl},
          observer
        );
      } catch (err) {
        console.error("Nobel failed:", err);
      }
    }

    let hasEvents = false;
    if (events.status === "fulfilled") {
      try {
        const eventsByMonth = toEventsByMonth(events.value);

        if (Object.keys(eventsByMonth).length > 0) {
          heroText.textContent = `The year was ${year}`;

          const wikiFragment = renderWikiSection(eventsByMonth, year, wikiTpl);
          resultsEl.appendChild(wikiFragment);

          resultsEl
            .querySelectorAll(".component-card.reveal, .component-card.reveal-left, .component-card.reveal-right")
            .forEach(el => observer.observe(el));
          hasEvents = true;
        }
      } catch (err) {
        console.error("Events failed:", err);
      }
    }

    if (entertainment.status === "fulfilled") {
      try {
        const highlights = toHighlights(entertainment.value);

        if (highlights) {

          entertainmentSection.classList.remove("hidden");

          highlightsSection.innerHTML = renderHighlights(highlights, year);
          movieSection.innerHTML = renderMovies(listSorter(toTopMovies(entertainment.value), "rating"));
          seriesSection.innerHTML = renderSeries(listSorter(toTopSeries(entertainment.value), "rating"), year);

          setTimeout(() => {
            const movieCards = movieSection.querySelectorAll('.movie-card.reveal');
            const seriesCards = seriesSection.querySelectorAll('.series-card.reveal');

            movieCards.forEach(card => observer.observe(card));
            seriesCards.forEach(card => observer.observe(card));
          }, 0);

        }
      } catch (err) {
        console.error("Entertainment failed:", err);
      }
    }

    if (hasEvents) setStatus("");
    else setStatus(`Found no events for ${year}.`, "error");
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

