from fastapi import APIRouter, HTTPException, status
from backend.app.services.wiki_service import fetch_year_events

router = APIRouter()

@router.get("/year/{year}/events")
def get_year_events(year: int):
    if year < 1800 or year > 2026:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Year must be between 1800 and 2026"
        )
    try:
        events = fetch_year_events(year)

        if not events:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"No events found for year {year}"
            )

        return {
            "year": year,
            "source": "wikipedia",
            "events_by_month": events
        }
    except HTTPException:
        raise

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Error fetching events for year {year}: {str(e)}"
        )



