import httpx
import re
from .wiki_cleaner import CLEANER

MONTHS = re.compile(
    r"\n=+\s*(January|February|March|April|May|June|July|August|September|October|November|December)\s*=+\s*\n",
)


HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}


def fetch_year_summary(year: int) -> str:
    """
    This function fetches a summary of events that occurred in a given year from Wikipedia.
    args:
        year (int): The year for which to fetch the summary.

    returns: str: A summary of events for the specified year.
    """
    WIKI_API_URL = f"https://en.wikipedia.org/w/rest.php/v1/page/{year}"
    fetch = httpx.get(WIKI_API_URL, headers=HEADERS, timeout=10)
    if fetch.status_code != 200:
        raise Exception("Failed to connect to Wikipedia API")

    return fetch.json().get("source", "")


def strip_wiki_recap(text: str) -> str:
    """
    This function removes the all the text before the '== month events ==' section in a Wikipedia year summary.
    args:
        text (str): The Wikipedia year summary text.

    returns: str: The cleaned summary text.
    """
    split_text = "== January =="
    index = text.find(split_text)
    return text[index: ] if index != -1 else text

def split_by_months(text: str) -> dict:
    """
    This function splits the year summary text into a dictionary of months and their corresponding events.
    args:
        text (str): The cleaned Wikipedia year summary text.
    """
    months = MONTHS.split(text)

    month_dict = {}

    for month in range(1, len(months), 2):
        month_name = months[month]
        content = months[month + 1]
        month_dict[month_name] = content.strip()

    return month_dict

def extract_year_events(month_text: str, limit: int = 3) -> list:
    """
    This function extracts a list of events from a month's text in the year summary.
    args:
        month_text (str): The text for a specific month.
        limit (int): The maximum number of events to extract.

    returns: list: A list of extracted events.
    """
    events = []

    for line in month_text.splitlines():
        if not line.startswith("*"):
            continue

        clean = CLEANER.clean_event_line(line, max_len=200, keep_date_prefix=True)
        if not clean:
            continue

        events.append(clean)
        if len(events) >= limit:
            break

    return events

def fetch_year_events(year: int) -> dict:
    """
    This function fetches and processes the year summary from Wikipedia to extract events by month.
    args:
        year (int): The year for which to fetch events.

    returns:
            dict: A dictionary with months as keys and lists of events as values.
    """
    text = fetch_year_summary(year)
    text = strip_wiki_recap(text)

    months = split_by_months(text)

    return {
        month: extract_year_events(content, limit=6)
        for month, content in months.items()
        if content
    }