package dev.ragcrawler.crawler.robots;

import java.net.URI;

public final class RobotsPolicy {

    private final RobotsCache cache;

    public RobotsPolicy(RobotsCache cache) {
        this.cache = cache;
    }

    public boolean isAllowed(URI uri) {
        RobotsCache.RobotsRules rules = cache.rulesFor(uri);
        String path = uri.getPath() == null ? "/" : uri.getPath();
        return rules.isAllowed(path);
    }
}

