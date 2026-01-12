from fastapi import APIRouter, Response, HTTPException, status

from app.services.awards_service import fetch_oscar_highlights
from app.services.movie_service import fetch_movies_for_year, fetch_series_for_year
from app.services.wiki_service import fetch_year_summary
from app.services.artist_of_the_year import get_artist_of_the_year
from app.services.hit_song_year import get_year_with_hit_songs

router = APIRouter()

@router.get("/year/{year}")
async def get_year(year: int):
    events = await fetch_year_summary(year)
    return {
        "year": year,
        "events_by_month": events,
        "movie_highlights": fetch_oscar_highlights(year),
        "movies": fetch_movies_for_year(year),
        "series": fetch_series_for_year(year),
        "billboard_top_artists": get_artist_of_the_year(year),
        "billboard_artist_top_songs": get_year_with_hit_songs(year, limit=5)
        # music, events, sports osv senare...
        # music, events, sports osv senare...
    }
