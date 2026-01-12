from app.clients.awards_client import (
    get_oscar_edition_by_year,
    get_oscar_categories,
    get_oscar_category_details
)
from app.clients.movie_client import search_movie_by_title, search_person_by_name
import re

OSCAR_CATEGORY_MAP = {
    "Best Picture": "bestPicture",
    "Actor In A Leading Role": "bestActor",
    "Actress In A Leading Role": "bestActress",
}
# Extract movie title and character name from "more" field
def extract_movie_title(more_field: str) -> str:
    if not more_field:
        return ""
    # Remove character name in curly braces
    title = re.sub(r'\s*\{.*?\}\s*', '', more_field)
    return title.strip()


def fetch_oscar_highlights(year: int):
    editions = get_oscar_edition_by_year(year)
    if not editions:
        return None

    edition_id = editions[0]["id"]
    categories = get_oscar_categories(edition_id)

    oscars = {}

    for category in categories:
        category_name = category.get("name")

        if category_name not in OSCAR_CATEGORY_MAP:
            continue

        key = OSCAR_CATEGORY_MAP[category_name]
        nominees = get_oscar_category_details(edition_id, category["id"])

        winner = None

        for nominee in nominees:
            if nominee.get("winner") is True:
                winner = nominee
                break

        if not winner:
            continue

        if key == "bestPicture":
            movie_title = winner.get("name")
            movie_data = search_movie_by_title(movie_title, year)

            oscars[key] = {
                "title": movie_title,
                "poster": movie_data.get("poster_path") if movie_data else None,
            }
        else:
            person_name = winner.get("name")
            movie_title = extract_movie_title(winner.get("more", ""))
            person_data = search_person_by_name(person_name)

            oscars[key] = {
                "name": person_name,
                "movie": movie_title,
                "image": person_data.get("profile_path") if person_data else None,
            }

    return {
        "year": year,
        "oscars": oscars,
        "source": "The Awards API",
    }

