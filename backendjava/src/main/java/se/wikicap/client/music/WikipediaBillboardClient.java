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

@Component
public class WikipediaBillboardClient implements MusicRankProvider {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaBillboardClient.class);
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[.*?\\]");
    private static final Pattern FEATURED_DELIMITER_PATTERN = Pattern.compile("(?i)\\s+(?:featuring|feat\\.?|with)\\s+");
    private static final Pattern LEAD_DELIMITER_PATTERN = Pattern.compile("(?i)\\s+(?:&|and)\\s+|\\s*,\\s*");
    private static final Pattern ALL_DELIMITERS_PATTERN = Pattern.compile("(?i)\\s+(?:featuring|feat\\.?|with|&|and)\\s+|\\s*,\\s*");

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

    private Document fetchDocument(int year) throws Exception {
        String url = String.format("https://en.wikipedia.org/wiki/Billboard_Year-End_Hot_100_singles_of_%d", year);
        logger.info("Fetching Billboard chart: {}", url);
        return Jsoup.connect(url)
                .userAgent("WikiCap/1.0 (https://github.com/WikiCap/year-overview)")
                .timeout(10000)
                .get();
    }

    private Element findBillboardTable(Document doc) {
        Elements tables = doc.select("table.wikitable");
        for (Element table : tables) {
            if (analyzeTable(table) != null) return table;
        }
        return null;
    }

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

    private List<RankedSong> extractSongsFromTable(Element table, RankedTableInfo info) {
        List<RankedSong> songs = new ArrayList<>();
        Elements rows = table.select("tr");

        String[] lastValues = new String[info.columnCount];
        int[] rowspans = new int[info.columnCount];

        for (int i = 1; i < rows.size(); i++) {
            String[] rowData = unpackRow(rows.get(i), info, lastValues, rowspans);
            String artist = cleanText(rowData[info.artistIndex]);
            String title = cleanText(rowData[info.songIndex]).replaceAll("^\"|\"$", "");
            int rank = parseRank(rowData[info.rankIndex], i, info.rankIndex != -1);

            if (!artist.isEmpty() && !title.isEmpty()) {
                songs.add(new RankedSong(title, artist, getPrimaryArtist(artist), rank));
            }
        }
        return songs;
    }

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
            int rank = parseRank(rowData[info.rankIndex], i, info.rankIndex != -1);

            if (!artistsText.isEmpty()) {
                aggregateArtists(artistsText, rank, leadArtists, ranks, counts);
            }
        }

        return leadArtists.stream()
                .map(name -> new RankedArtist(name, name, ranks.get(name), counts.get(name)))
                .toList();
    }

    private int parseRank(String text, int defaultRank, boolean hasRankCol) {
        if (!hasRankCol || text == null || text.isBlank()) return defaultRank;
        try {
            return Integer.parseInt(cleanText(text).replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return defaultRank;
        }
    }

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

    private String getPrimaryArtist(String artist) {
        String[] parts = ALL_DELIMITERS_PATTERN.split(artist);
        return parts.length > 0 ? parts[0].trim() : artist;
    }

    private String cleanText(String text) {
        return CITATION_PATTERN.matcher(text).replaceAll("").trim();
    }

    private record RankedTableInfo(int artistIndex, int songIndex, int rankIndex, int columnCount) {}
}
