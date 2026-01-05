from backend.app.core import config
import httpx

BASE_URL = "https://api.themoviedb.org/3"
HEADERS = {
    "Accept": "application/json",
    "Authorization": "Bearer " + config.TMDB_API_KEY,
}

def get_top_movies_by_year(year: int):
    response = httpx.get(
        f"{BASE_URL}/discover/movie",
        headers=HEADERS,
        params={
            "primary_release_year": year,
            "sort_by": "vote_count.desc",
            "vote_average.gte": 7,
            "vote_count.gte": 1000,
        }
    )
    return response.json()

def get_top_series_by_year(year: int):
    response = httpx.get(
        f"{BASE_URL}/discover/tv",
        headers=HEADERS,
        params={
            "air_date.gte": f"{year}-01-01",
            "air_date.lte": f"{year}-12-31",
            "sort_by": "popularity.desc",
            "vote_count.gte": 1000,
            "vote_average.gte": 7,
            "include_null_first_air_dates": False,
        }
    )
    return response.json()
