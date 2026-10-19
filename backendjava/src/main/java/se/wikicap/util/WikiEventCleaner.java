package se.wikicap.util;

import org.jsoup.parser.Parser;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for cleaning and normalizing Wikipedia event lines (wikitext).
 * Mirrors the Python WikiCleaner with Java regex equivalents.
 */
public final class WikiEventCleaner {

    private static final Pattern REF_TAG = Pattern.compile("<ref[^>]*>.*?</ref>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern REF_SELF = Pattern.compile("<ref[^/>]*/>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>", Pattern.DOTALL);
    private static final Pattern COMMENTS = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern TEMPLATES = Pattern.compile("\\{\\{[^}]+}}", Pattern.DOTALL);
    private static final Pattern FILES = Pattern.compile("\\[\\[(File|Image):[^]]+]]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^]]+)]]");
    private static final Pattern QUOTES = Pattern.compile("''+");

    private static final String MONTHS_PATTERN =
            "January|February|March|April|May|June|July|August|September|October|November|December";

    // a range end is a closed link "[[June 8|8]]" / "[[July 15]]" or a bare "July 15";
    // anything looser eats a description's leading "12-year-old" or "[[24-hour clock]]"
    private static final String RANGE_END =
            "(?:\\[\\[(?:(?:" + MONTHS_PATTERN + ")\\s+)?\\d{1,2}(?:\\|\\d{1,2})?]]"
                    + "|(?:" + MONTHS_PATTERN + ")\\s+\\d{1,2})";

    // optional range end before the separator: "[[June 6]]–[[June 8|8]] –" or "[[April 7]] – [[July 15]] –"
    private static final Pattern DATE_PREFIX = Pattern.compile(
            "^(?:\\[\\[)?(" + MONTHS_PATTERN + ")\\s+(\\d{1,2})(?:]])?"
                    + "(?:\\s*[–—-]\\s*" + RANGE_END + ")?"
                    + "\\s*[–—-]\\s*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DATE_ONLY = Pattern.compile(
            "^(" + MONTHS_PATTERN + ")\\s+(\\d{1,2}):?$",
            Pattern.CASE_INSENSITIVE
    );

    private WikiEventCleaner() {
    }

    public record CleanResult(String date, String description) {}

    /**
     * Clean a single bullet-point line from Wikipedia wikitext.
     *
     * - Removes refs/templates/html/files
     * - Flattens wiki links to display text
     * - Extracts a date prefix like "January 12 -" if present
     * - A line holding only a date, like "* [[February 12]]", returns that date and an empty description
     */
    public static CleanResult cleanEventLine(String line, boolean keepDatePrefix, int maxLen) {
        if (line == null || line.isBlank()) {
            return null;
        }

        // decode before the date match: "[[May 10]] &ndash; ..." must read as a dash
        String stripped = Parser.unescapeEntities(line, false).replace('\u00A0', ' ').strip();
        if (!stripped.startsWith("*")) {
            return null;
        }

        stripped = stripped.replaceFirst("^\\*+", "").trim();

        String date = "";
        Matcher dateMatch = DATE_PREFIX.matcher(stripped);
        if (dateMatch.find()) {
            date = shortDate(dateMatch.group(1), dateMatch.group(2));
            if (!keepDatePrefix) {
                stripped = dateMatch.replaceFirst("").trim();
            }
        }

        stripped = REF_TAG.matcher(stripped).replaceAll("");
        stripped = REF_SELF.matcher(stripped).replaceAll("");
        stripped = COMMENTS.matcher(stripped).replaceAll("");
        stripped = HTML_TAG.matcher(stripped).replaceAll("");

        stripped = FILES.matcher(stripped).replaceAll("");
        stripped = TEMPLATES.matcher(stripped).replaceAll("");

        stripped = replaceWikiLinks(stripped);

        stripped = QUOTES.matcher(stripped).replaceAll("");

        stripped = stripped.replace("–", "-").replace("—", "-");
        stripped = stripped.replaceAll("\\s+", " ").trim();
        stripped = stripped.replaceAll("^[-\\s]+|[-\\s]+$", "");

        Matcher dateOnly = DATE_ONLY.matcher(stripped);
        if (date.isBlank() && dateOnly.matches()) {
            return new CleanResult(shortDate(dateOnly.group(1), dateOnly.group(2)), "");
        }

        if (stripped.length() < 8) {
            return null;
        }

        if (keepDatePrefix && !date.isBlank()) {
            stripped = date + " - " + stripped;
        }

        if (stripped.length() > maxLen) {
            stripped = stripped.substring(0, Math.max(0, maxLen - 3)).trim() + "...";
        }

        return new CleanResult(date, stripped);
    }

    private static String replaceWikiLinks(String input) {
        Matcher matcher = WIKI_LINK.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String inner = matcher.group(1) == null ? "" : matcher.group(1).trim();
            String replacement = "";
            if (!inner.isBlank()) {
                String lowered = inner.toLowerCase(Locale.ROOT);
                if (!(lowered.startsWith("category:")
                        || lowered.startsWith("help:")
                        || lowered.startsWith("portal:")
                        || lowered.startsWith("special:"))) {
                    if (inner.contains("|")) {
                        replacement = inner.substring(inner.lastIndexOf('|') + 1).trim();
                    } else {
                        replacement = inner;
                    }
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String shortDate(String month, String day) {
        return capitalize(month).substring(0, 3) + " " + day;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
