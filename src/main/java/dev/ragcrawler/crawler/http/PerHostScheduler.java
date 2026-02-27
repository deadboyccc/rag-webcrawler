package dev.ragcrawler.crawler.http;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enforces per-host concurrency and minimum delay between requests.
 */
public final class PerHostScheduler {

    private record HostState(Semaphore semaphore, AtomicLong lastRequestNanos) {}

    private final int maxPerHost;
    private final long minDelayMillis;
    private final Instant deadline;
    private final AtomicBoolean cancelled;
    private final Map<String, HostState> hosts = new ConcurrentHashMap<>();

    public PerHostScheduler(int maxPerHost, long minDelayMillis, Instant deadline, AtomicBoolean cancelled) {
        this.maxPerHost = maxPerHost;
        this.minDelayMillis = minDelayMillis;
        this.deadline = deadline;
        this.cancelled = cancelled;
    }

    public boolean beforeRequest(URI uri) throws InterruptedException {
        if (cancelled.get()) return false;
        HostState state = hosts.computeIfAbsent(hostKey(uri),
                k -> new HostState(new Semaphore(maxPerHost), new AtomicLong(0L)));
        if (!state.semaphore.tryAcquire()) {
            state.semaphore.acquire();
        }
        try {
            if (cancelled.get() || Instant.now().isAfter(deadline)) {
                cancelled.set(true);
                return false;
            }
            long now = System.nanoTime();
            long last = state.lastRequestNanos.get();
            long minNanos = minDelayMillis * 1_000_000L;
            long elapsed = now - last;
            if (last > 0 && elapsed < minNanos) {
                long sleepNanos = minNanos - elapsed;
                long sleepMillis = sleepNanos / 1_000_000L;
                if (sleepMillis > 0) {
                    Thread.sleep(sleepMillis);
                }
            }
            state.lastRequestNanos.set(System.nanoTime());
            return true;
        } finally {
            state.semaphore.release();
        }
    }

    private static String hostKey(URI uri) {
        String host = Objects.toString(uri.getHost(), "");
        int port = uri.getPort();
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        return scheme + "://" + host + ":" + port;
    }
}

