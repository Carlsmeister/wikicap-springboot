import asyncio
import httpx
from app.clients.wiki_client import fetch_year_toc, get_month_wikitext
from app.utils.wiki_cleaner import CLEANER


WIKI_API = "https://en.wikipedia.org/w/api.php"

HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

MONTHS = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
]

TIMEOUT = httpx.Timeout(10.0, connect=5.0)

def normalize_toc(toc) -> list[dict]:
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
    if isinstance(toc, list):
        return toc

    if isinstance(toc, dict):
        items = []
        for value in toc.values():
            if isinstance(value, list):
                items.extend(value)
            elif isinstance(value, dict):
                items.append(value)
        return items
    return []



async def get_month_sections(client: httpx.AsyncClient,year: int, ) -> dict[str, str]:
    """
    Extract month section from a wikipedia year page
    This function maps month names (Jan-Dec) to their corresponding section indices in the TOC data.
    Args:
        year (int): The year for which to extract month sections.

    Returns:
        dict[str, str]: A dictionary mapping month names to their section indices.
    """
    toc = await fetch_year_toc(client, year)
    items = normalize_toc(toc)

    months = {}
    for item in items:
        title = item.get("line", "")
        index = item.get("index", "")

        if title in MONTHS:
            months[title] = index


    return months



def extract_month_events(wikitext: str, limit: int = 6) -> list[str]:
    """
    Extracts and cleans event entries from month wikitext.

    This function processes the raw wikitext of a month section,
    identifies event lines, cleans them using the WikiCleaner,
    and returns a list of cleaned event descriptions.

    Args:
        wikitext (str): The raw wikitext of the month section.
        limit (int): Maximum number of events to extract.

    Returns:
        list[str]: A list of cleaned event descriptions.


    """
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

async def fetch_year_summary(year: int,*, limit: int = 6, concurrency: int = 4) -> dict[str,list[str]]:
    """
    Fetch a sumarized list of events for each month in a given year.
    This functions data flow:
    -fetch month sections from the TOC
    - retrive month-specific wikitext
    - extract and clean event lines

    Args:
        year (int): The year for which to fetch the summary.

    Returns:
        dict: A dictionary with month names as keys and lists of event descriptions as values."""
    results: dict[str, list[str]] = {}
    sem = asyncio.Semaphore(concurrency)

    async with httpx.AsyncClient(headers=HEADERS, timeout=TIMEOUT) as client:
        toc = await fetch_year_toc(client, year)
        items = normalize_toc(toc)

        months = {}
        for item in items:
            title = item.get("line", "")
            index = item.get("index", "")

            if title in MONTHS:
                months[title] = index


        tasks = [fetch_month_events(client, sem, year, month, index, limit)
                for month, index in months.items()]

        for month, events in await asyncio.gather(*tasks):
            if events:
                results[month] = events
    return results


async def fetch_month_events(client: httpx.AsyncClient, sem: asyncio.Semaphore,year: int, month: str, index: str,limit: int):
    async with sem:
        wikitext = await get_month_wikitext(client,year, index)
        events = extract_month_events(wikitext, limit=limit)
        return month, events

