from app.services.artist_of_the_year import get_artist_of_the_year
from app.clients.billboard_artist_client import get_hit_song

          
def get_year_with_hit_songs(year: int, limit: int = 5) -> dict:
    """
    Combines artists of a given year with their hit songs. 
    
    Retrives artist for the specified year using get_artrist_of_the_year function.
    Then feches each artist's top hit songs using get_hit_song function. 
    
    Args:
        year (int): The year for which artists and songs to be retrieved.
        limit (int): Maximum number of top songs to return for each artist. Defaults at 5.
    
    :Returns: 
        dict: A dictionary containing the year, artist names and their top songs.
    """   
   
    artists_payload = get_artist_of_the_year(year)

    # Om get_artist_of_the_year returnerar felobjekt
    if isinstance(artists_payload, dict) and artists_payload.get("error"):
        return {
            "year": year,
            "artist": [],
            "error": artists_payload.get("error")
        }

    # Plocka ut själva listan med artistnamn
    artist_names = artists_payload.get("artists", []) if isinstance(artists_payload, dict) else artists_payload

    result = {
        "year": year,
        "artist": []
    }

    for name in artist_names:
        top_songs = get_hit_song(name, limit)
        if not top_songs:
            continue

        result["artist"].append({
            "artist": name,
            "toptracks": top_songs
        })

    return result

   