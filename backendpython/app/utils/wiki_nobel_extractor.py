
from bs4 import BeautifulSoup


def extract_nobel(html: str) -> dict:
    """
    Extract Nobel Prize laureates from the HTML of a Wikipedia Nobel Prize page.

    The function looks for headings matching standard Nobel categories and
    then parses the following wikitable to extract:
    - Laureate names
    - Motivations
    - Image URLs (if available)

    Args:
        html (str): The HTML string from the wikipedia API ("prop=text")

    Returns:
        A mapping of nobel category -> list of laureate dicts

        Example:
            {
            "Physics": [
                {"name": "Name", "motivation": "…", "image": "https://…"}
            ],
            "Peace": [...]
            }


    """

    soup = BeautifulSoup(html, "html.parser")
    prizes = {}

    for header in soup.find_all(['h2', 'h3']):
        title = header.get_text(strip = True)

        if title not in {
            "Physics",
            "Chemistry",
            "Physiology or Medicine",
            "Literature",
            "Peace",
            "Economic Sciences"
        }:
            continue

        table = header.find_next("table", class_ = "wikitable")
        if not table:
            continue

        laureates = []
        motivation = None

        for row in table.find_all("tr"):
            cells = row.find_all("td")
            if len(cells) < 2:
                continue

            if len(cells) >= 4:
                motivation = cells[3].get_text(" ",strip = True)

            name = cells[1].find("a")
            if not name:
                continue

            img_tag = cells[0].find("img")
            image_url = None
            if img_tag and img_tag.get("src"):
                image_url = img_tag["src"]
                if image_url.startswith("//"):
                    image_url = "https:" + image_url


            laureates.append({
                "name": name.get_text(strip = True),
                "motivation": motivation,
                "image": image_url
            })

        prizes[title] = laureates

    return prizes
