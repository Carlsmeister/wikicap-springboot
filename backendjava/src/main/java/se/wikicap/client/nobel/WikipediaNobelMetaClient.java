package se.wikicap.client.nobel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.wikicap.dto.nobel.WikipediaMetadata;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Client for fetching Wikipedia metadata for Nobel laureates.
 * Uses both Wikipedia and Wikidata APIs, with HTML scraping as a fallback.
 */
@Component
public class WikipediaNobelMetaClient {

    private static final String USER_AGENT = "WikiCapSpringBoot/1.0 (WikiCap course project; contact: carl.lundholm@example.com)";

    private final WebClient wikipediaClient;
    private final WebClient wikidataClient;

    public WikipediaNobelMetaClient() {
        this.wikipediaClient = WebClient.builder()
                .baseUrl("https://en.wikipedia.org/w/api.php")
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 2048))
                .build();

        this.wikidataClient = WebClient.builder()
                .baseUrl("https://www.wikidata.org/w/api.php")
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 2048))
                .build();
    }

    private record CountryDetails(String countryName, String countryFlag) {}

    /**
     * Fetch Wikipedia metadata for a laureate given their Wikipedia URL.
     * Prefers structured Wikidata API for reliability, falls back to HTML scraping.
     * @param wikiUrl The full Wikipedia URL (e.g., https://en.wikipedia.org/wiki/Albert_Einstein)
     * @return Mono emitting WikipediaMetadata with imageURL, countryName, countryFlag
     */
    public Mono<WikipediaMetadata> fetchWikipediaMetadata(String wikiUrl) {
        if (wikiUrl == null || wikiUrl.isBlank() || !wikiUrl.startsWith("https://en.wikipedia.org/wiki/")) {
            return Mono.just(new WikipediaMetadata("", "", ""));
        }

        String title = extractTitleFromUrl(wikiUrl);
        if (title.isBlank()) {
            return Mono.just(new WikipediaMetadata("", "", ""));
        }

        // First, try to get Wikidata ID from Wikipedia page
        return getWikidataId(title)
                .flatMap(wikidataId -> {
                    if (wikidataId != null && !wikidataId.isBlank()) {
                        // Get country QID from laureate's Wikidata
                        return getCountryQid(wikidataId)
                                .flatMap(countryQid -> {
                                    if (countryQid != null && !countryQid.isBlank()) {
                                        // Fetch image from Wikipedia API, country from Wikidata
                                        Mono<String> imageMono = getImageFromWikipedia(title, wikiUrl);
                                        Mono<CountryDetails> countryMono = fetchCountryDetailsFromWikidata(countryQid);
                                        return Mono.zip(imageMono, countryMono)
                                                .map(tuple -> new WikipediaMetadata(
                                                        tuple.getT1(),
                                                        tuple.getT2().countryName(),
                                                        tuple.getT2().countryFlag()
                                                ));
                                    } else {
                                        // No country, fetch image and scrape country
                                        Mono<String> imageMono = getImageFromWikipedia(title, wikiUrl);
                                        Mono<String> countryMono = scrapeCountryFromWikipedia(wikiUrl);
                                        return Mono.zip(imageMono, countryMono)
                                                .flatMap(tuple -> {
                                                    String imageURL = tuple.getT1();
                                                    String countryName = tuple.getT2();
                                                    if (countryName != null && !countryName.isBlank()) {
                                                        return scrapeFlagFromCountry(countryName)
                                                                .map(flagURL -> new WikipediaMetadata(imageURL, countryName, flagURL));
                                                    } else {
                                                        return Mono.just(new WikipediaMetadata(imageURL, "", ""));
                                                    }
                                                });
                                    }
                                });
                    } else {
                        // Fallback to scraping
                        return fetchByScraping(wikiUrl);
                    }
                })
                .timeout(java.time.Duration.ofSeconds(10)) // Defensive timeout
                .onErrorReturn(new WikipediaMetadata("", "", "")); // Fallback on error
    }

    /**
     * Extract the page title from the Wikipedia URL.
     * E.g., from https://en.wikipedia.org/wiki/Albert_Einstein extract "Albert_Einstein"
     * @param wikiUrl The Wikipedia URL
     * @return The URL-encoded page title
     */
    private String extractTitleFromUrl(String wikiUrl) {
        try {
            int lastSlash = wikiUrl.lastIndexOf('/');
            if (lastSlash == -1) return "";
            return URLEncoder.encode(wikiUrl.substring(lastSlash + 1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get Wikidata ID for a Wikipedia page title.
     * Uses MediaWiki API: action=query&prop=pageprops&ppprop=wikibase_item
     * @param title The Wikipedia page title
     * @return Mono emitting the Wikidata ID (e.g., "Q42") or empty string
     */
    private Mono<String> getWikidataId(String title) {
        return wikipediaClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "query")
                        .queryParam("prop", "pageprops")
                        .queryParam("ppprop", "wikibase_item")
                        .queryParam("titles", title)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractWikidataId)
                .onErrorReturn(""); // If API fails, return empty
    }

    @SuppressWarnings("unchecked")
    private String extractWikidataId(Map<String, Object> response) {
        try {
            Map<String, Object> query = (Map<String, Object>) response.get("query");
            Map<String, Object> pages = (Map<String, Object>) query.get("pages");
            for (Object page : pages.values()) {
                Map<String, Object> pageMap = (Map<String, Object>) page;
                Map<String, Object> pageprops = (Map<String, Object>) pageMap.get("pageprops");
                if (pageprops != null) {
                    Object wikibaseItem = pageprops.get("wikibase_item");
                    if (wikibaseItem instanceof String) {
                        return (String) wikibaseItem;
                    }
                }
            }
        } catch (Exception e) {
            // Parsing error, return null
        }
        return null;
    }

    /**
     * Fallback: Fetch metadata by scraping the Wikipedia HTML.
     * Less reliable due to varying page structures, but used when Wikidata is unavailable.
     * Attempts to scrape image, country name, and flag.
     */
    private Mono<WikipediaMetadata> fetchByScraping(String wikiUrl) {
        Mono<String> imageMono = scrapeImageFromWikipedia(wikiUrl);
        Mono<String> countryMono = scrapeCountryFromWikipedia(wikiUrl);

        return Mono.zip(imageMono, countryMono)
                .flatMap(tuple -> {
                    String imageURL = tuple.getT1();
                    String countryName = tuple.getT2();
                    if (countryName != null && !countryName.isBlank()) {
                        return scrapeFlagFromCountry(countryName)
                                .map(flagURL -> new WikipediaMetadata(imageURL, countryName, flagURL));
                    } else {
                        return Mono.just(new WikipediaMetadata(imageURL, "", ""));
                    }
                });
    }

    /**
     * Scrape the main image from a Wikipedia page's infobox.
     * Selector: .infobox img - targets the first image in the infobox, often the main portrait.
     * Failure cases: Pages without infobox, disambiguation pages, or non-standard layouts.
     * @param wikiUrl The Wikipedia URL
     * @return Mono emitting the image URL or empty string
     */
    public Mono<String> scrapeImageFromWikipedia(String wikiUrl) {
        if (wikiUrl == null || wikiUrl.isBlank()) return Mono.just("");

        return Mono.fromCallable(() -> {
            try {
                Document doc = Jsoup.connect(wikiUrl).userAgent(USER_AGENT).get();
                Element img = doc.select(".infobox img").first();
                if (img != null) {
                    String src = img.attr("src");
                    if (src.startsWith("//")) src = "https:" + src;
                    return src;
                }
            } catch (Exception e) {
                // Log error if needed, e.g., network issues or parsing failures
            }
            return "";
        });
    }

    /**
     * Scrape the country flag image from the country's Wikipedia page.
     * First, would need to scrape country name from laureate page, then flag from country page.
     * For simplicity in fallback, not implemented fully, as Wikidata is preferred.
     * @param countryName The country name
     * @return Mono emitting the flag image URL or empty string
     */
    public Mono<String> scrapeFlagFromCountry(String countryName) {
        if (countryName == null || countryName.isBlank()) return Mono.just("");

        return Mono.fromCallable(() -> {
            try {
                String countryUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(countryName.replace(" ", "_"), StandardCharsets.UTF_8);
                Document doc = Jsoup.connect(countryUrl).userAgent(USER_AGENT).get();
                Element img = doc.select(".infobox img[src*='Flag']").first();
                if (img == null) {
                    // Prefer SVG images (flags are often SVG)
                    img = doc.select(".infobox img[src$='.svg']").first();
                    if (img == null) img = doc.select(".infobox img").first();
                }
                if (img != null) {
                    String src = img.attr("src");
                    if (src.startsWith("//")) src = "https:" + src;
                    // Validate that it's likely a flag: contains "Flag_of_" or is SVG
                    if (src.contains("Flag_of_") || src.endsWith(".svg")) {
                        return src;
                    }
                }
            } catch (Exception e) {
                // Log error
            }
            return "";
        });
    }

    /**
     * Scrape the country name from a Wikipedia page.
     * Attempts to find the country in the infobox or by other means on the page.
     * @param wikiUrl The Wikipedia URL
     * @return Mono emitting the country name or empty string
     */
    public Mono<String> scrapeCountryFromWikipedia(String wikiUrl) {
        if (wikiUrl == null || wikiUrl.isBlank()) return Mono.just("");

        return Mono.fromCallable(() -> {
            try {
                Document doc = Jsoup.connect(wikiUrl).userAgent(USER_AGENT).get();
                // Try to find country in infobox
                Element infobox = doc.select(".infobox").first();
                if (infobox != null) {
                    for (Element row : infobox.select("tr")) {
                        Element th = row.select("th").first();
                        Element td = row.select("td").first();
                        if (th != null && td != null) {
                            String header = th.text();
                            String value = td.text();
                            // Heuristics to find country name
                            if (header.contains("Born") && value.contains(",")) {
                                String country = value.substring(value.lastIndexOf(',') + 1).trim();
                                return country;
                            } else if (header.contains("Nationality") || header.contains("Citizenship")) {
                                String country = value.trim();
                                return country;
                            } else if (header.contains("Birth place") || header.contains("Place of birth")) {
                                String[] parts = value.split(",");
                                if (parts.length > 1) {
                                    String country = parts[parts.length - 1].trim();
                                    return country;
                                }
                            } else if (header.contains("Country") || header.contains("Origin")) {
                                String country = value.trim();
                                return country;
                            } else if (header.contains("Born") && value.contains(" in ")) {
                                String country = value.substring(value.lastIndexOf(" in ") + 4).trim();
                                return country;
                            }
                        }
                    }
                }
                // Fallback: Just extract from the first paragraph
                Element firstPara = doc.select("p").first();
                if (firstPara != null) {
                    String text = firstPara.text();
                    if (text.contains(",")) {
                        String country = text.substring(text.lastIndexOf(',') + 1).trim();
                        return country;
                    }
                }
            } catch (Exception e) {
                // Log error if needed
            }
            return "";
        });
    }

    /**
     * Get image URL from Wikipedia API's pageimages prop.
     * Uses MediaWiki API: action=query&prop=pageimages&pithumbsize=300
     * Falls back to scraping if no thumbnail available.
     */
    private Mono<String> getImageFromWikipedia(String title, String wikiUrl) {
        return wikipediaClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "query")
                        .queryParam("prop", "pageimages")
                        .queryParam("pithumbsize", "300")
                        .queryParam("titles", title)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractImageFromPageimages)
                .flatMap(url -> {
                    if (url.isBlank()) {
                        // Fallback to scraping
                        return scrapeImageFromWikipedia(wikiUrl);
                    } else {
                        return Mono.just(url);
                    }
                })
                .onErrorReturn(""); // If API fails, return empty
    }

    /**
     * Extract image URL from Wikipedia pageimages API response.
     * @param response The Wikipedia API response map
     * @return The image URL or empty string
     */
    @SuppressWarnings("unchecked")
    private String extractImageFromPageimages(Map<String, Object> response) {
        try {
            Map<String, Object> query = (Map<String, Object>) response.get("query");
            Map<String, Object> pages = (Map<String, Object>) query.get("pages");
            for (Object page : pages.values()) {
                Map<String, Object> pageMap = (Map<String, Object>) page;
                Map<String, Object> pageimages = (Map<String, Object>) pageMap.get("pageimages");
                if (pageimages != null) {
                    Object imageObject = pageimages.get("thumbnail");
                    if (imageObject instanceof Map<?, ?> imgMap) {
                        String url = (String) imgMap.get("url");
                        return url != null ? url : "";
                    }
                }
            }
        } catch (Exception e) {
            // Parsing error, return empty
        }
        return "";
    }

    /**
     * Fetch country details (name and flag) from Wikidata using the country QID.
     * @param countryQid The Wikidata QID for the country
     * @return Mono emitting CountryDetails with countryName, countryFlag
     */
    private Mono<CountryDetails> fetchCountryDetailsFromWikidata(String countryQid) {
        return wikidataClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "wbgetentities")
                        .queryParam("ids", countryQid)
                        .queryParam("props", "labels")
                        .queryParam("languages", "en")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Map.of())
                .map(resp -> {
                    if (resp.isEmpty()) return new CountryDetails("", "");
                    try {
                        Map<String, Object> entities = (Map<String, Object>) resp.get("entities");
                        if (entities == null || entities.isEmpty()) return new CountryDetails("", "");
                        Map<String, Object> entity = (Map<String, Object>) entities.values().iterator().next();
                        Map<String, Object> labels = (Map<String, Object>) entity.get("labels");
                        String countryName = extractLabel(labels, countryQid);
                        return countryName != null ? countryName : "";
                    } catch (Exception e) {
                        return "";
                    }
                })
                .flatMap(countryNameObj -> {
                    String countryName = countryNameObj instanceof String ? (String) countryNameObj : "";
                    if (countryName.isBlank()) {
                        return Mono.just(new CountryDetails("", ""));
                    }
                    // Scrape flag from country's Wikipedia page
                    return scrapeFlagFromCountry(countryName)
                            .map(flagUrl -> new CountryDetails(countryName, flagUrl))
                            .onErrorReturn(new CountryDetails(countryName, ""));
                });
    }

    /**
     * Get country QID from the laureate's Wikidata P27 (country of citizenship).
     * @param laureateQid The Wikidata QID of the laureate
     * @return Mono emitting the country QID or empty string
     */
    private Mono<String> getCountryQid(String laureateQid) {
        return wikidataClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("action", "wbgetentities")
                        .queryParam("ids", laureateQid)
                        .queryParam("props", "claims")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractCountryQidFromLaureate)
                .onErrorReturn("");
    }

    /**
     * Extract country QID from Wikidata laureate entity response.
     * @param response The Wikidata API response map
     * @return The country QID or null
     */
    @SuppressWarnings("unchecked")
    private String extractCountryQidFromLaureate(Map<String, Object> response) {
        try {
            Map<String, Object> entities = (Map<String, Object>) response.get("entities");
            Map<String, Object> entity = (Map<String, Object>) entities.values().iterator().next();
            Map<String, Object> claims = (Map<String, Object>) entity.get("claims");
            return extractCountryQid(claims);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract country QID from Wikidata claims map.
     * @param claims The claims map from Wikidata entity
     * @return The country QID or null
     */
    @SuppressWarnings("unchecked")
    private String extractCountryQid(Map<String, Object> claims) {
        try {
            Object p27 = claims.get("P27");
            if (p27 instanceof java.util.List<?> list && !list.isEmpty()) {
                Map<String, Object> claim = (Map<String, Object>) list.get(0);
                Map<String, Object> mainsnak = (Map<String, Object>) claim.get("mainsnak");
                Map<String, Object> datavalue = (Map<String, Object>) mainsnak.get("datavalue");
                Map<String, Object> value = (Map<String, Object>) datavalue.get("value");
                return (String) value.get("id");
            }
        } catch (Exception e) {
            // No country
        }
        return null;
    }

    /**
     * Extract English label from Wikidata labels map.
     * @param labels The labels map from Wikidata entity
     * @param qid The QID of the entity (for logging)
     * @return The English label or null
     */
    @SuppressWarnings("unchecked")
    private String extractLabel(Map<String, Object> labels, String qid) {
        try {
            Object enLabel = labels.get("en");
            if (enLabel instanceof Map<?, ?> map) {
                return (String) map.get("value");
            }
        } catch (Exception e) {
            // Error extracting label
        }
        return null;
    }
}
