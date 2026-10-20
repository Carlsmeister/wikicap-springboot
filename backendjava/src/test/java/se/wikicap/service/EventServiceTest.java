package se.wikicap.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import se.wikicap.client.EventClient;
import se.wikicap.dto.EventResponse;
import se.wikicap.dto.EventResponse.EventItem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EventService against real MediaWiki API responses for the page "1994", recorded on 2026-10-19
 * from {@code https://en.wikipedia.org/w/api.php} with the URLs EventClient builds:
 * {@code action=parse&page=1994&prop=tocdata&format=json&formatversion=2} and
 * {@code action=parse&page=1994&prop=wikitext&section=<index>&format=json&formatversion=2}.
 * Sections recorded: 2 (Events/January), 3 (Events/February), 6 (Events/May), 7 (Events/June),
 * 15 (Births/January), 16 (Births/February). Any other section is served as empty wikitext.
 * Events/May is there because January and February hold no HTML entities; May has
 * {@code [[May 10]] &ndash; ...}, which gives the entity test something to decode.
 * Events/June is there for its date range {@code [[June 6]]–[[June 8|8]] – ...}.
 */
class EventServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String DATE = "^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{1,2}$";

    private EventResponse response;

    @BeforeEach
    void callServiceWithRecordedResponses() {
        response = new EventService(recordedClient(recordedToc())).getEventsByYear(1994).block();
    }

    @Test
    void takesJanuaryFromEventsSectionNotBirths() {
        String events = plainText(recordedSection("2"));
        String births = plainText(recordedSection("15"));

        List<EventItem> january = response.eventsByMonth().get("January");
        assertThat(january).isNotEmpty();
        for (EventItem item : january) {
            String text = item.description().replaceFirst("\\.\\.\\.$", "");
            assertThat(events).contains(text);
            assertThat(births).doesNotContain(text);
        }
    }

    @Test
    void datesEveryEventAsShortMonthAndDay() {
        assertThat(response.eventsByMonth()).isNotEmpty();
        response.eventsByMonth().values().stream().flatMap(List::stream).forEach(item ->
                assertThat(item.date()).as(item.description()).matches(d -> d.isEmpty() || d.matches(DATE)));
    }

    @Test
    void givesNestedBulletsTheirParentDate() {
        // Events/February: "* [[February 12]]" followed by two "**" sub-bullets
        List<EventItem> february = response.eventsByMonth().get("February");

        assertThat(february).contains(new EventItem("Feb 12", "The 1994 Winter Olympics begin in Lillehammer."));
        assertThat(february).anyMatch(item -> item.date().equals("Feb 12")
                && item.description().startsWith("Edvard Munch's painting The Scream is stolen in Oslo"));
        assertThat(february).noneMatch(item -> item.description().matches("February \\d{1,2}"));
    }

    @Test
    void decodesHtmlEntitiesInDescriptions() {
        // Events/May: "* [[May 10]] &ndash; [[Nelson Mandela]] is inaugurated ..."
        assertThat(response.eventsByMonth().get("May")).contains(new EventItem("May 10",
                "Nelson Mandela is inaugurated as South Africa's first black president."));
        response.eventsByMonth().values().stream().flatMap(List::stream).forEach(item ->
                assertThat(item.description()).doesNotContain("&ndash;", "&amp;", "&nbsp;", "&#"));
    }

    @Test
    void keepsStartDateAndDropsRangeEndFromDescription() {
        // Events/June: "* [[June 6]]–[[June 8|8]] – [[Ceasefire]] negotiations ..."
        EventItem range = response.eventsByMonth().get("June").stream()
                .filter(item -> item.description().contains("Ceasefire")).findFirst().orElseThrow();

        assertThat(range.date()).isEqualTo("Jun 6");
        assertThat(range.description()).doesNotMatch("^[\\d\\-–].*")
                .startsWith("Ceasefire negotiations for the Yugoslav War");
    }

    @Test
    void fallsBackToFirstMonthOccurrenceWhenTocHasNoLevels() {
        List<EventClient.TocItem> levelless = recordedToc().stream()
                .map(item -> new EventClient.TocItem(item.line(), item.index(), null)).toList();
        EventClient client = recordedClient(levelless);

        new EventService(client).getEventsByYear(1994).block();

        verify(client).fetchSectionWikitext(1994, "2");
        verify(client, never()).fetchSectionWikitext(1994, "15");
    }

    @Test
    void givesSubBulletsTheDateOfTheirDatedParent() {
        List<EventItem> january = januaryFrom("""
                * [[January 5]] – Something happens on the fifth of January.
                ** A detail of what happens on the fifth.
                """);

        assertThat(january).containsExactly(
                new EventItem("Jan 5", "Something happens on the fifth of January."),
                new EventItem("Jan 5", "A detail of what happens on the fifth."));
    }

    @Test
    void givesSubBulletsNoDateWhenTheirParentCleansToNothing() {
        List<EventItem> january = januaryFrom("""
                * [[January 5]] – Something happens on the fifth of January.
                * [[NATO]]
                ** A child event under an undated parent.
                """);

        assertThat(january).containsExactly(
                new EventItem("Jan 5", "Something happens on the fifth of January."),
                new EventItem("", "A child event under an undated parent."));
    }

    @Test
    void treatsDateOnlyLinesWithRefsOrColonsAsParents() {
        List<EventItem> january = januaryFrom("""
                * [[January 12]]<ref>{{cite web |title=Source}}</ref>
                ** The first child event of the twelfth.
                * [[January 13]]:
                ** The child event of the thirteenth.
                * [[January 2]]
                ** The child event of the second.
                """);

        assertThat(january).containsExactly(
                new EventItem("Jan 12", "The first child event of the twelfth."),
                new EventItem("Jan 13", "The child event of the thirteenth."),
                new EventItem("Jan 2", "The child event of the second."));
    }

    @Test
    void treatsMonthNameOnlyLinesAsUndatedParents() {
        // 1920's December section has this shape: "* December" over "**" sub-bullets
        List<EventItem> january = januaryFrom("""
                * [[January 5]] – Something happens on the fifth of January.
                * December
                ** A child event under a bare month name.
                * [[September]]
                ** A child event under a linked month name.
                """);

        assertThat(january).containsExactly(
                new EventItem("Jan 5", "Something happens on the fifth of January."),
                new EventItem("", "A child event under a bare month name."),
                new EventItem("", "A child event under a linked month name."));
    }

    /** A client serving the given TOC and the recorded sections. */
    private static EventClient recordedClient(List<EventClient.TocItem> toc) {
        EventClient client = mock(EventClient.class);
        when(client.fetchYearToc(1994)).thenReturn(Mono.just(toc));
        when(client.fetchSectionWikitext(eq(1994), anyString()))
                .thenAnswer(call -> Mono.just(recordedSection(call.getArgument(1))));
        return client;
    }

    /** January events for a page whose only month is Events/January with the given wikitext. */
    private static List<EventItem> januaryFrom(String wikitext) {
        EventClient client = mock(EventClient.class);
        when(client.fetchYearToc(1994)).thenReturn(Mono.just(List.of(
                new EventClient.TocItem("Events", "1", 1), new EventClient.TocItem("January", "2", 2))));
        when(client.fetchSectionWikitext(1994, "2")).thenReturn(Mono.just(wikitext));
        return new EventService(client).getEventsByYear(1994).block().eventsByMonth().get("January");
    }

    /** Maps the recorded tocdata to TocItems the way EventClient.toTocItem does (EventClientTest covers that). */
    private static List<EventClient.TocItem> recordedToc() {
        List<EventClient.TocItem> items = new ArrayList<>();
        for (JsonNode section : read("1994-tocdata.json").path("parse").path("tocdata").path("sections")) {
            JsonNode level = section.get("tocLevel");
            items.add(new EventClient.TocItem(section.path("line").asString(), section.path("index").asString(),
                    level == null ? null : level.asInt()));
        }
        return items;
    }

    private static String recordedSection(String index) {
        if (EventServiceTest.class.getResource("/wikipedia/1994-section-" + index + ".json") == null) {
            return "";
        }
        return read("1994-section-" + index + ".json").path("parse").path("wikitext").asString();
    }

    private static JsonNode read(String name) {
        try (InputStream in = EventServiceTest.class.getResourceAsStream("/wikipedia/" + name)) {
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException(name, e);
        }
    }

    /** Wikitext reduced to its reading text, so cleaned descriptions can be found in it. */
    private static String plainText(String wikitext) {
        return wikitext
                .replaceAll("(?s)<ref[^>]*>.*?</ref>", "")
                .replaceAll("\\{\\{[^}]*}}", "")
                .replaceAll("\\[\\[(?:[^|\\]]*\\|)?([^\\]]*)]]", "$1")
                .replace("''", "")
                .replace("–", "-").replace("—", "-")
                .replaceAll("\\s+", " ");
    }
}
