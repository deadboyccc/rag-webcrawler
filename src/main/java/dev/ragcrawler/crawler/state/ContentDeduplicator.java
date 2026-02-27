package dev.ragcrawler.crawler.state;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ContentDeduplicator {

    private final Set<String> hashes = ConcurrentHashMap.newKeySet();

    public boolean isDuplicate(String hash) {
        return !hashes.add(hash);
    }
}

