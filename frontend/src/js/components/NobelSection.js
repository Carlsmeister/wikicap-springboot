/**
 * @typedef {Object} NobelWinner
 * @property {string} [name] - Laureate name.
 * @property {string} [motivation] - Nobel motivation text.
 * @property {string} [image] - Optional portrait URL.
 */

/**
 * Map of category -> list of winners.
 * @typedef {Record<string, NobelWinner[]>} NobelPrizeMap
 */

/**
 * Nobel API response can be either a wrapped structure or already a map.
 * @typedef {NobelPrizeMap | { prizes: NobelPrizeMap }} NobelData
 */

/**
 * @typedef {Object} NobelUI
 * @property {HTMLElement | null} nobelSection - Wrapper section that is shown/hidden.
 * @property {HTMLElement | null} nobelGrid - Grid container where cards are appended.
 * @property {HTMLTemplateElement | null} nobelTpl - Template for a single Nobel card.
 * @property {HTMLElement | null} statsEl - Element where stats text is shown (e.g. "3 prizes").
 */

/**
 * Clear Nobel section
 *
 * - Clears the card grid
 * - Hides the section
 * - resets stats text
 *
 * Safe to call even if elements are null.
 *
 *  @param {Pick<NobelUI, "nobelGrid" | "nobelSection" | "statsEl">} params
 *  @returns {void}
 */

export function clearNobel({nobelGrid, nobelSection, statsEl}) {
    if (nobelGrid) nobelGrid.innerHTML = "";
    if (nobelSection) nobelSection.classList.add("hidden");
    if (statsEl) statsEl.textContent = "";
}

/**
 * Render Nobel cards into the nobel section
 *
 * Accepts nobel data in two shapes:
 * - "{ prizes: { [category]: NobelWinner[]] } }"
 * - "{ [category]: NobelWinner[] }"
 *
 * The section is shown only if there is atleast one winner.
 * Each rendered card is optionally registered with the provided IntersectionObserver.
 *
 * @param {NobelData} nobelData - Nobel prize data for a year.
 * @param {NobelUI} ui - Nobel DOM references.
 * @param {IntersectionObserver | null | undefined} observer - Observer used for reveal animations.
 * @returns {void}
 */


export function renderNobel(nobelData, {nobelSection, nobelGrid, nobelTpl, statsEl}, observer) {
    clearNobel({nobelGrid, nobelSection, statsEl});

    if (!nobelData || !nobelTpl || !nobelGrid || !nobelSection) return;

    // Handle nobelData structure
    const prizes = nobelData.prizes ?? nobelData;
    if (statsEl) {
    const prizeCount = Object.keys(prizes).length;
    statsEl.textContent = `${prizeCount} prize${prizeCount === 1 ? "" : "s"}`;
    }

    const winners = Object.entries(prizes).flatMap(([category, people]) =>
    (people ?? []).map(p => ({...p, category}))
);

    if (winners.length === 0) return;

    nobelSection.classList.remove("hidden");

    winners.forEach((winner,index) => {
        const node = nobelTpl.content.firstElementChild.cloneNode(true);

        const isLeft = index % 2 === 0;
        node.classList.add(
            "reveal",
            "opacity-0",
            "transition-all",
            "duration-700",
            "ease-out",
            "blur-sm",
            isLeft ? "-translate-x-10" : "translate-x-10"
        );

        // Populate card with winner data

        const img = node.querySelector("img");
        const nameEl = node.querySelector(".name");
        const categoryEl = node.querySelector(".nobel-category span") || node.querySelector(".category span");
        const motivationEl = node.querySelector(".motivation");

        if (nameEl) nameEl.textContent = winner.name ?? "Unknown";
        if (categoryEl) categoryEl.textContent = winner.category ?? "Unknown";
        if (motivationEl) motivationEl.textContent = winner.motivation ?? "No motivation provided.";

        const imgUrl = winner.image ?? "";
        if (img) {
            if (imgUrl) {
                img.src = imgUrl;
                img.alt = `Portrait of ${winner.name ?? "Unknown"}`;
            } else {
                img.src = "https://upload.wikimedia.org/wikipedia/en/e/ed/Nobel_Prize.png";
                img.alt = "Nobel Prize Medal";
            }
        }

        nobelGrid.appendChild(node);

        if (observer) observer.observe(node);

    });
}
