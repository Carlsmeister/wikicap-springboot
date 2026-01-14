package se.wikicap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {


    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("uptime", getUptime());
        health.put("application", "WikiCap");
        health.put("version", "0.0.1-SNAPSHOT");
        health.put("java", System.getProperty("java.version"));
        return health;
    }

    private String getUptime() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = (uptimeMs / 1000) % 60;
        long minutes = (uptimeMs / (1000 * 60)) % 60;
        long hours = (uptimeMs / (1000 * 60 * 60)) % 24;
        long days = uptimeMs / (1000 * 60 * 60 * 24);

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
