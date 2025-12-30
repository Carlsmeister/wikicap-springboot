import requests
import re
from .wiki_cleaner import CLEANER
from bs4 import BeautifulSoup




HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}


def fetch_year_html(year: int) -> str:
    """
    This function fetches a summary of events that occurred in a given year from Wikipedia.
    args:
        year (int): The year for which to fetch the summary.

    returns: str: A summary of events for the specified year.
    """
    WIKI_API_URL = f"https://en.wikipedia.org/w/api.php"
    params = {
        "action": "parse",
        "page": str(year),
        "prop": "text",
        "format": "json",
        "formatversion": "2",
    }

    response = requests.get(WIKI_API_URL, params = params, headers = HEADERS)
    response.raise_for_status()

    return response.json()["parse"]["text"]




def fetch_year_events(year: int) -> dict:
    """
    This function fetches and processes the year summary from Wikipedia to extract events by month.
    args:
        year (int): The year for which to fetch events.

    returns:
            dict: A dictionary with months as keys and lists of events as values.
    """

    html = fetch_year_html(year)
    soup = BeautifulSoup(html, "html.parser")

    events_h2 = soup.find("h2", id = "Events")
    if not events_h2:
        events_span = soup.find("span", id = "Events")
        events_h2 = events_span.find_parent("h2") if events_span else None

    if not events_h2:
        return {}

    events_by_month = {}
    current_month = None

    node = events_h2.find_next()
    while node:
        if node.name == "h2":
            break

        if node.name == "h3":
            current_month = node.get_text(" ", strip = True)
            events_by_month[current_month] = []

        elif node.name == "li" and current_month:
            if len(events_by_month[current_month]) >= 6:
                node = node.find_next()
                continue
            
            clean = CLEANER.clean_event_line(
                "* " + node.get_text(" ", strip= True),
                keep_date_prefix = False
            )
            if clean:
                events_by_month[current_month].append(clean)

        node = node.find_next()
    return events_by_month