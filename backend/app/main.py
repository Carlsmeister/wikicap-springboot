from dotenv import load_dotenv
import os
load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.v1.year import router as year_router
from app.api.v1.movies import router as movies_router

from resources.wiki_service import fetch_year_events

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(year_router, prefix="/api/v1")
app.include_router(movies_router, prefix="/api/v1")

@app.get("/")
def read_root():
    return {
        "message": "WikiCap API is running!",
    }

@app.get("/api/year/{year}")
def get_year(year: int):
    return {
        "year": year,
        "events_by_month": fetch_year_events(year)
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="8000", port=8000)

