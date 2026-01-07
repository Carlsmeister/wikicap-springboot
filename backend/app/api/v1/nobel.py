from fastapi import APIRouter
from app.services.nobel_service import get_nobel_prizes

router = APIRouter()

@router.get("/year/{year}/nobel")
def year_nobel(year: int):
    return get_nobel_prizes(year)
