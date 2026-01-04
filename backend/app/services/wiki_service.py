import re
from app.clients.wiki_client import get_year_page_source
from app.utils.wiki_cleaner import CLEANER

MONTHS = re.compile(
    r"\n=+\s*(January|February|March|April|May|June|July|August|September|October|November|December)\s*=+\s*\n",
)


def strip_wiki_recap(text: str) -> str:
    """
    Removes all the text before the '== January ==' section in a Wikipedia year summary.

    args:
        text (str): The Wikipedia year summary text.

    returns:
        str: The cleaned summary text starting from January.
    """
    split_text = "== January =="
    index = text.find(split_text)
    return text[index:] if index != -1 else text


def split_by_months(text: str) -> dict:
    """
    Splits the year summary text into a dictionary of months and their corresponding events.

    args:
        text (str): The cleaned Wikipedia year summary text.

    returns:
        dict: A dictionary with month names as keys and event text as values.
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
    Extracts a list of events from a month's text in the year summary.

    args:
        month_text (str): The text for a specific month.
        limit (int): The maximum number of events to extract.

    returns:
        list: A list of extracted and cleaned events.
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
    Fetches and processes the year summary from Wikipedia to extract events by month.

    args:
        year (int): The year for which to fetch events.

    returns:
        dict: A dictionary with months as keys and lists of events as values.
    """
    text = get_year_page_source(year)
    text = strip_wiki_recap(text)

    months = split_by_months(text)

    return {
        month: extract_year_events(content, limit=6)
        for month, content in months.items()
        if content
    }

