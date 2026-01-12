import urllib.parse
import httpx

WIKI_SUMMARY = "https://en.wikipedia.org/api/rest_v1/page/summary"
HEADERS = {"User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"}


def fetch_wiki_image(title:str) -> str | None:
    """
    Fetch the main image URL for a given Wikipedia page title.
    
    Retrieves image information from the Wikipedia summary API and returns the page's 
    thumbnail or original image URL if available. 
    
    Parameters:
        title (str):
            The title of the Wikipedia page to look up.

    Returns:
        str or None:
            The URL of the page's thumbnail or original image if available;
            otherwise, None.
    """
    safe_title = urllib.parse.quote(title.replace(" ", "_"))
    url = f"{WIKI_SUMMARY}/{safe_title}"
    
    response = httpx.get(url, headers=HEADERS, timeout=10)
    response.raise_for_status()
    
    data = response.json()
    if "thumbnail" in data and data["thumbnail"].get("source"):
        return data["thumbnail"]["source"]
    if "originalimage" in data and data["orginalimage"].get("source"):
        return data["originalimage"]["source"]
    
    return None 

