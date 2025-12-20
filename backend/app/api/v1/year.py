from fastapi import APIRouter, Response
router = APIRouter()

# from app.services.music_serivce import fetch_music_for_year
# from app.services.sport_serivce import fetch_sports_for_year
# from app.services.event_serivce import fetch_events_for_year

from app.services.movie_service import fetch_movies_for_year

@router.get("/year/{year}")
def get_year(year: int):
    return {
        "year": year,
        "movies": fetch_movies_for_year(year),
        # music, events, sports osv senare...
    }
