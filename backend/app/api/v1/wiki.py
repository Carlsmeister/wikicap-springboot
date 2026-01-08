from fastapi import APIRouter
from app.services.wiki_service import fetch_year_summary

router = APIRouter()

@router.get("/year/{year}/wiki")
async def get_year(year: int):
    events = await fetch_year_summary(year)
    return {
        "year": year,
        "events_by_month": events,
    }
