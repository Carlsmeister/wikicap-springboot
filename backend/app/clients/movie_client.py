"""
TMDb (The Movie Database) API client module.

This module provides async HTTP client functions for interacting with The Movie
Database (TMDb) API v3. Handles authentication, request construction, and returns
raw JSON responses for movies, TV series, and person search operations.

API Documentation: https://developers.themoviedb.org/3
"""

from app.core import config
import httpx

BASE_URL = "https://api.themoviedb.org/3"
HEADERS = {
    "Accept": "application/json",
    "Authorization": "Bearer " + config.TMDB_API_KEY,
}

async def get_top_movies_by_year(year: int):
    """
    Fetch top-rated movies for a specific release year from TMDb.

    Uses the TMDb discover/movie endpoint to retrieve movies filtered by:
    - Primary release year matching the specified year
    - Vote average ≥ 7.0
    - Vote count ≥ 1000
    - Sorted by vote count (descending)

    Args:
        year (int): The primary release year to filter by.

    Returns:
        dict: Raw TMDb API response containing:
            - results (list): Array of movie objects with fields like title,
                             vote_average, vote_count, poster_path, etc.
            - page (int): Current page number
            - total_results (int): Total number of results
            - total_pages (int): Total number of pages

    Raises:
        httpx.HTTPStatusError: If the API returns a non-2xx status code
        httpx.RequestError: If the request fails due to network issues

    Example:
        await get_top_movies_by_year(2020)
        {"results": [...], "page": 1, "total_results": 42, ...}
    """
    async with httpx.AsyncClient() as client:
        response = await client.get(
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


async def get_top_series_by_year(year: int):
    """
    Fetch top-rated TV series that aired during a specific year from TMDb.

    Uses the TMDb discover/tv endpoint to retrieve series filtered by:
    - Air date between January 1 and December 31 of the specified year
    - Vote average ≥ 7.0
    - Vote count ≥ 1000
    - Sorted by popularity (descending)
    - Excludes entries with null first air dates

    Args:
        year (int): The year to filter series by (based on air date).

    Returns:
        dict: Raw TMDb API response containing:
            - results (list): Array of TV series objects with fields like name,
                             vote_average, vote_count, poster_path, first_air_date, etc.
            - page (int): Current page number
            - total_results (int): Total number of results
            - total_pages (int): Total number of pages

    Raises:
        httpx.HTTPStatusError: If the API returns a non-2xx status code
        httpx.RequestError: If the request fails due to network issues

    Example:
        await get_top_series_by_year(2020)
        {"results": [...], "page": 1, "total_results": 38, ...}
    """
    async with httpx.AsyncClient() as client:
        response = await client.get(
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


async def search_movie_by_title(title: str, year: int | None = None):
    """
    Search TMDb for a movie by title with optional year filtering.

    Uses the TMDb search/movie endpoint to find movies matching the given title.
    Returns the first result (best match) or None if no results found.

    Args:
        title (str): The movie title to search for.
        year (int | None): Optional release year to narrow results.

    Returns:
        dict | None: First movie result object containing fields like:
            - id (int): TMDb movie ID
            - title (str): Movie title
            - poster_path (str): Path to poster image
            - vote_average (float): Average vote rating
            - release_date (str): Release date
            - overview (str): Movie plot summary

        Returns None if no results found.

    Raises:
        httpx.HTTPStatusError: If the API returns a non-2xx status code
        httpx.RequestError: If the request fails due to network issues

    Example:
        await search_movie_by_title("Tenet", 2020)
        {"id": 577922, "title": "Tenet", "poster_path": "/path.jpg", ...}
        await search_movie_by_title("NonexistentMovie12345")
        None
    """
    params = {
        "query": title,
        "include_adult": False,
    }
    if year:
        params["year"] = year

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{BASE_URL}/search/movie",
            headers=HEADERS,
            params=params,
        )
        results = response.json().get("results", [])
        return results[0] if results else None


async def search_person_by_name(name: str):
    """
    Search TMDb for a person (actor, director, etc.) by name.

    Uses the TMDb search/person endpoint to find people matching the given name.
    Returns the first result (best match) or None if no results found. Useful
    for retrieving profile images and biographical information.

    Args:
        name (str): The person's name to search for.

    Returns:
        dict | None: First person result object containing fields like:
            - id (int): TMDb person ID
            - name (str): Person's name
            - profile_path (str): Path to profile image
            - known_for_department (str): Primary department (e.g., "Acting")
            - known_for (list): Array of notable works
            - popularity (float): Popularity score

        Returns None if no results found.

    Raises:
        httpx.HTTPStatusError: If the API returns a non-2xx status code
        httpx.RequestError: If the request fails due to network issues

    Example:
        await search_person_by_name("Joaquin Phoenix")
        {"id": 73421, "name": "Joaquin Phoenix", "profile_path": "/path.jpg", ...}
        await search_person_by_name("NonexistentActor12345")
        None
    """
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{BASE_URL}/search/person",
            headers=HEADERS,
            params={
                "query": name,
                "include_adult": False,
            }
        )
        results = response.json().get("results", [])
        return results[0] if results else None
