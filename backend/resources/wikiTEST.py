from wiki_service import fetch_year_summary, fetch_year_events

def fetch_year_summary(year: int):
    summary = fetch_year_summary(year)
    print(f"Year Summary for {year}:\n{summary}\n")


if __name__ == "__main__":
    data = fetch_year_events(1997)

    for month, events in data.items():
        print(f"\n{month}")
        for e in events:
            print(" -", e)
