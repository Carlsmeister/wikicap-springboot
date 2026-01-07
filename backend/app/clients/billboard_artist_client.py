from bs4 import BeautifulSoup 
import httpx
from dotenv import load_dotenv
import os
from pathlib import Path


HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

def get_billboard_artist(year: int) ->list[str]:
    """
    Retrieve the Billboard Wikipedia page and table class for a given year.

    This function selects the correct Wikipedia URL pattern based on the year,
    sends an HTTP GET request to fetch the page, and returns both the HTTP
    response object and the expected table CSS class used for parsing artist
    information. If the request fails, both values are returned as None.

    Args:
        year (int): The year for which to retrieve the Billboard page.

    Returns:
        tuple: A tuple containing:
            - httpx.Response or None: The HTTP response if the request was successful.
            - str or None: The CSS class name of the Billboard data table.
            Returns (None, None) if the request fails.
    """
    if year >=2000:
        url = (f"https://en.wikipedia.org/wiki/"
               f"List_of_Billboard_Hot_100_number-one_of_{year}"
        )
        table_class = "wikitable plainrowheaders top 3"
    else: 
        url = ( f"https://en.wikipedia.org/wiki/"
                f"List_of_Billboard_Hot_100_number-one_singles_of_{year}"
        )
        table_class = "wikitable plainrowheaders"
    
    response = httpx.get(url, headers=HEADERS)
    if response.status_code != 200:
        return None, None
    
    return response, table_class
    
URL = "https://ws.audioscrobbler.com/2.0/"
LASTFM_API_KEY = os.getenv("LASTFM_API_KEY")


def get_artist_lastfm(artist_name: str, limit: int=5) ->list[dict]:
    """
    Search for artists by name using the Last.fm API.

    This function calls Last.fm's ``artist.search`` endpoint and returns a list
    of matching artist names. If the request fails, times out, or expected data
    is missing in the API response, an empty list is returned.

    Args:
        artist_name (str): The name of the artist to search for.
        limit (int): Maximum number of artists to return. Defaults to 5.

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
    
    for artist in artists:
        name = artist.get("name")
        if name is not None:
            artist_list.append(name)
            
    return artist_list        


def get_hit_song(artist: str, limit: int=5) -> list[dict]:
    """
    Retrive an artist's hit songs from the Last.fm API. 
    
    This fucntion calls LastFm's "artist.gettoptracks" endpoint to returns a list of the artists most popluar songs.
    If the request fails, times out or data is missing from the API response, an empty list is returned.
    
    Args:
        artist (str): The name of the artist.
        limit (int): Maximum number of songss to return. Defaults at 5.
        
    Returns: 
        list[dict]: A list of dictionaries containing song titles. 
        Returns an empty list if no songs are found.    
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
    