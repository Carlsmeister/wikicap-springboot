import re
from bs4 import BeautifulSoup
from app.clients.billboard_artist_client import get_billboard_page
from app.clients.artist_img_client import fetch_wiki_image
from typing import Callable


def find_artist_column(table_data: list[str]) -> int| None:
    """
    Identify the column in a table header that corresponds to artist information.

    The function iterates through a list of header strings,
    and returns the index of the first one containing the word ``"artist"``. 
    If no matching header is found, None is returned.

    Parameters
    ----------
        table_data:  list[str]
            A list of column header strings.

    Returns
    -------
        int or None: 
            The index of the column containing ``"artist"``, or None if
        no matching column is found.
    """
    for column_index, header in enumerate(table_data):
        table_head = header.lower()
        if "artist" in table_head:
            return column_index 
    return None    
        
  
async def get_artist_of_the_year(year: int) -> dict:
    """
    Extracts artist names from the Billboard Hot 100 Wikipedia Page for a given year.
    
    The asynchronous function fetches the relevant Wikipedia page, locates the first table with an ``"Artist"`` column, 
    and extracts all artist names from that column. Duplicates are removed while keeping the original order.
    The function always returns a dictionary containing the year and a list of extracted artist names.
    
    Parameters
    ----------
        year: int
            The year for which to retrieve artist information.

    Returns
    --------
        dict: 
            A dictionary containing the year, a list of extracted artist names.
            If no suitable table or artist column is found, the ``"artists"`` list will be empty.
    """
    html = await get_billboard_page(year)

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
            "artists": []
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
    artists_seen = set()
    for artist in extracted_artists:
        if artist not in artists_seen:
            unique_artists.append(artist)
            artists_seen.add(artist)

    return {
        "year": year, 
        "artists": unique_artists
    }
    
    
def add_artist_images( year_data: dict, fetch_image: Callable[[str], str | None]) -> dict: 
    """
    Add image URLs to each artist in the provided year-data. 
    
    This function iterates through the list of artists in ``"year_data"`` and uses the 
    ``"fetch_image"`` callable to look up an image URL for each artist. 
    It also has a simple cache in-memory to avoid fetching the same artist image multiple times. 
    
    Parameters
    ---------- 
        year_data: dict
            A dicitionary that includes an ``"artist_list"``.
        fetch_image: Callable[[str], str| None]
            Returns an image URL for an artist or None.
             
    Returns
    -------
        dict: 
            A copy of ``"year_data"`` with an added ``"artists_with_images"`` key.
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
