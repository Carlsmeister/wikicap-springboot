from app.clients.music_client import get_spotify_token, get_auth_header, get_songs_by_year, get_artists_by_year
import base64
import httpx
import random


def fetch_songs_for_year(year: int):
    """
    Fetches relevant songs from Spotify for a specific year.
    Args:
        year (int): The year to search for songs.
    Returns:
        dict: A dictionary containing the year and a list of top songs.
    """

    token = get_spotify_token()
    auth_header = get_auth_header(token)
    raw = get_songs_by_year(year, auth_header)

    raw = sorted(
    raw,
    key=lambda x: x["popularity"], # Sortera efter popularitet (högre = mer populär)
    reverse=True) # Kör listan från högst till lägst

    raw = random.sample(raw, min(len(raw), 10)) # Väljer 10 random låtar från listan

    songs = []
    for item in raw:
        songs.append({
            "title": item["name"],
            "artist": item["artists"][0]["name"],
            "album": item["album"]["name"],
            "release_date": item["album"]["release_date"],
            "preview_url": item["preview_url"],
            "spotify_url": item["external_urls"]["spotify"],
            "image": item["album"]["images"][0]["url"],
            
        })

    return {
        "year": year,
        "top_songs": songs,
        "source": "Spotify"
    }

def fetch_artists_for_year(year: int):
    """
    Fetches relevant artists from Spotify for a specific year.
    Args:
        year (int): The year to search for artists.
    Returns:
        dict: A dictionary containing the year and a list of top artists.
    """

    print("Spotify fetch artists)")
    token = get_spotify_token()
    auth_header = get_auth_header(token)
    raw = get_artists_by_year(year, auth_header)

    artists = []
    for item in raw:
        artists.append({
            "name": item["name"],
            "genres": item["genres"],
            "popularity": item["popularity"],
            "followers": item["followers"]["total"],
            "spotify_url": item["external_urls"]["spotify"]
        })

    return {
        "year": year,
        "top_artists": artists,
        "source": "Spotify"
    }