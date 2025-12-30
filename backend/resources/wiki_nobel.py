import requests
from backend.resources.wiki_nobel_extractor import extract_nobel

WIKI_API = "https://en.wikipedia.org/w/api.php"

HEADERS = {
    "User-Agent": "WikiCap/1.0 (school project; contact: alex@example.com)",
    "Accept": "application/json",
    "Accept-Language": "en",
}

def get_nobel_prizes(year: int) -> dict:
    title = f"{year}_Nobel_Prizes"

    params = {
        "action": "parse",
        "page": title,
        "prop": "text",
        "format": "json",
        "formatversion": "2",
    }

    r = requests.get(WIKI_API, params=params, headers=HEADERS, timeout=20)
    r.raise_for_status()
    data = r.json()

    html = (data.get("parse", {}).get("text") or "")

    prizes = extract_nobel(html)
    return {
        "year": year,
        "nobel_prizes": prizes,
    }