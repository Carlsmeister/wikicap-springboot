from fastapi import APIRouter, HTTPException, status
from app.services.artist_of_the_year import get_artist_of_the_year, add_artist_images
from app.clients.artist_img_client import fetch_wiki_image
from app.services.hit_song_year import get_year_with_hit_songs
from app.utils.validate_year import validate_year
import httpx


router = APIRouter()

@router.get("/year/{year}/billboard/artist/top-songs")
async def get_billboard_top_songs(year: int):
    """
    Retrives the top Billboard artists and their hit songs for a given year.
    
    This endpoint vaidates the provided year and handles common errors 
    and translates them into clear HTTP resonses for the se.wikicap.client.
    
    Parameters
    -----------
    year: int
        The specific year to fetcg Billboard artist and song data. Must be a valid year.
    
    Returns
    ---------
    dict:
        A dictionary containing Billboard data for the given year. 
        The structure includes an "artists" key with a list of artists and their associated hit songs.    

    Raises
    ------
    HTTPException:
        - 404 NOT FOUND: No Billboard data exsits for the given year.
        - 429 TOO MANY REQUESTS: The Billboard se.wikicap.service rate limit was exceeded.
        - 502 BAD GATEWAY: The Billboard se.wikicap.service returned an unexpected error.
        - 503 SERVICE UNAVAILABLE: A connection error occurred when contacting the Billboard se.wikicap.service.
    """
    validate_year(year)

    try:
        result = await get_year_with_hit_songs(year)
    except httpx.HTTPStatusError as e:
        code = e.response.status_code
        if code == 404:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"NOT FOUND: No Billboard data found for year {year}."
            )
        if code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="TOO MANY REQUESTS: Rate limit exceeded when accessing Billboard data."
            )

        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"BAD GATEWAY: Billboard se.wikicap.service returned {code}."
        )
    except httpx.RequestError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="SERVICE UNAVAILABLE: An error occurred while trying to connect to the Billboard se.wikicap.service."
        )

    if not result or not result.get("artists"):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"NOT FOUND: No Billboard data found for year {year}."
        )

    return result

@router.get("/year/{year}/billboard/artist")
async def get_billboard_artists(year:int):
    """
    Retrieve Billboard's top artists for a given year.
    
    This endpoint validates the provided year and calls an external 
    Billboard data se.wikicap.service to fetch the most popular artists for that year.
    It also fetches the response with artist images sourced from Wikipedia.
    
    Common errors are caught and translated into clear HTTP responses. 
    
    Parameters
    ----------
    year: int
        The specific year to fetch Billboard artist data for, ust be a valid year.
    
    Returns
    -------
    dict:
        A dicitionary containing Billboard artist data for the given year. 
        The structure includes an "artists" key with a list of artists and image URLs.  
        
    Raises
    ------
    HTTPException:
        - 404 NOT FOUND: No Billboard data exsits for the given year.
        - 429 TOO MANY REQUESTS: The Billboard se.wikicap.service rate limit was exceeded.
        - 502 BAD GATEWAY: The BIllboard se.wikicap.service returned an unexpected error.
        - 503 SERVICE UNAVAILABLE:  A conection error occurred when contacting the Billboard se.wikicap.service.
    """
    validate_year(year)

    try:
        base = await get_artist_of_the_year(year)
    except httpx.HTTPStatusError as e:
        code = e.response.status_code
        if code == 404:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"NOT FOUND: No Billboard data found for year {year}."
            )
        if code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="TOO MANY REQUESTS: Rate limit exceeded when accessing Billboard data."
            )

        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"BAD GATEWAY: Billboard se.wikicap.service returned {code}."
        )
    except httpx.RequestError:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="SERVICE UNAVAILABLE: An error occurred while trying to connect to the Billboard se.wikicap.service."
        )

    if not base or not base.get("artists"):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"NOT FOUND: No Billboard data found for year {year}."
        )

    return add_artist_images(base, fetch_wiki_image)

