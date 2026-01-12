"""
Movies and TV Series API endpoints module.

This module provides API endpoints for retrieving top-rated movies and TV series
for a specific year using The Movie Database (TMDb) API. Includes comprehensive
error handling for API failures, rate limiting, and data validation.
"""

from fastapi import APIRouter, HTTPException, status
from app.services.movie_service import fetch_movies_for_year
from app.services.movie_service import fetch_series_for_year
from app.utils.validate_year import validate_year
import httpx


router = APIRouter()

@router.get("/year/{year}/movies")
async def get_movies(year: int):
    """
    Retrieve top-rated movies for a specific year.

    Fetches the top 8 movies released in the specified year from TMDb,
    filtered by vote count and rating. Results are sorted by vote count
    to surface the most popular critically-acclaimed films.

    Args:
        year (int): The release year to retrieve movies for (e.g., 2020).

    Returns:
        dict: A dictionary containing:
            - year (int): The requested year
            - top_movies (list): List of up to 8 movie objects with:
                - title (str): Movie title
                - rating (float): Average vote rating (0-10, rounded to 1 decimal)
                - votes (int): Total number of votes
                - poster (str): Poster image path (TMDb URL path)
                - release_date (str): Release date in YYYY-MM-DD format
            - source (str): Data source identifier ("TMDb")

    Raises:
        HTTPException:
            - 400: Invalid year format or out of valid range
            - 404: No movie data found for the specified year
            - 429: TMDb API rate limit exceeded
            - 502: TMDb API returned an error response
            - 503: Unable to connect to TMDb service

    Example:
        GET /api/v1/year/2020/movies

        Response:
        {
            "year": 2020,
            "top_movies": [
                {
                    "title": "Tenet",
                    "rating": 7.5,
                    "votes": 5234,
                    "poster": "/path/to/poster.jpg",
                    "release_date": "2020-08-26"
                },
                ...
            ],
            "source": "TMDb"
        }
    """
    validate_year(year)

    try:
        movies = await fetch_movies_for_year(year)
    except httpx.HTTPStatusError as e:
        code = e.response.status_code
        if code == 404:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"NOT FOUND: No movie data found for year {year}."
            )
        if code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="TOO MANY REQUESTS: No movies for year {year} found."
            )

        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"BAD GATEWAY: Movie service returned {code}."
        )
    except httpx.RequestError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="SERVICE UNAVAILABLE: An error occurred while trying to connect to the Movie service."
        )

    if not movies:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"NOT FOUND: No movie data found for year {year}."
        )

    return movies

@router.get("/year/{year}/series")
async def get_series(year: int):
    """
    Retrieve top-rated TV series for a specific year.

    Fetches the top 8 TV series that aired in the specified year from TMDb.
    Results are ranked using a custom scoring algorithm that considers popularity,
    rating, and an age penalty factor to prioritize recent and relevant shows.

    Args:
        year (int): The air year to retrieve TV series for (e.g., 2020).

    Returns:
        dict: A dictionary containing:
            - year (int): The requested year
            - top_series (list): List of up to 8 TV series objects with:
                - title (str): Series name
                - rating (float): Average vote rating (0-10, rounded to 1 decimal)
                - votes (int): Total number of votes
                - poster (str): Poster image path (TMDb URL path)
                - release_date (str): First air date in YYYY-MM-DD format
            - source (str): Data source identifier ("TMDb")

    Raises:
        HTTPException:
            - 400: Invalid year format or out of valid range
            - 404: No series data found for the specified year
            - 429: TMDb API rate limit exceeded
            - 502: TMDb API returned an error response
            - 503: Unable to connect to TMDb service

    Example:
        GET /api/v1/year/2020/series

        Response:
        {
            "year": 2020,
            "top_series": [
                {
                    "title": "The Queen's Gambit",
                    "rating": 8.6,
                    "votes": 3421,
                    "poster": "/path/to/poster.jpg",
                    "release_date": "2020-10-23"
                },
                ...
            ],
            "source": "TMDb"
        }

    Note:
        Series are ranked using a custom algorithm that applies an age penalty
        to older shows, ensuring that series relevant to the queried year are
        prioritized over long-running shows that merely aired during that year.
    """
    validate_year(year)

    try:
        series = await fetch_series_for_year(year)

    except httpx.HTTPStatusError as e:
        code = e.response.status_code
        if code == 404:
            raise HTTPException(
                status_code = status.HTTP_404_NOT_FOUND,
                detail = f"NOT FOUND: No series data found for year {year}."
            )
        if code == 429:
            raise HTTPException(
                status_code = status.HTTP_429_TOO_MANY_REQUESTS,
                detail = "TOO MANY REQUESTS: Rate limit exceeded when accessing series data."
            )
        raise HTTPException(
            status_code = status.HTTP_502_BAD_GATEWAY,
            detail = f"BAD GATEWAY: Series service returned {code}."
        )

    if not series:
        raise HTTPException(
            status_code = status.HTTP_404_NOT_FOUND,
            detail = f"NOT FOUND: No series data found for year {year}."
        )

    return series
