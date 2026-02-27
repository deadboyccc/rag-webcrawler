package dev.ragcrawler.crawler;

import java.net.URI;
import java.time.Duration;

/**
 * Immutable crawl configuration.
 */
public record CrawlConfig(
        URI rootUri,
        Duration maxTime,
        Integer maxPages,
        Integer maxDepth,
        int perHostConcurrency,
        long perHostMinDelayMillis,
        String userAgent
) {

    public CrawlConfig {
        if (rootUri == null) throw new IllegalArgumentException("rootUri must not be null");
        if (maxTime == null || maxTime.isNegative() || maxTime.isZero()) {
            throw new IllegalArgumentException("maxTime must be positive");
        }
        if (perHostConcurrency <= 0) {
            throw new IllegalArgumentException("perHostConcurrency must be > 0");
        }
        if (perHostMinDelayMillis < 0) {
            throw new IllegalArgumentException("perHostMinDelayMillis must be >= 0");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent must not be blank");
        }
    }
}

