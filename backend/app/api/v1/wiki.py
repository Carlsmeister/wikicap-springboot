from fastapi import APIRouter, HTTPException, status
import httpx
from app.services.wiki_service import fetch_year_summary

router = APIRouter()

@router.get("/year/{year}/wiki", status_code=status.HTTP_200_OK)
def get_year(year: int):
    if year <1800 or year > 2027:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail = "BAD REQUEST: Year must be between 1800 and 2027"
        )
    elif status := 404:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail = f"NOT FOUND: No data found for year {year}"
            )

    try:
        fetch_year_summary(year)

    except httpx.HTTPStatusError as e:
        raise HTTPException(
            status_code=e.response.status_code,
            detail = f"INTERNAL SERVER ERROR: failed to fetch data for year {year}: {str(e)}"
        )

    except httpx.RequestError as e:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail = "Wiki"
        )
    return {
        "year": year,
        "events_by_month": fetch_year_summary(year)
    }
