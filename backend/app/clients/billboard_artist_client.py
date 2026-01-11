import httpx
from dotenv import load_dotenv
import os
from pathlib import Path


HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

def get_billboard_page(year: int) -> str | None:
    """
    Retrieves the HTML Billboard Hot 100 Wikipedia page for a given year.

    This function selects the correct Wikipedia URL based on the year,
    sends an HTTP GET request and returns the page's HTML content as a string.
    If the request fails or the server doesn't respond with a 200 status code, 
    the function returns None.

    Args:
        year (int): The year for which to retrieve the Billboard Hot 100 page.

    Returns:
        str or None: The HTML content of the Wikipedia page if the request succeeds,
        otherwise None.
    """
    
    if year >=2000:
        url = (f"https://en.wikipedia.org/wiki/List_of_Billboard_Hot_100_number_ones_of_{year}")
        
    else: 
        url = ( f"https://en.wikipedia.org/wiki/List_of_Billboard_Hot_100_number-one_singles_of_{year}")
    
    try:
        response = httpx.get(url, headers=HEADERS, timeout=20.0, follow_redirects=True)
    except Exception as e:
        print("DEBUG fetch exception", e)
        return None 
    print("DEBUG fetch:", year, "status:", response.status_code, "url:", str(response.url))   
    
    if response.status_code != 200:
        return None
    
    return response.text
    
URL = "https://ws.audioscrobbler.com/2.0/"
LASTFM_API_KEY = os.getenv("LASTFM_API_KEY")


def get_artist_lastfm(artist_name: str, limit: int=9) ->list[str]:
    """
    Search for artists by name using the Last.fm API.

    This function calls Last.fm's ``artist.search`` endpoint and extracts a list
    of matching artist names. If the request fails, times out, or expected data
    is missing in the API response, an empty list is returned.

    Args:
        artist_name (str): The name of the artist to search for.
        limit (int): Maximum number of artists to request from the API. Defaults to 9.

    Returns:
        list[str]: A list of matching artist names. Returns an empty list if no
        artists are found or the request fails.
    """

    params = {
        "method": "artist.search",
        "artist": artist_name,
        "api_key": LASTFM_API_KEY,
        "format": "json",
        "limit": limit,
    }
    
    try: 
        response = httpx.get(URL, params=params, timeout=15)
    except httpx.ReadTimeout:
        return []
    
    if response.status_code != 200:
        return []
    
    api_data = response.json()
    
    results = api_data.get("results")
    if results is None:
        return []
    
    artistmatches = results.get("artistmatches") 
    if artistmatches is None:  
        return []
    
    artists = artistmatches.get("artist")
    if artists is None: 
        return []
    
    artist_list = []
    
    for artist in artists[:limit]:
        name = artist.get("name")
        if name is not None:
            artist_list.append(name)
            
    return artist_list        


def get_hit_song(artist: str, limit: int=5) -> list[dict]:
    """
    Retrive an artist's hit songs from the Last.fm API. 
    
    This fucntion calls LastFm's "artist.gettoptracks" endpoint to extract the most popluar songs for the given artist.
    If the request fails, times out or data is missing from the API response, an empty list is returned.
    
    Args:
        artist (str): The name of the artist.
        limit (int): Maximum number of songss to request from the API. Defaults to 5.
        
    Returns: 
        list[dict]: A list of dictionaries containing song titles. 
        Returns an empty list if no songs are found or the request fails.    
    """
    
    
    params = {
        "method": "artist.gettoptracks",
        "artist": artist,
        "api_key": LASTFM_API_KEY,
        "format": "json",
        "limit": limit,
        "autocorrect": 1,
    }
        
    try:
        response = httpx.get(URL, params=params, timeout=15)
    except httpx.ReadTimeout:
        return []
    
    print("STATUS:", response.status_code)
    print("URL:", response.url)
    print("RAW RESPONSE:", response.text[:500])
        
    if response.status_code != 200:
        return []
        
    data = response.json()
        
    toptracks = data.get("toptracks")
    if not toptracks:
        return []
            
    tracks = toptracks.get("track")
    if not tracks:
        return []
    
    if isinstance(tracks, dict):
        tracks = [tracks]
        
    return [
        {"title": track.get("name")}
        for track in tracks
        if track.get("name")
    ]  
    