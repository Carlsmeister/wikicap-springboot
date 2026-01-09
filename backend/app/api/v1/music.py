from fastapi import APIRouter
from app.services.music_service import fetch_songs_for_year, fetch_artists_for_year
router = APIRouter()

@router.get("/year/{year}/artists")
def get_artists(year: int):
    return fetch_artists_for_year(year)


@router.get("/year/{year}/top-songs")
def get_songs(year: int):
    return fetch_songs_for_year(year)