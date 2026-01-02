import requests
import re
from .wiki_cleaner import CLEANER



WIKI_API = "https://en.wikipedia.org/w/api.php"

HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

MONTHS = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
]


def normalize_toc(toc: dict) -> list[dict]:
    """
    Function to normalize the TOC structure into a flat list of items

    The Wikipedia API may return Data in nested dictionaries and lists.
    This functions flattens the structure into a single list of items, for
    easier processing.

    Args:
        toc (dict): The TOC data structure from the Wikipedia API.

    Returns:
        list[dict]: A flat list of TOC items.
    """
    items = []

    for value in toc.values():
        if isinstance(value, list):
            items.extend(value)
        elif isinstance(value, dict):
            items.append(value)

    return items

def fetch_year_toc(year: int) -> str:
    """Fetch the TOC for a given year from Wikipedia.
    This function uses the wikipedia API to fetch the TOC data for a specified year.

    Args:
        year (int): The year for which to fetch the TOC.

    Returns:
        list [dict]: The TOC data structure.
        """
    params = {
        "action": "parse",
        "page": str(year),
        "prop": "tocdata",
        "format": "json",
        "formatversion": "2",
    }
    request_response = requests.get(WIKI_API, params=params, headers=HEADERS, timeout=20)
    request_response.raise_for_status()

    return request_response.json().get("parse", {}).get("tocdata", [])


def get_month_sections(year: int) -> dict[str, str]:
    """
    Get the month sections for a given year from Wikipedia.
    """
    toc = fetch_year_toc(year)
    items = normalize_toc(toc)

    months = {}
    for item in items:
        title = item.get("line", "")
        index = item.get("index", "")

        if title in MONTHS:
            months[title] = index


    return months

def get_month_wikitext(year: int, month_index: str) -> str:
    params = {
        "action": "parse",
        "page": str(year),
        "prop": "wikitext",
        "section": month_index,
        "format": "json",
        "formatversion": "2",
    }

    request_response = requests.get(WIKI_API, params=params, headers=HEADERS, timeout=20)
    request_response.raise_for_status()

    return request_response.json().get("parse", {}).get("wikitext", "")

def extract_month_events(wikitext: str, limit: int = 6) -> list[str]:
    events = []

    for line in wikitext.splitlines():
        if not line.startswith("*"):
            continue

        clean = CLEANER.clean_event_line(line, keep_date_prefix = False)
        if clean:
            events.append(clean)

        if len(events) >= limit:
            break

    return events

def fetch_year_summary(year: int) -> str:
    months = get_month_sections(year)
    results = {}

    for month, index in months.items():
        wikitext = get_month_wikitext(year, index)
        events = extract_month_events(wikitext)

        if events:
            results[month] = events

    return results

if __name__ == "__main__":
    toc = fetch_year_toc(1999)
    items = normalize_toc(toc)

    print("Flattened TOC items:", len(items))
    for item in items:
        print(item["index"], item["line"])

