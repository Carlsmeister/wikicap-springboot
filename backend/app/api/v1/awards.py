from fastapi import APIRouter
from backend.app.services.awards_service import fetch_oscar_highlights

router = APIRouter()

@router.get("/year/{year}/movies")
def get_movies(year: int):
    return fetch_oscar_highlights(year)