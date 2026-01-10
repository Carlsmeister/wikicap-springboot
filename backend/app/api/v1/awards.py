from fastapi import APIRouter, HTTPException, status
import httpx
from app.services.awards_service import fetch_oscar_highlights
from app.utils import validate_year


router = APIRouter()

@router.get("/year/{year}/movies")
def get_movies(year: int):
    validate_year(year)

    try:
        highlights = fetch_oscar_highlights(year)

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

