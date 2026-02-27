package dev.ragcrawler.crawler.robots;

import dev.ragcrawler.crawler.http.HttpFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very small robots.txt cache and parser, focused on Disallow rules.
 * This is intentionally conservative and simple.
 */
public final class RobotsCache {

    private static final Logger log = LoggerFactory.getLogger(RobotsCache.class);

    private final Map<String, RobotsRules> cache = new ConcurrentHashMap<>();
    private final HttpFetcher httpFetcher;
    private final String userAgent;

    public RobotsCache(HttpFetcher fetcher, String userAgent) {
        this.httpFetcher = fetcher;
        this.userAgent = userAgent;
    }

    public RobotsRules rulesFor(URI uri) {
        String key = hostKey(uri);
        return cache.computeIfAbsent(key, k -> fetchRules(uri));
    }

    private RobotsRules fetchRules(URI uri) {
        try {
            URI robotsUri = new URI(uri.getScheme(), uri.getAuthority(), "/robots.txt", null, null);
            HttpFetcher.Response resp = httpFetcher.fetch(robotsUri);
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return RobotsParser.parse(resp.body(), userAgent);
            }
            // If robots not found or inaccessible, default to allow all.
            return RobotsRules.allowAll();
        } catch (Exception e) {
            log.warn("Failed to fetch robots.txt for {}: {}", uri, e.toString());
            return RobotsRules.allowAll();
        }
    }

    private static String hostKey(URI uri) {
        return uri.getScheme() + "://" + uri.getHost();
    }

    public record RobotsRules(List<String> disallowPaths, Duration crawlDelay) {
        public static RobotsRules allowAll() {
            return new RobotsRules(List.of(), Duration.ZERO);
        }

        public boolean isAllowed(String path) {
            for (String dis : disallowPaths) {
                if (!dis.isEmpty() && path.startsWith(dis)) {
                    return false;
                }
            }
            return true;
        }
    }
}

