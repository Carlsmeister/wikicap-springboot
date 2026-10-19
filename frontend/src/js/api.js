export const API_BASE = "http://127.0.0.1:8080";

export async function fetchSection(year, section) {
  const response = await fetch(`${API_BASE}/api/v1/years/${year}/${section}`);
  if (!response.ok) throw new Error(`API error: ${response.status}`);
  return response.json();
}

// Music scrapes Wikipedia and is slow; two components read it, so share one request per year.
const musicByYear = new Map();

export function getMusic(year) {
  if (!musicByYear.has(year)) {
    const request = fetchSection(year, "music");
    request.catch(() => musicByYear.delete(year));
    musicByYear.set(year, request);
  }
  return musicByYear.get(year);
}
