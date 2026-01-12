"""
Movie and TV series service module.

This module provides business logic for fetching and processing movie and TV series
data from The Movie Database (TMDb) API. Includes custom ranking algorithms for
series and data normalization for consistent API responses.
"""

from app.clients.movie_client import get_top_movies_by_year
from app.clients.movie_client import get_top_series_by_year

async def fetch_movies_for_year(year: int):
    """
    Fetch and normalize top movies for a specific year.

    Retrieves movies from TMDb filtered by release year, minimum vote average (≥7),
    and minimum vote count (≥1000). Results are sorted by vote count to surface
    the most popular critically-acclaimed films. Returns up to 8 movies.

    Args:
        year (int): The release year to fetch movies for.

    Returns:
        dict: A dictionary containing:
            - year (int): The requested year
            - top_movies (list): List of up to 8 movie dictionaries with:
                - title (str): Movie title
                - rating (float): Vote average rounded to 1 decimal place
                - votes (int): Total vote count
                - poster (str): Poster path for TMDb image URL construction
                - release_date (str): Release date in YYYY-MM-DD format
            - source (str): Always "TMDb"

    Example:
        await fetch_movies_for_year(2020)
        {
            "year": 2020,
            "top_movies": [
                {
                    "title": "Tenet",
                    "rating": 7.5,
                    "votes": 5234,
                    "poster": "/path.jpg",
                    "release_date": "2020-08-26"
                }
            ],
            "source": "TMDb"
        }
    """
    raw = await get_top_movies_by_year(year)

    movies = []
    for item in raw.get("results", [])[:8]:
        movies.append({
            "title": item["title"],
            "rating": round(item["vote_average"], 1),
            "votes": item["vote_count"],
            "poster": item["poster_path"],
            "release_date": item["release_date"]
        })

    return {
        "year": year,
        "top_movies": movies,
        "source": "TMDb"
    }

async def fetch_series_for_year(year: int):
    """
    Fetch, rank, and normalize top TV series for a specific year.

    Retrieves TV series from TMDb that aired in the specified year with minimum
    vote average (≥7) and vote count (≥1000). Series are then ranked using a
    custom scoring algorithm that factors in popularity, rating, and an age
    penalty to prioritize shows relevant to the queried year. Returns up to 8 series.

    Args:
        year (int): The air year to fetch TV series for.

    Returns:
        dict: A dictionary containing:
            - year (int): The requested year
            - top_series (list): List of up to 8 TV series dictionaries with:
                - title (str): Series name
                - rating (float): Vote average rounded to 1 decimal place
                - votes (int): Total vote count
                - poster (str): Poster path for TMDb image URL construction
                - release_date (str): First air date in YYYY-MM-DD format
            - source (str): Always "TMDb"

    Note:
        The ranking algorithm applies an age penalty to older shows:
        - Shows ≤2 years old: No penalty (1.0x)
        - Shows 3-5 years old: 10% penalty (0.9x)
        - Shows 6-10 years old: 20% penalty (0.8x)
        - Shows 11-20 years old: 35% penalty (0.65x)
        - Shows >20 years old: 50% penalty (0.5x)

    Example:
        await fetch_series_for_year(2020)
        {
            "year": 2020,
            "top_series": [
                {
                    "title": "The Queen's Gambit",
                    "rating": 8.6,
                    "votes": 3421,
                    "poster": "/path.jpg",
                    "release_date": "2020-10-23"
                }
            ],
            "source": "TMDb"
        }
    """
    raw = await get_top_series_by_year(year)
    results = raw.get("results", [])

    ranked = sorted(
        results,
        key=lambda item: series_score(item, year),
        reverse=True,
    )

    series = []
    for item in ranked[:8]:
        series.append({
            "title": item["name"],
            "rating": round(item["vote_average"], 1),
            "votes": item["vote_count"],
            "poster": item["poster_path"],
            "release_date": item.get("first_air_date"),
        })

    return {
        "year": year,
        "top_series": series,
        "source": "TMDb",
    }

def series_score(item: dict, year: int) -> float:
    """
    Calculate a ranking score for a TV series.

    Combines popularity and rating metrics with an age-based penalty to create
    a score that prioritizes recent, relevant shows over long-running series
    that happen to have aired during the queried year.

    Args:
        item (dict): TMDb series object containing 'popularity', 'vote_average',
                     and 'first_air_date' fields.
        year (int): The year being queried, used to calculate the age penalty.

    Returns:
        float: Composite score where higher values indicate better rankings.
               Formula: (popularity * 0.6 + normalized_rating * 100 * 0.4) * age_penalty

    Example:
        >>> item = {"popularity": 150, "vote_average": 8.5, "first_air_date": "2020-10-23"}
        >>> series_score(item, 2020)
        124.0  # (150 * 0.6) + (0.85 * 100 * 0.4) * 1.0 = 124.0
    """
    popularity = item.get("popularity", 0)
    rating = item.get("vote_average", 0) / 10

    base = (popularity * 0.6) + (rating * 100 * 0.4)
    return base * age_penalty(item.get("first_air_date"), year)


def age_penalty(first_air_date: str | None, year: int) -> float:
    """
    Calculate an age-based penalty multiplier for TV series ranking.

    Applies progressive penalties to older shows to ensure that series relevant
    to the queried year are prioritized. Shows that premiered close to the
    queried year receive no penalty, while older shows receive increasing penalties.

    Args:
        first_air_date (str | None): First air date in 'YYYY-MM-DD' format.
                                      None or invalid dates receive no penalty.
        year (int): The year being queried, used as the reference point.

    Returns:
        float: Penalty multiplier between 0.5 and 1.0:
            - 1.0: Shows ≤2 years old (no penalty)
            - 0.9: Shows 3-5 years old
            - 0.8: Shows 6-10 years old
            - 0.65: Shows 11-20 years old
            - 0.5: Shows >20 years old

    Example:
        >>> age_penalty("2020-10-23", 2020)
        1.0  # New show, no penalty
        >>> age_penalty("2015-04-12", 2020)
        0.9  # 5 years old
        >>> age_penalty("2000-01-01", 2020)
        0.65  # 20 years old
    """
    if not first_air_date:
        return 1.0

    try:
        start_year = int(first_air_date[:4])
    except ValueError:
        return 1.0

    age = year - start_year

    if age <= 2:
        return 1.0
    if age <= 5:
        return 0.9
    if age <= 10:
        return 0.8
    if age <= 20:
        return 0.65
    return 0.5
