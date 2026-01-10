from fastapi import APIRouter, HTTPException, status
import httpx
from app.services.nobel_service import get_nobel_prizes
from app.utils.validate_year import validate_year

router = APIRouter()

@router.get("/year/{year}/nobel")
def year_nobel(year: int):
    validate_year(year)

    try:
        nobel_prize = get_nobel_prizes(year)

    except HTTPException as e:
        code = e.status_code

        if code == 404:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"NOT FOUND: No Nobel Prize data found for year {year}."
            )
        if code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="TOO MANY REQUESTS: Rate limit exceeded when accessing Nobel Prize data."
            )

        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"BAD GATEWAY: Wikipedia returned {code}."
        )
    
    except httpx.RequestError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="SERVICE UNAVAILABLE: An error occurred while trying to connect to the Nobel Prize service."
        )



    if not nobel_prize:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"NOT FOUND: No Nobel Prize data found for year {year}."
        )

    return nobel_prize