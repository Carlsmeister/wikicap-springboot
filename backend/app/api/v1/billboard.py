from fastapi import APIRouter
from app.services.artist_of_the_year import get_artist_of_the_year, add_artist_images
from app.clients.artist_img_client import fetch_wiki_image
from app.services.hit_song_year import get_year_with_hit_songs


router = APIRouter()

@router.get("/year/{year}/billboard/artist")
def get_billboard_artists(year:int):
    base = get_artist_of_the_year(year)
    
    if isinstance(base, dict) and base.get("error"):
        return base
    return add_artist_images(base, fetch_wiki_image)
    
@router.get("/year/{year}/billboard/artist/top-songs")
def get_billboard_top_songs(year: int, limit: int=5):
    result = get_year_with_hit_songs(year, limit) 
    return result
   