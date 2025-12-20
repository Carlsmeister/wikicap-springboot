from app.clients.movie_client import get_top_movies_by_year

def fetch_movies_for_year(year: int):
    raw = get_top_movies_by_year(year)

    movies = []
    for item in raw.get("results", [])[:10]:
        movies.append({
            "title": item["title"],
            "rating": round(item["vote_average"], 1),
            "poster": item["poster_path"],
            "releaseDate": item["release_date"]
        })

    return {
        "year": year,
        "topMovies": movies,
        "source": "TMDb"
    }
