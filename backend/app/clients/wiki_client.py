import httpx

WIKI_API = "https://en.wikipedia.org/w/api.php"


HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

TIMEOUT = httpx.Timeout(10.0, connect=5.0)



async def fetch_year_toc(client: httpx.AsyncClient, year: int) -> list[dict]:
    """
    Fetches the wikipedia table of contents for a given year.

    This function uses Mediawiki "parse" endpoint with "prop=tocdata" to
    retrieve sections of information available on the Wikipedia page for the specified year.
    This can be used to identify correct section indexes for later calls,

    Args:
        client (httpx.AsyncClient): An instance of httpx AsyncClient for making requests.
        year (int): The year for which to fetch the table of contents.

    Returns:
        list[dict]: A list of section dictionaries representing the table of contents.
        Returns an empty list if no sections are found.

        Raises:
            httpx.HTTPStatusError: If the HTTP request returns an unsuccessful status code.
            httpx.RequestError: If there is an issue making the HTTP request.
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
    Fetch raw wikitext for a specific section of the wikipedia year page

    The section is identified by its index in the table of contents., example:
    "month_index" typically taken from the TOC data fetched via fetch_year_toc function.

    Args:
        client: An initialized `httpx.AsyncClient` used to make the request.
        year: The year (e.g. 1997) whose Wikipedia page should be queried.
        month_index: The section index for the month/section to fetch.

    Returns:
        The raw wikitext content of the specified month/section as a string.
        Returns an empty string if the section is not found.

    Raises:
        httpx.HTTPStatusError: If the HTTP request returns an unsuccessful status code.
        httpx.RequestError: If there is an issue making the HTTP request.
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