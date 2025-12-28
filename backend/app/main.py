import base64
from dotenv import load_dotenv
import os
from requests import post

load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.v1.year import router as year_router
from app.api.v1.movies import router as movies_router
from app.api.v1.music import router as music_router

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
app.include_router(music_router, prefix="/api/v1")

@app.get("/")
def read_root():
    return {
        "message": "WikiCap API is running!",
    }
