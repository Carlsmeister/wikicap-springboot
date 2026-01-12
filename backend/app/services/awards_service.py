"""
Awards service module.

This module provides business logic for fetching and processing Oscar (Academy Awards)
data from The Awards API, enriched with images from The Movie Database (TMDb).
Focuses on the three major categories: Best Picture, Best Actor, and Best Actress.
"""

from app.clients.awards_client import (
    get_oscar_edition_by_year,
    get_oscar_categories,
    get_oscar_category_details
)
from app.clients.movie_client import search_movie_by_title, search_person_by_name
import asyncio
import re

OSCAR_CATEGORY_MAP = {
    "Best Picture": "bestPicture",
    "Actor In A Leading Role": "bestActor",
    "Actress In A Leading Role": "bestActress",
}

def extract_movie_title(more_field: str) -> str:
    """
    Extract clean movie title from The Awards API 'more' field.

    The Awards API sometimes includes character names in curly braces within
    the 'more' field (e.g., "Joker {Arthur Fleck}"). This function strips
    those annotations to return a clean movie title.

    Args:
        more_field (str): The 'more' field from The Awards API nominee data.

    Returns:
        str: Clean movie title with character annotations removed.

    Example:
        extract_movie_title("Joker {Arthur Fleck}")
        -->    "Joker"
        extract_movie_title("The Irishman")
        -->    "The Irishman"
        extract_movie_title("")
        -->    ""
    """
    if not more_field:
        return ""
    # Remove character name in curly braces
    title = re.sub(r'\s*\{.*?\}\s*', '', more_field)
    return title.strip()


async def fetch_oscar_highlights(year: int):
    """
    Fetch Oscar winners for major categories and enrich with TMDb images.

    Retrieves the Oscar edition for the specified year from The Awards API,
    then fetches winners for Best Picture, Best Actor (Leading Role), and
    Best Actress (Leading Role). Movie posters and actor profile images are
    retrieved from TMDb to enhance the response data.

    Args:
        year (int): The Oscar ceremony year (e.g., 2020 for the 92nd Academy Awards).

    Returns:
        dict | None: A dictionary containing:
            - year (int): The requested year
            - oscars (dict): Dictionary with up to three keys:
                - bestPicture (dict, optional):
                    - title (str): Winning movie title
                    - poster (str | None): TMDb poster path
                - bestActor (dict, optional):
                    - name (str): Winning actor's name
                    - movie (str): Movie they won for
                    - image (str | None): TMDb profile image path
                - bestActress (dict, optional):
                    - name (str): Winning actress's name
                    - movie (str): Movie they won for
                    - image (str | None): TMDb profile image path
            - source (str): Always "The Awards API"

        Returns None if no Oscar edition found for the year.

    Note:
        - Not all categories may be present if no winner is found
        - Image paths may be None if TMDb lookup fails
        - Categories are fetched concurrently but processed sequentially
          to maintain proper error handling for each TMDb lookup

    Example:
        await fetch_oscar_highlights(2020)
        {
            "year": 2020,
            "oscars": {
                "bestPicture": {"title": "Parasite", "poster": "/path.jpg"},
                "bestActor": {"name": "Joaquin Phoenix", "movie": "Joker", "image": "/path.jpg"},
                "bestActress": {"name": "Renée Zellweger", "movie": "Judy", "image": "/path.jpg"}
            },
            "source": "The Awards API"
        }
    """
    editions = await get_oscar_edition_by_year(year)
    if not editions:
        return None

    edition_id = editions[0]["id"]
    categories = await get_oscar_categories(edition_id)

    relevant_categories = [
        cat for cat in categories
        if cat.get("name") in OSCAR_CATEGORY_MAP
    ]

    category_details = await asyncio.gather(
        *[get_oscar_category_details(edition_id, cat["id"]) for cat in relevant_categories],
        return_exceptions=True
    )

    oscars = {}

    async def process_category(category, nominees):
        if isinstance(nominees, Exception):
            return None

        category_name = category.get("name")
        key = OSCAR_CATEGORY_MAP[category_name]

        winner = None
        for nominee in nominees:
            if nominee.get("winner") is True:
                winner = nominee
                break

        if not winner:
            return None

        if key == "bestPicture":
            movie_title = winner.get("name")
            movie_data = await search_movie_by_title(movie_title, year)
            return (key, {
                "title": movie_title,
                "poster": movie_data.get("poster_path") if movie_data else None,
            })
        else:
            person_name = winner.get("name")
            movie_title = extract_movie_title(winner.get("more", ""))
            person_data = await search_person_by_name(person_name)
            return (key, {
                "name": person_name,
                "movie": movie_title,
                "image": person_data.get("profile_path") if person_data else None,
            })

    results = await asyncio.gather(
        *[process_category(cat, details) for cat, details in zip(relevant_categories, category_details)],
        return_exceptions=True
    )

    for result in results:
        if result and not isinstance(result, Exception):
            key, data = result
            oscars[key] = data

    return {
        "year": year,
        "oscars": oscars,
        "source": "The Awards API",
    }
