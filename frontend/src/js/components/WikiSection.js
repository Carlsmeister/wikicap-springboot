/**
 * Renders a single month card with events from Wikipedia
 * @param {Object} params - The parameters object
 * @param {string} params.month - The month name
 * @param {number} params.year - The year
 * @param {Array<string>} params.events - Array of event descriptions
 * @param {number} params.index - Index for alternating layout
 * @param {HTMLElement} params.template - The template element to clone
 * @returns {HTMLElement} The rendered month card element
 */
export function renderMonthCard({ month, year, events, index, template }) {
  const node = template.content.firstElementChild.cloneNode(true);

  // Alternate left/right layout
  const isOdd = index % 2 === 0; // 0-based: 0=left, 1=right, ...
  node.classList.add(isOdd ? "justify-start" : "justify-end");

  const card = node.querySelector(".component-card");
  card.classList.add(isOdd ? "reveal-left" : "reveal-right");


  const title = node.querySelector(".monthTitle");
  const chip = node.querySelector(".monthChip");
  const list = node.querySelector(".monthList");

  title.textContent = `${month} ${year}`;
  title.classList.add(isOdd ? "text-amber-400" : "text-yellow-400");

  chip.textContent = `${events.length} event${events.length !== 1 ? 's' : ''}`;

  for (const event of events) {
    const li = document.createElement("li");
    li.textContent = `• ${event}`;
    li.className = "leading-relaxed";
    list.appendChild(li);
  }

  return node;
}

/**
 * Renders all month cards for a year
 * @param {Object} eventsByMonth - Object with month names as keys and event arrays as values
 * @param {number} year - The year
 * @param {HTMLElement} template - The template element to clone
 * @returns {DocumentFragment} A document fragment containing all month cards
 */
export function renderWikiSection(eventsByMonth, year, template) {
  const fragment = document.createDocumentFragment();

  const entries = Object.entries(eventsByMonth).filter(
    ([, arr]) => Array.isArray(arr) && arr.length > 0
  );

  entries.forEach(([month, events], index) => {
    const card = renderMonthCard({ month, year, events, index, template });
    fragment.appendChild(card);
  });

  return fragment;
}

