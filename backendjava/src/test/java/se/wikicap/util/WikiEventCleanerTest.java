package se.wikicap.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WikiEventCleanerTest {

    @Test
    void keepsStartDateOfCrossMonthRange() {
        // No 1994 Events line spans two months; this one has the shape such lines take
        WikiEventCleaner.CleanResult cleaned = WikiEventCleaner.cleanEventLine(
                "* [[April 7]] – [[July 15]] – The [[Rwandan genocide]] takes place.", false, 200);

        assertThat(cleaned).isEqualTo(new WikiEventCleaner.CleanResult("Apr 7", "The Rwandan genocide takes place."));
    }
}
