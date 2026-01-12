"""
The Awards API client module.

This module provides async HTTP client functions for interacting with The Awards API
to retrieve Oscar (Academy Awards) data including editions, categories, and nominees.

API Documentation: https://theawards.vercel.app/api
"""

import httpx

BASE_AWARDS_URL = "https://theawards.vercel.app/api"

async def get_oscar_edition_by_year(year: int):
    """
    Fetch Oscar edition metadata for a specific ceremony year.

    Retrieves information about the Academy Awards ceremony that took place
    in the specified year, including the edition ID needed for subsequent queries.

    Args:
        year (int): The year of the Oscar ceremony (e.g., 2020 for the 92nd Academy Awards).

    Returns:
        list[dict]: List of Oscar edition objects (typically one per year) containing:
            - id (int): Edition ID for use in subsequent API calls
            - year (int): The ceremony year
            - date (str): Ceremony date
            - host (str): Ceremony host(s)
            - venue (str): Ceremony location

    Raises:
        httpx.HTTPStatusError: If the API returns a non-2xx status code
        httpx.RequestError: If the request fails due to network issues

    Example:
        await get_oscar_edition_by_year(2020)
        [{"id": 92, "year": 2020, "date": "2020-02-09", ...}]
    """
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{BASE_AWARDS_URL}/oscars/editions",
            params={"year": year},
            headers={"accept": "application/json"},
        )
        response.raise_for_status()
        return response.json()


async def get_oscar_categories(edition_id: int):
    """
    Fetch all award categories for a specific Oscar edition.

    Retrieves the list of categories (e.g., Best Picture, Best Actor) available
    in a particular Academy Awards ceremony.

    Args:
        edition_id (int): The Oscar edition ID from get_oscar_edition_by_year.

    Returns:
        list[dict]: List of category objects containing:
            - id (int): Category ID for use in get_oscar_category_details
            - name (str): Category name (e.g., "Best Picture", "Actor In A Leading Role")
            - description (str): Category description

    Raises:
        httpx.HTTPStatusError: If the API returns a non-2xx status code
        httpx.RequestError: If the request fails due to network issues

    Example:
        await get_oscar_categories(92)
        [
            {"id": 1, "name": "Best Picture", ...},
            {"id": 2, "name": "Actor In A Leading Role", ...}
        ]
    """
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{BASE_AWARDS_URL}/oscars/editions/{edition_id}/categories",
            headers={"accept": "application/json"},
        )
        response.raise_for_status()
        return response.json()


async def get_oscar_category_details(edition_id: int, category_id: int):
    """
    Fetch nominees and winner for a specific Oscar category in an edition.

    Retrieves detailed information about all nominees in a particular category
    for a specific ceremony, including which nominee won the award.

    Args:
        edition_id (int): The Oscar edition ID from get_oscar_edition_by_year.
        category_id (int): The category ID from get_oscar_categories.

    Returns:
        list[dict]: List of nominee objects containing:
            - id (int): Nominee ID
            - name (str): Nominee name (person or film title)
            - winner (bool): True if this nominee won the award
            - more (str): Additional information (e.g., movie title for actor nominations)

    Raises:
        httpx.HTTPStatusError: If the API returns a non-2xx status code
        httpx.RequestError: If the request fails due to network issues

    Example:
        await get_oscar_category_details(92, 2)
        [
            {"id": 123, "name": "Joaquin Phoenix", "winner": True, "more": "Joker {Arthur Fleck}"},
            {"id": 124, "name": "Leonardo DiCaprio", "winner": False, "more": "Once Upon a Time..."}
        ]
    """
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{BASE_AWARDS_URL}/oscars/editions/{edition_id}/categories/{category_id}/nominees",
            headers={"accept": "application/json"},
        )
        response.raise_for_status()
        return response.json()
