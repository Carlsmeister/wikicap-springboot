import httpx

WIKI_API = "https://en.wikipedia.org/w/api.php"


HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

TIMEOUT = httpx.Timeout(10.0, connect=5.0)



async def fetch_year_toc(client: httpx.AsyncClient, year: int) -> list[dict]:
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
    request_response = await client.get(WIKI_API, params = params)
    request_response.raise_for_status()

    return request_response.json().get("parse", {}).get("tocdata", [])

async def get_month_wikitext(client: httpx.AsyncClient, year: int, month_index: str) -> str:
    """
    Fetches raw wikitext from specific month section.
    Uses the wikipedia API to retrive the unparsed wikitext
    for a specific section, identified by month_index.

    Args:
        year (int): The year of the Wikipedia page.
        month_index (str): The section index for the month.

    Returns:
        str: The raw wikitext of the specified month section.
    """
    params = {
        "action": "parse",
        "page": str(year),
        "prop": "wikitext",
        "section": month_index,
        "format": "json",
        "formatversion": "2",
    }

    request_response = await client.get(WIKI_API, params=params)
    request_response.raise_for_status()

    return request_response.json().get("parse", {}).get("wikitext", "")