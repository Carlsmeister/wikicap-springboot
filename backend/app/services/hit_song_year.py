from app.services.artist_of_the_year import get_artist_of_the_year
from app.clients.billboard_artist_client import get_hit_song
import asyncio

          
async def get_year_with_hit_songs(year: int) -> dict:
    """
    Combines artists of a given year with their hit songs. 
    
    Retrives artist for the specified year using get_artrist_of_the_year function.
    Then feches each artist's top hit songs using get_hit_song function. 
    
    Args:
        year (int): The year for which artists and songs to be retrieved.

    :Returns: 
        dict: A dictionary containing the year, artist names and their top songs.
    """   
   
    artists_payload = await get_artist_of_the_year(year)

    # Om get_artist_of_the_year returnerar felobjekt
    if isinstance(artists_payload, dict) and artists_payload.get("error"):
        return {
            "year": year,
            "artists": [],
            "error": artists_payload.get("error")
        }

    # Plocka ut själva listan med artistnamn
    artist_names = artists_payload.get("artists", []) if isinstance(artists_payload, dict) else artists_payload

    result = {
        "year": year,
        "artists": []
    }

    async def fetch_artist_songs(name: str):
        top_songs = await get_hit_song(name, 5)
        if not top_songs:
            return None
        return {
            "artist": name,
            "top_tracks": top_songs
        }

    artist_results = await asyncio.gather(
        *[fetch_artist_songs(name) for name in artist_names],
        return_exceptions=True
    )

    for artist_data in artist_results:
        if artist_data and not isinstance(artist_data, Exception):
            result["artists"].append(artist_data)

    return result
