from fastapi import APIRouter, Response

from backend.app.services.awards_service import fetch_oscar_highlights
from backend.app.services.movie_service import fetch_movies_for_year
from backend.app.services.movie_service import fetch_series_for_year
from backend.app.services.wiki_service import fetch_year_summary

# from app.services.music_serivce import fetch_music_for_year
# from app.services.sport_serivce import fetch_sports_for_year

router = APIRouter()

@router.get("/year/{year}")
def get_year(year: int):
    return {
        "year": year,
        "events_by_month": fetch_year_summary(year),
        "movie_highlights": fetch_oscar_highlights(year),
        "movies": fetch_movies_for_year(year),
        "series": fetch_series_for_year(year)
        # music, events, sports osv senare...
    }
