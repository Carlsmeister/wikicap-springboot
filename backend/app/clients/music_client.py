import base64
import json
from app.core import config
import httpx

SPOTIFY_BASE_URL = "https://api.spotify.com/v1/search"
SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token"

spotify_client_id = config.SPOTIFY_CLIENT_ID
spotify_client_secret = config.SPOTIFY_CLIENT_SECRET

def get_spotify_token():
    """
    Retrieves an access token from the Spotify API using client credentials.
    Token is required for making authorized requests to Spotify endpoints.
    Token is valid for 1 hour.
    
    Returns: 
        json: Access token string.
    """

    auth_string = f"{spotify_client_id}:{spotify_client_secret}"
    auth_base64 = base64.b64encode(auth_string.encode()).decode()
    
    response = httpx.post(
        SPOTIFY_TOKEN_URL,
        headers={
            "Authorization": f"Basic {auth_base64}",
            "Content-Type": "application/x-www-form-urlencoded",
        },
        data={"grant_type": "client_credentials"}
    )
    response.raise_for_status()

    return response.json()["access_token"]

def get_auth_header(token):
    """
    Constructs the authorization header for Spotify API requests.
    Args:
        token (str): The access token obtained from Spotify.
    Returns:
        Authorization header with Bearer token.
    """

    spotify_token = token
    return {"Authorization": f"Bearer {spotify_token}"}

def get_artists_by_year(year: int, token):
    """
    Fetches artists from Spotify released in a specific year.
    Args:
        year (int): The year to search for artists.
        token (str): The access token for Spotify API.
    Returns:
        list: A list of artist items from Spotify.
    """

    response = httpx.get(
        SPOTIFY_BASE_URL,
        headers=token,
        params={
            "q": f"year:{year}",
            "type": "artist",
            "limit": 10
        }
    )
    
    return response.json()["artists"]["items"]

def get_songs_by_year(year: int, headers):
    """
    Fetches songs from Spotify released in a specific year.
    Args:
        year (int): The year to search for songs.
        token (str): The access token for Spotify API.
    Returns:
        list: A list of song items from Spotify.
    """

    response = httpx.get(
        SPOTIFY_BASE_URL,
        headers=headers,
        params={
            "q": f"year:{year}",
            "type": "track",
            "limit": 30,
        }
    )
    response.raise_for_status()

    songs = response.json()["tracks"]["items"]
    if not songs:
        print("No songs found for the given year.")
        return []
    
    return songs