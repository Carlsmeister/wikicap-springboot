from fastapi import APIRouter
from app.services.artist_of_the_year import get_artist_of_the_year
from app.services.hit_song_year import get_year_with_hit_songs

router = APIRouter()

@router.get("/year/{year}/billboard/artist")
def get_billboard_artists(year:int):
    result = get_artist_of_the_year(year)
    print(result)
    print("Heejejjeje")
    return result
    
@router.get("/year/{year}/billboard/artist/top-songs")
def get_billboard_top_songs(year: int, limit: int=5):
    return get_year_with_hit_songs(year, limit)    