package se.wikicap.util;

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

    private static final Pattern DATE_PREFIX = Pattern.compile(
            "^(?:\\[\\[)?(" + MONTHS_PATTERN + ")\\s+(\\d{1,2})(?:]])?\\s*[–—-]\\s*",
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
     */
    public static CleanResult cleanEventLine(String line, boolean keepDatePrefix, int maxLen) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String stripped = line.strip();
        if (!stripped.startsWith("*")) {
            return null;
        }

        stripped = stripped.replaceFirst("^\\*+", "").trim();

        String date = "";
        Matcher dateMatch = DATE_PREFIX.matcher(stripped);
        if (dateMatch.find()) {
            String month = capitalize(dateMatch.group(1));
            String day = dateMatch.group(2);
            date = month.substring(0, 3) + " " + day;
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

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
