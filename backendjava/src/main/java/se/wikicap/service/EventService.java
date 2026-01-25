package se.wikicap.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.wikicap.client.EventClient;
import se.wikicap.dto.EventResponse;
import se.wikicap.util.WikiEventCleaner;

import java.time.Duration;
import java.util.*;

@Service
public class EventService {

    private static final List<String> MONTHS = List.of(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    );
    private static final Set<String> MONTH_SET = new HashSet<>(MONTHS);

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LINE_LEN = 200;
    private static final int CONCURRENCY = 4;

    private final EventClient eventClient;

    public EventService(EventClient eventClient) {
        this.eventClient = eventClient;
    }

    /**
     * Fetch historical events for a specific year using the MediaWiki API.
     * @param year The year to fetch events for
     * @return Mono<EventResponse> containing events grouped by month
     */
    public Mono<EventResponse> getEventsByYear(int year) {
        return eventClient.fetchYearToc(year)
                .map(this::locateMonthSections)
                .flatMap(months -> fetchMonthEvents(year, months))
                .map(eventsByMonth -> new EventResponse(
                        year,
                        eventsByMonth,
                        "Wikipedia (MediaWiki API)"
                ))
                .timeout(Duration.ofSeconds(12))
                .onErrorReturn(new EventResponse(year, Map.of(), "Wikipedia (MediaWiki API)"));
    }

    /**
     * Locate month sections by scanning Table of content and focusing on the "Events" section when possible.
     * @param tocItems List of TOC items from the year page
     * @return Map of month names to their corresponding section indices
     */
    private Map<String, String> locateMonthSections(List<EventClient.TocItem> tocItems) {
        Map<String, String> months = new LinkedHashMap<>();

        if (tocItems == null || tocItems.isEmpty()) {
            return months;
        }

        int eventsIndex = -1;
        Integer eventsLevel = null;

        for (int i = 0; i < tocItems.size(); i++) {
            EventClient.TocItem item = tocItems.get(i);
            String line = item == null ? "" : safe(item.line());
            if (line.equalsIgnoreCase("Events") || line.toLowerCase(Locale.ROOT).startsWith("events")) {
                eventsIndex = i;
                eventsLevel = item.level();
                break;
            }
        }

        if (eventsIndex >= 0 && eventsLevel != null) {
            for (int i = eventsIndex + 1; i < tocItems.size(); i++) {
                EventClient.TocItem item = tocItems.get(i);
                if (item == null) continue;

                Integer level = item.level();
                if (level != null && level <= eventsLevel) {
                    break;
                }

                String line = safe(item.line());
                if (MONTH_SET.contains(line)) {
                    months.put(line, safe(item.index()));
                }
            }
        }

        if (months.isEmpty()) {
            for (EventClient.TocItem item : tocItems) {
                if (item == null) continue;
                String line = safe(item.line());
                if (MONTH_SET.contains(line)) {
                    months.put(line, safe(item.index()));
                }
            }
        }

        return months;
    }

    /**
     * Fetch events for each month in parallel and aggregate results.
     * @param year The year to fetch events for.
     * @param months Map of month names to section indices.
     * @return Mono of aggregated events by month.
     */
    private Mono<Map<String, List<EventResponse.EventItem>>> fetchMonthEvents(int year, Map<String, String> months) {
        if (months == null || months.isEmpty()) {
            return Mono.just(Map.of());
        }

        return Flux.fromIterable(months.entrySet())
                .flatMap(entry -> eventClient.fetchSectionWikitext(year, entry.getValue())
                        .map(wikitext -> Map.entry(entry.getKey(), extractMonthEvents(wikitext, entry.getKey()))),
                        CONCURRENCY)
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .collectList()
                .map(entries -> {
                    Map<String, List<EventResponse.EventItem>> byMonth = new LinkedHashMap<>();
                    for (Map.Entry<String, List<EventResponse.EventItem>> entry : entries) {
                        byMonth.put(entry.getKey(), entry.getValue());
                    }

                    Map<String, List<EventResponse.EventItem>> ordered = new LinkedHashMap<>();
                    for (String month : MONTHS) {
                        List<EventResponse.EventItem> monthEvents = byMonth.get(month);
                        if (monthEvents != null && !monthEvents.isEmpty()) {
                            ordered.put(month, monthEvents);
                        }
                    }
                    return ordered;
                });
    }

    /**
     * Extract individual events from raw wikitext for a specific month.
     * @param wikitext The raw wikitext of the month section
     * @param month The month name (for traceability when date is missing)
     * @return List of cleaned EventItem objects
     */
    private List<EventResponse.EventItem> extractMonthEvents(String wikitext, String month) {
        List<EventResponse.EventItem> events = new ArrayList<>();

        if (wikitext == null || wikitext.isBlank()) {
            return events;
        }

        for (String line : wikitext.split("\n")) {
            if (events.size() >= DEFAULT_LIMIT) {
                break;
            }

            WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(line, false, MAX_LINE_LEN);
            if (cleaned == null || cleaned.description() == null || cleaned.description().isBlank()) {
                continue;
            }

            String date = cleaned.date();
            if ((date == null || date.isBlank()) && month != null && !month.isBlank()) {
                date = month;
            }

            events.add(new EventResponse.EventItem(date == null ? "" : date, cleaned.description()));
        }

        return events;
    }

    /**
     * Safe trim a string, returning empty string if null.
     * @param value The input string
     * @return Trimmed string or empty if null
     */
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
