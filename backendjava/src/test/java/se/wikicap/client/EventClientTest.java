package se.wikicap.client;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventClientTest {

    @Test
    @SuppressWarnings("unchecked")
    void readsTocLevelFromTocdataEntries() throws Exception {
        // Real response for page "1994", recorded 2026-10-19 (see EventServiceTest)
        Map<String, Object> response;
        try (InputStream in = getClass().getResourceAsStream("/wikipedia/1994-tocdata.json")) {
            response = new ObjectMapper().readValue(in, new TypeReference<Map<String, Object>>() {});
        }
        Map<String, Object> tocdata = (Map<String, Object>) ((Map<String, Object>) response.get("parse")).get("tocdata");
        List<Map<String, Object>> sections = (List<Map<String, Object>>) tocdata.get("sections");

        EventClient client = new EventClient();
        List<EventClient.TocItem> items = sections.stream().map(client::toTocItem).toList();

        assertThat(items).isNotEmpty().allSatisfy(item -> assertThat(item.level()).as(item.line()).isNotNull());
        int events = items.stream().map(EventClient.TocItem::line).toList().indexOf("Events");
        assertThat(items.get(events + 1).line()).isEqualTo("January");
        assertThat(items.get(events + 1).index()).isEqualTo("2");
    }
}
