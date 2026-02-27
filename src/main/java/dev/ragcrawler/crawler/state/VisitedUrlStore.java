package dev.ragcrawler.crawler.state;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VisitedUrlStore {

    private final Set<String> visited = ConcurrentHashMap.newKeySet();

    /**
     * Marks the URL as visited if not already present.
     *
     * @return true if this call marked URL as visited, false if it was already visited.
     */
    public boolean markVisited(String normalizedUrl) {
        return visited.add(normalizedUrl);
    }
}

