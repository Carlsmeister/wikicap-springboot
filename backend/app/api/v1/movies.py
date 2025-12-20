from fastapi import APIRouter
from app.services.movie_service import fetch_movies_for_year

router = APIRouter()

@router.get("/year/{year}/movies")
def get_movies(year: int):
    return fetch_movies_for_year(year)
