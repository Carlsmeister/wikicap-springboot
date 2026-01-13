from fastapi import APIRouter, HTTPException, status
import httpx
from app.services.wiki_service import fetch_year_summary
from app.utils.validate_year import validate_year

router = APIRouter()



@router.get("/year/{year}/wiki", status_code=status.HTTP_200_OK)
async def get_year(year: int):
    validate_year(year)


    try:
        events = await fetch_year_summary(year)

    except httpx.HTTPStatusError as e:
        code = e.response.status_code
        if code == 404:
            raise HTTPException(
                status_code = status.HTTP_404_NOT_FOUND,
                detail = f"NOT FOUND: Wikipedia page for year {year} not found."
            )

        if code == 429:
            raise HTTPException(
                status_code = status.HTTP_429_TOO_MANY_REQUESTS,
                detail = "TOO MANY REQUESTS: Rate limit exceeded when accessing Wikipedia."
            )
        raise HTTPException(
            status_code = status.HTTP_502_BAD_GATEWAY,
            detail = f"BAD GATEWAY: Wikipedia returned {code}."
        )

    if not events:
        raise HTTPException(
            status_code = status.HTTP_404_NOT_FOUND,
            detail = f"NOT FOUND: No events found for year {year}."
        )

    return {
        "year": year,
        "events_by_month": events
    }
