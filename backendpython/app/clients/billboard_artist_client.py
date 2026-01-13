import httpx
import os

HEADERS = {
    "User-Agent": "WikiCap/1.0 (https://github.com/WikiCap/year-overview)"
}

async def get_billboard_page(year: int) -> str | None:
    """
    Retrieves the HTML Billboard Hot 100 Wikipedia page for a given year.

    Selects the correct Wikipedia URL based on the year,
    sends an HTTP GET request to fetch the page. If the request succeeds, the page's HTML content 
    is returned as a string. If the request fails or the server doesn't respond with a 200 status code, 
    the function returns None.

    Parameters
    -----------
        year: int
            The year for which to retrieve the Billboard Hot 100 page.

    Returns
    --------
        str or None: 
            The HTML content of the Wikipedia page if the request succeeds,
            otherwise None.
    """
    
    if year >=2000:
        url = (f"https://en.wikipedia.org/wiki/List_of_Billboard_Hot_100_number_ones_of_{year}")
        
    else: 
        url = ( f"https://en.wikipedia.org/wiki/List_of_Billboard_Hot_100_number-one_singles_of_{year}")
    
    async with httpx.AsyncClient() as client:
        response = await client.get(url, headers=HEADERS, timeout=20.0, follow_redirects=True)
        response.raise_for_status()

    return response.text

URL = "https://ws.audioscrobbler.com/2.0/"
LASTFM_API_KEY = os.getenv("LASTFM_API_KEY")


async def get_artist_lastfm(artist_name: str, limit: int=9) ->list[str]:
    """
    Search for artists by name using the Last.fm API.

    This asynchronous function calls Last.fm's ``"artist.search"`` endpoint 
    and extracts a list of matching artist names. If the API resonse is missing 
    expected data, an empty list is returned.

    Parameters
    -----------
        artist_name: str
            The name of the artist to search for.
        limit: int, optional
            Maximum number of artists to request from the API. Defaults to 9.

    Returns
    --------
        list[str]: 
            A list of matching artist names. Returns an empty list if the API
            response does not contain the expected structure. 
    """
    params = {
        "method": "artist.search",
        "artist": artist_name,
        "api_key": LASTFM_API_KEY,
        "format": "json",
        "limit": limit,
    }
    
    async with httpx.AsyncClient() as client:
        response = await client.get(URL, params=params, timeout=15)
        response.raise_for_status()
    
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


async def get_hit_song(artist: str, limit: int=5) -> list[dict]:
    """
    Retrieve an artist's hit songs from the Last.fm API. 
    
    This asynchronous fucntion calls LastFm's ``"artist.gettoptracks"`` endpoint to extract the most popluar songs for the given artist.
    If the request fails, times out or data is missing from the API response, an empty list is returned.
    
    Parameters
    ----------
        artist: str
            The name of the artist.
        limit: int, optional
            Maximum number of songss to request from the API. Defaults to 5.
        
    Returns
    ------- 
        list[dict]: 
            A list of dictionaries containing song titles under the key ``"title"``.
        Returns an empty list if the API response does not include the expected 
        structure or the request fails.    
    """
    
    
    params = {
        "method": "artist.gettoptracks",
        "artist": artist,
        "api_key": LASTFM_API_KEY,
        "format": "json",
        "limit": limit,
        "autocorrect": 1,
    }
        
    async with httpx.AsyncClient() as client:
        response = await client.get(URL, params=params, timeout=15)
        response.raise_for_status()
        
    data = response.json()

    top_tracks = data.get("toptracks")
    if not top_tracks:
        return []
            
    tracks = top_tracks.get("track")
    if not tracks:
        return []
    
    if isinstance(tracks, dict):
        tracks = [tracks]
        
    return [
        {"title": track.get("name")}
        for track in tracks
        if track.get("name")
    ]
