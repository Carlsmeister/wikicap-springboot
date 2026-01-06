from app.clients.music_client import get_spotify_token, get_auth_header, get_songs_by_year, get_artists_by_year
import base64
import httpx


def fetch_songs_for_year(year: int):
    print("Spotify fetch songs)")
    token = get_spotify_token()
    auth_header = get_auth_header(token)
    raw = get_songs_by_year(year, auth_header)

    songs = []
    for item in raw[:10]:
        songs.append({
            "title": item["name"],
            "artist": item["artists"][0]["name"],
            "album": item["album"]["name"],
            "releaseDate": item["album"]["release_date"],
            "previewUrl": item["preview_url"]
        })
    print(songs[0])

    return {
        "year": year,
        "topSongs": songs,
        "source": "Spotify"
    }

def fetch_artists_for_year(year: int):
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
            "spotifyUrl": item["external_urls"]["spotify"]
        })

    print(artists[0])
    return {
        "year": year,
        "topArtists": artists,
        "source": "Spotify"
    }