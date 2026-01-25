package se.wikicap.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregated Wikipedia events response for a specific year.
 *
 * @param year the year the data belongs to
 * @param eventsByMonth events grouped by month name
 * @param source source description (e.g., "Wikipedia (MediaWiki API)")
 */
public record EventResponse(
        int year,
        Map<String, List<EventItem>> eventsByMonth,
        String source
) {

    /**
     * Single event entry.
     *
     * @param date textual date if present (e.g., "Jan 12"), empty when missing
     * @param description cleaned event description
     */
    public record EventItem(
            String date,
            String description
    ) {
    }
}
