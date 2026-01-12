from app.core import config
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


def search_movie_by_title(title: str, year: int | None = None):
    """Search for a movie by title and optionally year to get its details including poster"""
    params = {
        "query": title,
        "include_adult": False,
    }
    if year:
        params["year"] = year

    response = httpx.get(
        f"{BASE_URL}/search/movie",
        headers=HEADERS,
        params=params,
    )
    results = response.json().get("results", [])
    return results[0] if results else None


def search_person_by_name(name: str):
    """Search for a person by name to get their profile image"""
    response = httpx.get(
        f"{BASE_URL}/search/person",
        headers=HEADERS,
        params={
            "query": name,
            "include_adult": False,
        }
    )
    results = response.json().get("results", [])
    return results[0] if results else None

