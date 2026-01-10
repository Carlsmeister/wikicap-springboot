from fastapi import APIRouter, HTTPException, status
from app.services.movie_service import fetch_movies_for_year
from app.services.movie_service import fetch_series_for_year
from app.utils.validate_year import validate_year
import httpx


router = APIRouter()

@router.get("/year/{year}/movies")
def get_movies(year: int):
    validate_year(year)

    try:
        movies = fetch_movies_for_year(year)
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
def get_series(year: int):

    validate_year(year)

    try:
        series = fetch_series_for_year(year)

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

    return fetch_series_for_year(year)
