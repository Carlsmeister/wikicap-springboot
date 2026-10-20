export const API_BASE = "http://127.0.0.1:8080";

export function isValidYear(raw) {
  const year = Number(raw);
  return /^\d+$/.test(raw) && year >= 1 && year <= new Date().getFullYear();
}

export async function fetchSection(year, section) {
  const response = await fetch(`${API_BASE}/api/v1/years/${year}/${section}`);
  if (!response.ok) throw new Error(`API error: ${response.status}`);
  return response.json();
}

// Music is slow and both submit listeners read it, so concurrent callers share one request.
// Only in-flight: Java caches /music per year, and a kept degraded 200 would stick until reload.
const musicInFlight = new Map();

export function getMusic(year) {
  const key = Number(year);
  if (!musicInFlight.has(key)) {
    const request = fetchSection(key, "music");
    const forget = () => musicInFlight.delete(key);
    request.then(forget, forget);
    musicInFlight.set(key, request);
  }
  return musicInFlight.get(key);
}
