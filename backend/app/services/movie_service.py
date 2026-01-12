from app.clients.movie_client import get_top_movies_by_year
from app.clients.movie_client import get_top_series_by_year

def fetch_movies_for_year(year: int):
    raw = get_top_movies_by_year(year)

    movies = []
    for item in raw.get("results", [])[:8]:
        movies.append({
            "title": item["title"],
            "rating": round(item["vote_average"], 1),
            "votes": item["vote_count"],
            "poster": item["poster_path"],
            "releaseDate": item["release_date"]
        })

    return {
        "year": year,
        "topMovies": movies,
        "source": "TMDb"
    }

def fetch_series_for_year(year: int):
    raw = get_top_series_by_year(year)
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
            "releaseDate": item.get("first_air_date"),
        })

    return {
        "year": year,
        "topSeries": series,
        "source": "TMDb",
    }

def series_score(item: dict, year: int) -> float:
    popularity = item.get("popularity", 0)
    rating = item.get("vote_average", 0) / 10

    base = (popularity * 0.6) + (rating * 100 * 0.4)
    return base * age_penalty(item.get("first_air_date"), year)


def age_penalty(first_air_date: str | None, year: int) -> float:
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
