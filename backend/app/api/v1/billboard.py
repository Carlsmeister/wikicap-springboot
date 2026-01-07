from fastapi import APIRouter
from app.services.artist_of_the_year import get_artist_of_the_year
from app.services.hit_song_year import get_year_with_hit_songs

router = APIRouter()

@router.get("/billboard/{year}")
def get_billboard_artists(year:int):
    return{
        "year": year,
        "top_artists": get_artist_of_the_year(year)
    }    
    
@router.get("/billboard/{year}/top-songs")
def get_billboard_top_songs(year: int, limit: int=5):
    return get_year_with_hit_songs(year, limit)    