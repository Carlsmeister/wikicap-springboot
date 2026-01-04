import httpx

WIKI_BASE_URL = "https://en.wikipedia.org/w/rest.php/v1"
HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}


def get_year_page_source(year: int) -> str:
    """
    Fetches the raw Wikipedia page source for a given year.

    args:
        year (int): The year for which to fetch the page source.

    returns:
        str: The raw Wikipedia page source.

    raises:
        Exception: If the API request fails.
    """
    response = httpx.get(
        f"{WIKI_BASE_URL}/page/{year}",
        headers=HEADERS,
        timeout=10
    )

    if response.status_code != 200:
        raise Exception(f"Failed to connect to Wikipedia API: {response.status_code}")

    return response.json().get("source", "")

