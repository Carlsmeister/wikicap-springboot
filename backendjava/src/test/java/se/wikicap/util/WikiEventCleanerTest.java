package se.wikicap.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WikiEventCleanerTest {

    @Test
    void keepsTextBetweenEscapedLessThanAndLaterGreaterThan() {
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* [[May 5]] – Prices rise &lt;5% and then fall >10 points afterwards", false, 200);

        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult(
                "May 5", "Prices rise <5% and then fall >10 points afterwards"));
    }

    @Test
    void keepsStartDateOfCrossMonthRange() {
        // No 1994 Events line spans two months; this one has the shape such lines take
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* [[April 7]] – [[July 15]] – The [[Rwandan genocide]] takes place.", false, 200);

        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult("Apr 7", "The Rwandan genocide takes place."));
    }

    @Test
    void keepsLeadingNumberAndHyphenInDescriptionAfterDate() {
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* [[January 1]] – 12-year-old boy wins the prize", false, 200);

        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult("Jan 1", "12-year-old boy wins the prize"));
    }

    @Test
    void keepsLeadingNumberRangeInDescriptionAfterDate() {
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* [[June 6]] – 50–60 people are killed in a crash", false, 200);

        // the cleaner turns every en dash into a hyphen; what matters is that "50" survives
        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult("Jun 6", "50-60 people are killed in a crash"));
    }

    @Test
    void keepsLeadingNumberInLinkedDescriptionAfterDate() {
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* [[January 1]] – [[24-hour clock]] adopted", false, 200);

        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult("Jan 1", "24-hour clock adopted"));
    }

    @Test
    void keepsLeadingNumberRangeInLinkedDescriptionAfterDate() {
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* [[June 6]] – [[1–2 win]] for the home side", false, 200);

        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult("Jun 6", "1-2 win for the home side"));
    }

    @Test
    void keepsStartDateOfUnlinkedCrossMonthRange() {
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* April 7 – July 15 – The [[Rwandan genocide]] takes place.", false, 200);

        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult("Apr 7", "The Rwandan genocide takes place."));
    }

    @Test
    void dropsLineHoldingOnlyAMonthName() {
        assertThat(WikiEventCleaner.cleanEventLine("* [[December]]", false, 200)).isNull();
    }
}
