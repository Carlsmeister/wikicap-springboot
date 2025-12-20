from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from backend.resources.wiki_service import fetch_year_events


app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/api/year/{year}")
def get_year(year: int):
    return {
        "year": year,
        "events_by_month": fetch_year_events(year)
    }

