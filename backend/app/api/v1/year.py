from fastapi import APIRouter, Response, HTTPException, status

from backend.app.services.awards_service import fetch_oscar_highlights
from backend.app.services.movie_service import fetch_movies_for_year
from backend.app.services.movie_service import fetch_series_for_year
from backend.app.services.wiki_service import fetch_year_events

# from app.services.music_serivce import fetch_music_for_year
# from app.services.sport_serivce import fetch_sports_for_year

router = APIRouter()

@router.get("/year/{year}")
def get_year(year: int):
    if year < 1800 or year > 2026:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Year must be between 1800 and 2026"
        )
    try:
        return {
            "year": year,
            "events_by_month": fetch_year_events(year),
            "movie_highlights": fetch_oscar_highlights(year),
            "movies": fetch_movies_for_year(year),
            "series": fetch_series_for_year(year)
            # music, events, sports osv senare...
        }

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Error fetching data for year {year}: {str(e)}"
            
        )

