from app.core import config
import httpx

BASE_URL = "https://api.themoviedb.org/3"

def get_top_movies_by_year(year: int):
    response = httpx.get(
        f"{BASE_URL}/discover/movie",
        headers={
            "Accept": "application/json",
            "Authorization": "Bearer " + config.TMDB_API_KEY,
        },
        params={
            "primary_release_year": year,
            "sort_by": "vote_count.desc"
        }
    )
    return response.json()
