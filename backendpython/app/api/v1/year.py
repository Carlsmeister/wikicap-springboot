"""
Year API endpoint module.

This module provides API endpoints for retrieving comprehensive year data,
including historical events, movies, TV series, music charts, and award information.
All data is fetched concurrently from multiple external APIs for optimal performance.
"""

from fastapi import APIRouter
import asyncio

from app.services.awards_service import fetch_oscar_highlights
from app.services.movie_service import fetch_movies_for_year, fetch_series_for_year
from app.services.wiki_service import fetch_year_summary
from app.services.artist_of_the_year import get_artist_of_the_year
from app.services.hit_song_year import get_year_with_hit_songs
from app.services.nobel_service import get_nobel_prizes
from app.services.music_service import fetch_songs_for_year

router = APIRouter()

@router.get("/year/{year}")
async def get_year(year: int):
    """
    Retrieve comprehensive data for a specific year.

    This endpoint aggregates data from multiple sources including Wikipedia,
    TMDb (The Movie Database), The Awards API, Last.fm, and Nobel Prize API.
    All API calls are executed concurrently using asyncio.gather() for optimal performance.

    Args:
        year (int): The year to retrieve data for (e.g., 2020).

    Returns:
        dict: A dictionary containing:
            - year (int): The requested year
            - events_by_month (dict): Monthly historical events from Wikipedia
            - movie_highlights (dict): Oscar winners (Best Picture, Actor, Actress)
            - movies (dict): Top-rated movies released in the year
            - series (dict): Top-rated TV series aired in the year
            - billboard_top_artists (dict): Billboard Hot 100 chart-topping artists
            - billboard_artist_top_songs (dict): Top songs for each artist
            - nobel_prizes (dict): Nobel Prize winners for the year

    Example:
        GET /api/v1/year/2020

        Response:
        {
            "year": 2020,
            "events_by_month": {...},
            "movie_highlights": {...},
            "movies": {...},
            "series": {...},
            "billboard_top_artists": {...},
            "billboard_artist_top_songs": {...},
            "nobel_prizes": {...}
        }

    Note:
        All external API calls run concurrently, so the total response time
        is approximately equal to the slowest API call rather than the sum
        of all calls.
    """
    # Run all API calls concurrently for maximum performance
    events, movie_highlights, movies, series, billboard_artists, billboard_songs, nobel = await asyncio.gather(
        fetch_year_summary(year),
        fetch_oscar_highlights(year),
        fetch_movies_for_year(year),
        fetch_series_for_year(year),
        get_artist_of_the_year(year),
        get_year_with_hit_songs(year),
        get_nobel_prizes(year)
    )

    return {
        "year": year,
        "events_by_month": events,
        "movie_highlights": movie_highlights,
        "movies": movies,
        "series": series,
        "billboard_top_artists": billboard_artists,
        "billboard_artist_top_songs": billboard_songs,
        "nobel_prizes": nobel,
        "spotify_songs": fetch_songs_for_year(year)
    }
