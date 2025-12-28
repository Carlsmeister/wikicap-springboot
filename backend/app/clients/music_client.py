import json
from app.core import config
import httpx

BASE_URL = "https://billboard-api2.p.rapidapi.com"
SPOTIFY_BASE_URL = "https://api.spotify.com/v1/search"

def get_top_artists_by_year(year: int): #  Ska tas bort sen
    response = httpx.get(
        f"{BASE_URL}/year-end-chart/top-artists",
        headers={
            "Accept": "application/json",
            'x-rapidapi-key': config.BILLBOARD_100_API_KEY,
            'x-rapidapi-host': "billboard-api2.p.rapidapi.com"
        },
        params={
            "year": year
        }
    )
    
    return response.json()


def get_songs_by_year(year: int, token):
    
    query = f"?q={year}&type=track,artist&limit=1"  #Limit = 1, så endast en artist returneras
    query_url = SPOTIFY_BASE_URL + query

    response = httpx.get(query_url, headers=token)
    response.raise_for_status()

    artists = response.json()["artists"]["items"]
    if not artists:
        print("No artists found for the given year.")
        return None
    
    return artists  # Returna första resultatet
    