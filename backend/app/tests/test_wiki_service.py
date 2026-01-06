"""
Test file for wiki service functionality.
Updated to use the refactored structure.
"""
from app.services.wiki_service import fetch_year_events


def test_fetch_year_events(year: int = 1997):
    """
    Test the fetch_year_events function with a specific year.
    """
    print(f"Fetching events for year {year}...\n")

    data = fetch_year_events(year)

    if not data:
        print(f"No events found for {year}")
        return

    print(f"Successfully fetched events for {year}\n")
    print("=" * 80)

    for month, events in data.items():
        print(f"\n{month}")
        print("-" * 40)
        for event in events:
            print(f"  • {event}")

    print("\n" + "=" * 80)
    print(f"Total months with events: {len(data)}")
    total_events = sum(len(events) for events in data.values())
    print(f"Total events extracted: {total_events}")


if __name__ == "__main__":
    # Test with default year
    test_fetch_year_events(1997)

    # Optionally test with other years
    # test_fetch_year_events(2020)

