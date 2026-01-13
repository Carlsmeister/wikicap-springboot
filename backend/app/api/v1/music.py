from fastapi import APIRouter, HTTPException
import httpx
from app.services.music_service import fetch_songs_for_year, fetch_artists_for_year
router = APIRouter()

@router.get("/year/{year}/artists") # Används inte ännu
def get_artists(year: int):

    return fetch_artists_for_year(year)


@router.get("/year/{year}/songs")
def get_songs(year: int):
    """
    Retrieve top songs for a specific year from Spotify.
    Args:
        year (int): The year to retrieve songs for (e.g., 2020).
    Returns:
        dict: If no error, returns a dictionary containing the year and a list of top songs.
        Else, raises HTTPException with appropriate status code and detail message.
    Raises:
        HTTPException: Raises appropriate HTTP exceptions for various error scenarios.
    """
    try:
        songs = fetch_songs_for_year(year)
        return songs
    
    except httpx.HTTPStatusError as error:
        status = error.response.status_code
        
        if status == 400:
            print("Spotify status:", error.response.status_code)
            print("Spotify body:", error.response.text)
            raise HTTPException(
                status_code=400,
                detail="Invalid year parameter."
            )

        if status == 404:
            raise HTTPException(
                status_code=404,
                detail=f"No songs found for year {year}."
            )

        if status == 429:
            raise HTTPException(
                status_code=429,
                detail="Rate limit exceeded when fetching songs from Spotify."
            )

        if status == 502:
            raise HTTPException(
                status_code=502,
                detail="Bad gateway error when accessing Spotify."
            )

        if status == 503:
            raise HTTPException(
                status_code=503,
                detail="Spotify service is currently unavailable. Please try again later."
            )
        
        raise HTTPException(
            status_code=500,
            detail="An unexpected error occurred while fetching songs.")
        