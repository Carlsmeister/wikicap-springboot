import requests
import re
from .wiki_cleaner import CLEANER
from bs4 import BeautifulSoup


WIKI_API = "https://en.wikipedia.org/w/api.php"

HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

MONTHS = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
]


def normalize_toc(toc: dict) -> list[dict]:
    items = []

    for value in toc.values():
        if isinstance(value, list):
            items.extend(value)
        elif isinstance(value, dict):
            items.append(value)

    return items

def fetch_year_toc(year: int) -> str:

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



# def fetch_year_events(year: int) -> dict:
#     """
#     This function fetches and processes the year summary from Wikipedia to extract events by month.
#     args:
#         year (int): The year for which to fetch events.

#     returns:
#             dict: A dictionary with months as keys and lists of events as values.
#     """

#     html = fetch_year_html(year)
#     soup = BeautifulSoup(html, "html.parser")

#     events_h2 = soup.find("h2", id = "Events")
#     if not events_h2:
#         events_span = soup.find("span", id = "Events")
#         events_h2 = events_span.find_parent("h2") if events_span else None

#     if not events_h2:
#         return {}

#     events_by_month = {}
#     current_month = None

#     node = events_h2.find_next()
#     while node:
#         if node.name == "h2":
#             break

#         if node.name == "h3":
#             current_month = node.get_text(" ", strip = True)
#             events_by_month[current_month] = []

#         elif node.name == "li" and current_month:
#             if len(events_by_month[current_month]) >= 6:
#                 node = node.find_next()
#                 continue

#             clean = CLEANER.clean_event_line(
#                 "* " + node.get_text(" ", strip= True),
#                 keep_date_prefix = False
#             )
#             if clean:
#                 events_by_month[current_month].append(clean)

#         node = node.find_next()
#     return events_by_month