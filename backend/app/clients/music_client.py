import base64
import json
import os
from app.core import config
import httpx

SPOTIFY_BASE_URL = "https://api.spotify.com/v1/search"
SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token"


spotify_client_id = os.getenv("SPOTIFY_CLIENT_ID")
spotify_client_secret = os.getenv("SPOTIFY_CLIENT_SECRET")

def get_spotify_token(): # Getter för att få en token från Spotify, den varar i 1 timme
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
    spotify_token = token
    return {"Authorization": f"Bearer {spotify_token}"}

def get_artists_by_year(year: int, token):
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

def get_songs_by_year(year: int, token):
    response = httpx.get(
        SPOTIFY_BASE_URL,
        headers=token,
        params={
            "q": f"year:{year}",
            "type": "track",
            "limit": 10
        }
    )
    songs = response.json()["tracks"]["items"]
    if not songs:
        print("No songs found for the given year.")
        return None
    
    return songs