import httpx

BASE_AWARDS_URL = "https://theawards.vercel.app/api"

def get_oscar_edition_by_year(year: int):
    """Fetch Oscar edition metadata for a specific year."""
    response = httpx.get(
        f"{BASE_AWARDS_URL}/oscars/editions",
        params={"year": year},
        headers={"accept": "application/json"},
    )
    response.raise_for_status()
    return response.json()


def get_oscar_categories(edition_id: int):
    """Fetch Oscar categories for a given edition id."""
    response = httpx.get(
        f"{BASE_AWARDS_URL}/oscars/editions/{edition_id}/categories",
        headers={"accept": "application/json"},
    )
    response.raise_for_status()
    return response.json()


def get_oscar_category_details(edition_id: int, category_id: int):
    """Fetch nominees for a specific Oscar category in an edition."""
    response = httpx.get(
        f"{BASE_AWARDS_URL}/oscars/editions/{edition_id}/categories/{category_id}/nominees",
        headers={"accept": "application/json"},
    )
    response.raise_for_status()
    return response.json()
