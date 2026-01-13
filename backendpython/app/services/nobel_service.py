import httpx
from app.utils.wiki_nobel_extractor import extract_nobel

WIKI_API = "https://en.wikipedia.org/w/api.php"

HEADERS = {
    "User-Agent": "WikiCap/1.0 (school project; contact: alex@example.com)",
    "Accept": "application/json",
    "Accept-Language": "en",
}

async def get_nobel_prizes(year: int) -> dict:
    """
    Fetch nobel prize lauureates for a given year and extract structured data.

    This function requests the wikipedia page "{year}_Nobel_Prizes" using
    mediawiki "parse" endpoint with "prop=text", then parses the returned
    HTML to extract Nobel Prize laureates using the extract_nobel utility function.

    Args:
        year (int): The year for which to fetch Nobel Prize data.

    Returns:
        dict: A dictionary containing the year and a nested dictionary of Nobel Prize categories and their laureates.

        Example:
            {
            "year": 1997,
            "prizes": {
                "Physics": [{"name": "...", "motivation": "...", "image": "..."}]
            }
            }
    Raises:
        httpx.HTTPStatusError: If the HTTP request returns an unsuccessful status code.
        httpx.RequestError: If there is an issue making the HTTP request.
    """
    title = f"{year}_Nobel_Prizes"

    params = {
        "action": "parse",
        "page": title,
        "prop": "text",
        "format": "json",
        "formatversion": "2",
    }

    async with httpx.AsyncClient() as client:
        r = await client.get(WIKI_API, params=params, headers=HEADERS, timeout=20)
        r.raise_for_status()
        data = r.json()

    html = (data.get("parse", {}).get("text") or "")

    prizes = extract_nobel(html)
    return {
        "year": year,
        "prizes": prizes,
    }