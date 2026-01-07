from fastapi import APIRouter
from app.services.wiki_service import fetch_year_summary

router = APIRouter()

@router.get("/year/{year}/wiki")
def get_year(year: int):
    return {
        "year": year,
        "events_by_month": fetch_year_summary(year)
    }
