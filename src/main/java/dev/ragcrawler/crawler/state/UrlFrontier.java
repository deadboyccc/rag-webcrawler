package dev.ragcrawler.crawler.state;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple unbounded frontier.
 * Can be evolved to bounded if needed.
 */
public final class UrlFrontier {

    public record Task(String normalizedUrl, int depth) {}

    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

    public void offer(Task task) {
        queue.offer(task);
    }

    public Optional<Task> poll() {
        Task t = queue.poll();
        return Optional.ofNullable(t);
    }

    public Optional<Task> poll(Duration timeout) throws InterruptedException {
        Task t = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        return Optional.ofNullable(t);
    }

    public int size() {
        return queue.size();
    }
}

