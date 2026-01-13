"""Application configuration loader for external API credentials.

Loads environment variables from the project .env file (two directories up) and
raises clear errors when required keys are missing. Exposes TMDB, LastFM, and
Spotify credentials as module-level constants for import by clients/services.
"""

import os
from dotenv import load_dotenv
from pathlib import Path

env_path = Path(__file__).resolve().parents[2] / ".env"
load_dotenv(dotenv_path=env_path)
TMDB_API_KEY = os.getenv("TMDB_API_KEY")
LASTFM_API_KEY=os.getenv("LASTFM_API_KEY")
SPOTIFY_CLIENT_ID=os.getenv("SPOTIFY_CLIENT_ID")
SPOTIFY_CLIENT_SECRET=os.getenv("SPOTIFY_CLIENT_SECRET")

if not TMDB_API_KEY:
    raise RuntimeError("TMDB_API_KEY is missing in the environment")

if not LASTFM_API_KEY:
    raise RuntimeError("LASTFM_API_KEY is missing in the environment")

if not SPOTIFY_CLIENT_ID:
    raise RuntimeError("SPOTIFY_CLIENT_ID is missing in the environment")

if not SPOTIFY_CLIENT_SECRET:
    raise RuntimeError("SPOTIFY_CLIENT_SECRET is missing in the environment")
