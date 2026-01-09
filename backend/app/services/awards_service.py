from app.clients.awards_client import (
    get_oscar_edition_by_year,
    get_oscar_categories,
    get_oscar_category_details
)

OSCAR_CATEGORY_MAP = {
    "Best Picture": "bestPicture",
    "Actor In A Leading Role": "bestActor",
    "Actress In A Leading Role": "bestActress",
}

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
            oscars[key] = {
                "title": winner.get("name"),
            }
        else:
            oscars[key] = {
                "name": winner.get("name"),
                "movie": winner.get("more"),
            }

    return {
        "year": year,
        "oscars": oscars,
        "source": "The Awards API",
    }