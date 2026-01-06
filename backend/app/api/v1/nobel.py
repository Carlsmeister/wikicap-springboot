from fastapi import APIRouter
from backend.app.services.nobel_service import get_nobel_prizes

router = APIRouter()

@router.get("/api/year/{year}/nobel")
def year_nobel(year: int):
    return get_nobel_prizes(year)
