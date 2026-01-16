package se.wikicap.client.music;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import se.wikicap.service.MusicRankProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Wikipedia-based ranking provider for Billboard year-end Hot 100 charts.
 *
 * Responsibilities:
 * - Downloads the Wikipedia year-end Hot 100 page for a given year
 * - Finds and parses the relevant wikitable
 * - Extracts ranked songs and aggregates artists
 *
 * Notes:
 * - This client performs network I/O and HTML parsing, so work is executed on
 *   {@link reactor.core.scheduler.Schedulers#boundedElastic()}.
 * - Wikipedia pages can vary in structure. The parser is defensive and falls back to
 *   a row index based rank if a rank column is missing.
 */
@Component
public class WikipediaBillboardClient implements MusicRankProvider {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaBillboardClient.class);
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[.*?]");
    private static final Pattern FEATURED_DELIMITER_PATTERN = Pattern.compile("(?i)\\s+(?:featuring|feat\\.?|with)\\s+");
    private static final Pattern LEAD_DELIMITER_PATTERN = Pattern.compile("(?i)\\s+(?:&|and)\\s+|\\s*,\\s*");
    private static final Pattern ALL_DELIMITERS_PATTERN = Pattern.compile("(?i)\\s+(?:featuring|feat\\.?|with|&|and)\\s+|\\s*,\\s*");

    /**
     * Fetches the top songs for a given year.
     *
     * Songs are extracted from the first Wikipedia table that contains both an "Artist"
     * column and a "Song"/"Title" column.
     *
     * @param year year to fetch for (e.g. 2015)
     * @return a {@link Mono} emitting a list of ranked songs; returns an empty list if no suitable table is found
     */
    @Override
    public Mono<List<RankedSong>> getTopSongs(int year) {
        return Mono.fromCallable(() -> {
            Document doc = fetchDocument(year);
            Element table = findBillboardTable(doc);
            if (table == null) return List.<RankedSong>of();

            RankedTableInfo info = analyzeTable(table);
            return info != null ? extractSongsFromTable(table, info) : List.<RankedSong>of();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Fetches the top artists for a given year.
     *
     * Artist aggregation rules:
     * - Featured artists ("feat.", "featuring", "with") are counted, but only the first "lead" artist per row
     *   is used for ordering.
     * - Lead artists are split on commas, "and", and "&".
     *
     * @param year year to fetch for
     * @return a {@link Mono} emitting a list of aggregated artists; returns an empty list if no suitable table is found
     */
    @Override
    public Mono<List<RankedArtist>> getTopArtists(int year) {
        return Mono.fromCallable(() -> {
            Document doc = fetchDocument(year);
            Element table = findBillboardTable(doc);
            if (table == null) return List.<RankedArtist>of();

            RankedTableInfo info = analyzeTable(table);
            return info != null ? extractArtistsFromTable(table, info) : List.<RankedArtist>of();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Downloads and parses the Wikipedia page for the given year.
     *
     * @param year year to fetch
     * @return parsed {@link Document}
     * @throws Exception if the HTTP request fails
     */
    protected Document fetchDocument(int year) throws Exception {
        String url = String.format("https://en.wikipedia.org/wiki/Billboard_Year-End_Hot_100_singles_of_%d", year);
        logger.info("Fetching Billboard chart: {}", url);
        return Jsoup.connect(url)
                .userAgent("WikiCap/1.0 (https://github.com/WikiCap/year-overview)")
                .timeout(10000)
                .get();
    }

    /**
     * Finds the first Wikipedia table that looks like a Billboard year-end chart table.
     *
     * @param doc parsed Wikipedia document
     * @return the first matching table element, or null if none is found
     */
    private Element findBillboardTable(Document doc) {
        Elements tables = doc.select("table.wikitable");
        for (Element table : tables) {
            if (analyzeTable(table) != null) return table;
        }
        return null;
    }

    /**
     * Inspects a table header row to locate key columns.
     *
     * Required columns:
     * - Artist
     * - Song or Title
     *
     * Optional columns:
     * - Rank ("#", "No.", "Rank")
     *
     * @param table table to inspect
     * @return metadata describing column indices, or null if required columns are missing
     */
    private RankedTableInfo analyzeTable(Element table) {
        Element headerRow = table.selectFirst("tr");
        if (headerRow == null) return null;

        Elements headers = headerRow.select("th, td");
        int artistIdx = -1, songIdx = -1, rankIdx = -1;

        for (int i = 0; i < headers.size(); i++) {
            String text = headers.get(i).text().toLowerCase();
            if (text.contains("artist")) artistIdx = i;
            else if (text.contains("song") || text.contains("title")) songIdx = i;
            else if (text.matches("#|no\\.|rank|№")) rankIdx = i;
        }

        if (artistIdx == -1 || songIdx == -1) return null;
        return new RankedTableInfo(artistIdx, songIdx, rankIdx, headers.size());
    }

    /**
     * Extracts songs from a chart table.
     *
     * @param table chart table
     * @param info extracted column metadata
     * @return list of ranked songs
     */
    private List<RankedSong> extractSongsFromTable(Element table, RankedTableInfo info) {
        List<RankedSong> songs = new ArrayList<>();
        Elements rows = table.select("tr");

        String[] lastValues = new String[info.columnCount];
        int[] rowspans = new int[info.columnCount];

        for (int i = 1; i < rows.size(); i++) {
            String[] rowData = unpackRow(rows.get(i), info, lastValues, rowspans);
            String artist = cleanText(rowData[info.artistIndex]);
            String title = cleanText(rowData[info.songIndex]).replaceAll("^\"|\"$", "");

            String rankText = (info.rankIndex >= 0 && info.rankIndex < rowData.length) ? rowData[info.rankIndex] : null;
            int rank = parseRank(rankText, i, info.rankIndex != -1);

            if (!artist.isEmpty() && !title.isEmpty()) {
                songs.add(new RankedSong(title, artist, getPrimaryArtist(artist), rank));
            }
        }
        return songs;
    }

    /**
     * Extracts/aggregates artists from a chart table.
     *
     * @param table chart table
     * @param info extracted column metadata
     * @return list of artists with rank and occurrence count
     */
    private List<RankedArtist> extractArtistsFromTable(Element table, RankedTableInfo info) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        java.util.Map<String, Integer> ranks = new java.util.HashMap<>();
        java.util.Set<String> leadArtists = new java.util.LinkedHashSet<>();

        Elements rows = table.select("tr");
        String[] lastValues = new String[info.columnCount];
        int[] rowspans = new int[info.columnCount];

        for (int i = 1; i < rows.size(); i++) {
            String[] rowData = unpackRow(rows.get(i), info, lastValues, rowspans);
            String artistsText = cleanText(rowData[info.artistIndex]);

            String rankText = (info.rankIndex >= 0 && info.rankIndex < rowData.length) ? rowData[info.rankIndex] : null;
            int rank = parseRank(rankText, i, info.rankIndex != -1);

            if (!artistsText.isEmpty()) {
                aggregateArtists(artistsText, rank, leadArtists, ranks, counts);
            }
        }

        return leadArtists.stream()
                .map(name -> new RankedArtist(name, name, ranks.get(name), counts.get(name)))
                .toList();
    }

    /**
     * Parses a rank (chart position) string.
     *
     * If there is no rank column, or the value cannot be parsed, the method falls back
     * to {@code defaultRank}.
     *
     * @param text raw rank cell text
     * @param defaultRank row-based fallback rank
     * @param hasRankCol whether the table has a rank column
     * @return parsed rank value or {@code defaultRank}
     */
    private int parseRank(String text, int defaultRank, boolean hasRankCol) {
        if (!hasRankCol || text == null || text.isBlank()) return defaultRank;
        try {
            return Integer.parseInt(cleanText(text).replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return defaultRank;
        }
    }

    /**
     * Expands a row into a fixed-width array, handling "rowspan" cells.
     *
     * Wikipedia tables often use rowspan to avoid repeating artist names. This method carries
     * the last seen value forward while the rowspan counter is active.
     *
     * @param row table row
     * @param info column metadata
     * @param lastValues last seen values per column (mutated)
     * @param rowspans remaining rowspan counters per column (mutated)
     * @return a row array with a value (or null) for each column
     */
    private String[] unpackRow(Element row, RankedTableInfo info, String[] lastValues, int[] rowspans) {
        Elements cells = row.select("td, th");
        String[] rowData = new String[info.columnCount];
        int cellIdx = 0;

        for (int i = 0; i < info.columnCount; i++) {
            if (rowspans[i] > 0) {
                rowData[i] = lastValues[i];
                rowspans[i]--;
            } else if (cellIdx < cells.size()) {
                Element cell = cells.get(cellIdx++);
                rowData[i] = cell.text();
                if (cell.hasAttr("rowspan")) {
                    try {
                        rowspans[i] = Integer.parseInt(cell.attr("rowspan")) - 1;
                        lastValues[i] = rowData[i];
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return rowData;
    }

    /**
     * Aggregates artists from a single row.
     *
     * Uses delimiter rules to separate lead artists from featured artists and tracks:
     * - rank of first appearance is stored
     * - occurrenceCount is incremented for every appearance
     *
     * @param text raw artist cell text
     * @param rank rank for the row
     * @param leadArtists insertion-ordered collection of lead artists
     * @param ranks first appearance rank per artist
     * @param counts occurrence count per artist
     */
    private void aggregateArtists(String text, int rank, java.util.Set<String> leadArtists,
                                  java.util.Map<String, Integer> ranks, java.util.Map<String, Integer> counts) {
        String[] parts = FEATURED_DELIMITER_PATTERN.split(text);
        for (int p = 0; p < parts.length; p++) {
            String[] subArtists = LEAD_DELIMITER_PATTERN.split(parts[p]);
            for (String artist : subArtists) {
                artist = artist.trim();
                if (artist.isEmpty()) continue;

                if (p == 0) leadArtists.add(artist);
                if (!ranks.containsKey(artist)) ranks.put(artist, rank);
                counts.put(artist, counts.getOrDefault(artist, 0) + 1);
            }
        }
    }

    /**
     * Returns the "primary" artist name for a display string.
     *
     * For example:
     * - "Mark Ronson featuring Bruno Mars" -> "Mark Ronson"
     * - "Ariana Grande & The Weeknd" -> "Ariana Grande"
     *
     * @param artist raw artist string
     * @return primary artist name
     */
    private String getPrimaryArtist(String artist) {
        String[] parts = ALL_DELIMITERS_PATTERN.split(artist);
        return parts.length > 0 ? parts[0].trim() : artist;
    }

    /**
     * Cleans Wikipedia cell text by removing citation markers like "[1]".
     *
     * @param text raw cell text
     * @return cleaned text
     */
    private String cleanText(String text) {
        return CITATION_PATTERN.matcher(text).replaceAll("").trim();
    }

    /**
     * Simple table metadata used during parsing.
     */
    private record RankedTableInfo(int artistIndex, int songIndex, int rankIndex, int columnCount) {}
}
