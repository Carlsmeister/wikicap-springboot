const API_BASE = "http://127.0.0.1:8000";

const form = document.querySelector("#yearForm");
const input = document.querySelector("#yearInput");
const statusEl = document.querySelector("#status");
const resultsEl = document.querySelector("#results");
const tpl = document.querySelector("#monthCardTpl");
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
  recapHeader.classList.add("hidden");
  yearBadge.textContent = "";
}

function renderMonthCard({ month, year, events, index }) {
  const node = tpl.content.firstElementChild.cloneNode(true);

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
  const url = `${API_BASE}/api/year/${encodeURIComponent(year)}`;

  const res = await fetch(url, { method: "GET" });
  if (!res.ok) {
    throw new Error(`API error: ${res.status}`);
  }
  return await res.json();
}
if (!form || !input || !statusEl || !resultsEl || !tpl || !recapHeader || !yearBadge || !submitBtn) {
  console.error("Missing DOM element(s):", {
    form, input, statusEl, resultsEl, tpl, recapHeader, yearBadge, submitBtn
  });
  throw new Error("HTML saknar ett eller flera id:n som scriptet behöver.");
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const raw = input.value.trim();
  const year = Number(raw);

  if (!raw || Number.isNaN(year) || year < 1 || year > 9999) {
    setStatus("Skriv ett giltigt år (t.ex. 1997).", "error");
    return;
  }

  clearResults();
  setStatus("Hämtar data…", "loading");
  submitBtn.disabled = true;
  submitBtn.classList.add("opacity-70", "cursor-not-allowed");

  try {
    const data = await fetchYear(year);

    const eventsByMonth = data?.events_by_month ?? {};
    const entries = Object.entries(eventsByMonth).filter(([, arr]) => Array.isArray(arr) && arr.length);

    if (entries.length === 0) {
      setStatus(`Hittade inga events för ${year}.`, "error");
      return;
    }

    heroText.textContent = String(`Året var ${year}`);

    // Show header badge
    recapHeader.classList.remove("hidden");
    recapHeader.classList.add("flex");
    yearBadge.textContent = String(year);

    // Render
    entries.forEach(([month, events], i) => {
      renderMonthCard({ month, year, events, index: i });
    });

    setStatus("");
  } catch (err) {
    console.error(err);
    setStatus("Kunde inte hämta data. Är backend igång på 127.0.0.1:8000?", "error");
  } finally {
    submitBtn.disabled = false;
    submitBtn.classList.remove("opacity-70", "cursor-not-allowed");
  }
});
