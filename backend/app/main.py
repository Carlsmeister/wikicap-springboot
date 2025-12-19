from dotenv import load_dotenv
import os
load_dotenv()

from fastapi import FastAPI
from app.api.v1.year import router as year_router
from app.api.v1.movies import router as movies_router

app = FastAPI()

app.include_router(year_router, prefix="/api/v1")
app.include_router(movies_router, prefix="/api/v1")

@app.get("/")
def read_root():
    return {
        "message": "WikiCap API is running!",
    }

