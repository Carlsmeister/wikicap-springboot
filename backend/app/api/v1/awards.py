"""
Awards API endpoints module.

This module provides API endpoints for retrieving Oscar (Academy Awards) highlights
for a specific year. Data is sourced from The Awards API and enriched with images
from The Movie Database (TMDb). Includes comprehensive error handling for API
failures, rate limiting, and data validation.
"""

from fastapi import APIRouter, HTTPException, status
import httpx
from app.services.awards_service import fetch_oscar_highlights
from app.utils.validate_year import validate_year


router = APIRouter()

@router.get("/year/{year}/awards")
async def get_awards(year: int):
    """
    Retrieve Oscar highlights for a specific year.

    Fetches the winners for three major Oscar categories (Best Picture, Best Actor,
    and Best Actress) for the specified year. Movie posters and actor profile images
    are retrieved from TMDb to enrich the response data.

    Args:
        year (int): The Oscar ceremony year to retrieve awards for (e.g., 2020).

    Returns:
        dict: A dictionary containing:
            - year (int): The requested year
            - oscars (dict): Dictionary of award categories:
                - bestPicture (dict, optional):
                    - title (str): Movie title
                    - poster (str | None): Poster image path from TMDb
                - bestActor (dict, optional):
                    - name (str): Actor's name
                    - movie (str): Movie they won for
                    - image (str | None): Profile image path from TMDb
                - bestActress (dict, optional):
                    - name (str): Actress's name
                    - movie (str): Movie they won for
                    - image (str | None): Profile image path from TMDb
            - source (str): Data source identifier ("The Awards API")

    Raises:
        HTTPException:
            - 400: Invalid year format or out of valid range
            - 404: No Oscar data found for the specified year
            - 429: API rate limit exceeded
            - 502: The Awards API returned an error response
            - 503: Unable to connect to The Awards API service

    Example:
        GET /api/v1/year/2020/awards

        Response:
        {
            "year": 2020,
            "oscars": {
                "bestPicture": {
                    "title": "Parasite",
                    "poster": "/path/to/poster.jpg"
                },
                "bestActor": {
                    "name": "Joaquin Phoenix",
                    "movie": "Joker",
                    "image": "/path/to/profile.jpg"
                },
                "bestActress": {
                    "name": "Renée Zellweger",
                    "movie": "Judy",
                    "image": "/path/to/profile.jpg"
                }
            },
            "source": "The Awards API"
        }

    Note:
        Not all categories may be present if data is unavailable for the given year.
        Image paths may be None if TMDb lookup fails.
    """
    validate_year(year)

    try:
        highlights = await fetch_oscar_highlights(year)

    except httpx.HTTPStatusError as e:
        code = e.response.status_code
        if code == 404:
            raise HTTPException(
                status_code = status.HTTP_404_NOT_FOUND,
                detail = f"NOT FOUND: No Oscar data found for year {year}."
            )
        if code == 429:
            raise HTTPException(
                status_code = status.HTTP_429_TOO_MANY_REQUESTS,
                detail = "TOO MANY REQUESTS: Rate limit exceeded when accessing Oscar data."
            )
        raise HTTPException(
            status_code = status.HTTP_502_BAD_GATEWAY,
            detail = f"BAD GATEWAY: Oscar service returned {code}."
        )
    except httpx.RequestError:
        raise HTTPException(
            status_code = status.HTTP_503_SERVICE_UNAVAILABLE,
            detail = "SERVICE UNAVAILABLE: An error occurred while trying to connect to the Oscar data source"
        )

    if not highlights:
        raise HTTPException(
            status_code = status.HTTP_404_NOT_FOUND,
            detail = f"NOT FOUND: No Oscar data found for year {year}."
        )
    return highlights
