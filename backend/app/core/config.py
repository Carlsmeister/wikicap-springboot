import os
from dotenv import load_dotenv
from pathlib import Path

env_path = Path(__file__).resolve().parents[2] / ".env"
load_dotenv(dotenv_path=env_path)
TMDB_API_KEY = os.getenv("TMDB_API_KEY")
BILLBOARD_100_API_KEY = os.getenv("BILLBOARD_100_API_KEY")

if not TMDB_API_KEY:
    raise RuntimeError("TMDB_API_KEY is missing in the environment")
