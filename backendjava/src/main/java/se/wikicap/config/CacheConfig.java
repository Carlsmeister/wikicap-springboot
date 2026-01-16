package se.wikicap.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    /**
     * Cache manager for @Cacheable usage.
     *
     * Keeps an in-memory cache (Caffeine) with a reasonable TTL so we don't
     * re-fetch Wikipedia/Spotify on every request for the same year.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("music");
        cacheManager.setAsyncCacheMode(true);
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(12))
                        .maximumSize(200)
        );
        return cacheManager;
    }
}
