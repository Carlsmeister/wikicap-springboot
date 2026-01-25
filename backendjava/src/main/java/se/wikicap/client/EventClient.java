package se.wikicap.client;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for Wikipedia year pages via MediaWiki API.
 * Uses parse?tocdata for section discovery and parse?wikitext for content.
 */
@Component
public class EventClient {

    private static final String USER_AGENT = "WikiCapSpringBoot/1.0 (WikiCap course project; contact: carl.lundholm@example.com)";

    private final WebClient wikipediaClient;

    public EventClient() {
        this.wikipediaClient = WebClient.builder()
                .baseUrl("https://en.wikipedia.org/w/api.php")
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    public record TocItem(String line, String index, Integer level) {}

    /**
     * Fetch the table-of-contents for a given year page using MediaWiki parse API.
     */
    public Mono<List<TocItem>> fetchYearToc(int year) {
        return wikipediaClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "parse")
                        .queryParam("page", String.valueOf(year))
                        .queryParam("prop", "tocdata")
                        .queryParam("format", "json")
                        .queryParam("formatversion", "2")
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::extractTocItems)
                .onErrorReturn(List.<TocItem>of());
    }

    /**
     * Fetch raw wikitext for a specific section index from a year page.
     */
    public Mono<String> fetchSectionWikitext(int year, String sectionIndex) {
        return wikipediaClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "parse")
                        .queryParam("page", String.valueOf(year))
                        .queryParam("prop", "wikitext")
                        .queryParam("section", sectionIndex)
                        .queryParam("format", "json")
                        .queryParam("formatversion", "2")
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::extractWikitext)
                .onErrorReturn("");
    }

    @SuppressWarnings("unchecked")
    private List<TocItem> extractTocItems(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return List.of();
        }

        Object parse = response.get("parse");
        if (!(parse instanceof Map<?, ?> parseMap)) {
            return List.of();
        }

        Object tocData = parseMap.get("tocdata");
        return normalizeToc(tocData);
    }

    @SuppressWarnings("unchecked")
    private List<TocItem> normalizeToc(Object tocData) {
        List<TocItem> items = new ArrayList<>();

        if (tocData instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    items.add(toTocItem((Map<String, Object>) map));
                }
            }
            return items;
        }

        if (tocData instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (value instanceof List<?> nestedList) {
                    for (Object entry : nestedList) {
                        if (entry instanceof Map<?, ?> nestedMap) {
                            items.add(toTocItem((Map<String, Object>) nestedMap));
                        }
                    }
                } else if (value instanceof Map<?, ?> nestedMap) {
                    items.add(toTocItem((Map<String, Object>) nestedMap));
                }
            }
        }

        return items;
    }

    private TocItem toTocItem(Map<String, Object> item) {
        if (item == null) {
            return new TocItem("", "", null);
        }

        String line = item.get("line") instanceof String lineValue ? lineValue : "";
        String index = item.get("index") instanceof String indexValue ? indexValue : "";
        Integer level = null;
        Object levelObj = item.get("level");
        if (levelObj instanceof Number number) {
            level = number.intValue();
        } else if (levelObj instanceof String levelStr) {
            try {
                level = Integer.parseInt(levelStr);
            } catch (NumberFormatException ignored) {
                level = null;
            }
        }

        return new TocItem(line, index, level);
    }

    @SuppressWarnings("unchecked")
    private String extractWikitext(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return "";
        }

        Object parse = response.get("parse");
        if (!(parse instanceof Map<?, ?> parseMap)) {
            return "";
        }

        Object wikitext = parseMap.get("wikitext");
        return wikitext instanceof String text ? text : "";
    }
}
