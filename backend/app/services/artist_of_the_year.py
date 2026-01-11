import re
from bs4 import BeautifulSoup
from app.clients.billboard_artist_client import get_billboard_page
from app.clients.artist_img_client import fetch_wiki_image
from typing import Callable


def find_artist_column(table_data: list[str]) -> int| None:
    """
    Identify the column in a table header that corresponds to artist information.

    The function iterates through a list of header strings,
    and returns the index of the first one containing the word "artist". 
    If no matching header is found, None is returned.

    Args:
        table_data (list[str]): A list of column header strings.

    Returns:
        int or None: The index of the column containing "artist", or None if
        no matching column is found.
    """
    for column_index, header in enumerate(table_data):
        table_head = header.lower()
        if "artist" in table_head:
            return column_index 
    return None    
        
  
def get_artist_of_the_year(year: int) -> dict:
    """
    Extracts artist names from the Billboard Hot 100 Wikipedi Page for a given year.
    
    The function fetches the page, finds the first table with an "Artist" column, 
    and extracts all artist names from that column removing duplicates while keeping the original order.
    The function always returns a dictionary.
    
    Args:
        year (int): The year for which to retrieve artist information.

    Returns:
        dict: A dictionary containing the year, a list of artist names,
        and optionally an error message if parsing fails.
    """
    html = get_billboard_page(year)

    # Fel: kunde inte hämta html eller html var tom
    if html is None or html.strip() == "":
        return {
            "year": year, 
            "artists": [], 
            "error": "Could not fetch Billboard page"
        }

    soup = BeautifulSoup(html, "html.parser")

    # Wikipedia har ofta flera wikitable — vi letar efter en tabell vars header innehåller "Artist"
    all_tables = soup.select("table.wikitable")

    selected_table = None
    artist_column_index = None

    for table in all_tables:
        header_row = table.find("tr")
        if header_row is None:
            continue

        header_cells = header_row.find_all(["th", "td"])
        header_texts = [cell.get_text(" ", strip=True) for cell in header_cells]

        artist_column = find_artist_column(header_texts)
        if artist_column is not None:
            selected_table = table
            artist_column_index = artist_column
            break

    if selected_table is None or artist_column_index is None:
        return {
            "year": year, 
            "artists": [], 
            "error": "Could not find a table with an Artist column"
        }

    # Plocka ut artister från rätt kolumn
    extracted_artists: list[str] = []

    data_rows = selected_table.find_all("tr")[1:]  # hoppa över header
    for row in data_rows:
        cells = row.find_all(["th", "td"])
        if len(cells) <= artist_column_index:
            continue

        raw_artist = cells[artist_column_index].get_text(" ", strip=True)
        cleaned_artist = re.sub(r"\[.*?\]", "", raw_artist).strip()  # tar bort [1], [a] osv

        if cleaned_artist:
            extracted_artists.append(cleaned_artist)

    # Ta bort dubbletter men behåll ordning
    unique_artists: list[str] = []
    seen = set()
    for artist in extracted_artists:
        if artist not in seen:
            unique_artists.append(artist)
            seen.add(artist)

    return {
        "year": year, 
        "artists": unique_artists
    }
    
    
def add_artist_images( year_data: dict, fetch_image: Callable[[str], str | None]) -> dict: 
    """
    Add image URLs to each artist in the year-data dictionary. 
    
    This function looks up an image for every artist using the `fetch_image`function. 
    It also has a simple cache in-memory to avoid fetching the same artist image multiple times. 
    
    Args: 
        year_data (dict):
            A dicitionary that includes an "artist_list".
        fetch_image(Callable[[str], str| None]):
            Returns an image URL for an artist or None.
             
    Returns:
        dict: 
            A copy of `year_data` with an added "artists_with_images" list.
    """
    artist_list = year_data.get("artists", []) 
    
    image_cache = {}
    
    artists_with_images = []
    
    for artist_name in artist_list: 
        if artist_name not in image_cache:
           try:
               image_cache[artist_name] = fetch_image(artist_name)
           except Exception:
               image_cache[artist_name] = None
               
        artists_with_images.append({
                "name": artist_name,
                "image": image_cache[artist_name]
        })  
        
    result = dict(year_data)
    result["artists_with_images"] = artists_with_images
    
    return result

      
    
    