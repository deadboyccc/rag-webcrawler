package dev.ragcrawler.crawler;

import dev.ragcrawler.crawler.http.HttpFetcher;
import dev.ragcrawler.crawler.http.PerHostScheduler;
import dev.ragcrawler.crawler.output.JsonlChunkWriter;
import dev.ragcrawler.crawler.parsing.ContentChunker;
import dev.ragcrawler.crawler.parsing.ContentExtractor;
import dev.ragcrawler.crawler.parsing.HtmlParser;
import dev.ragcrawler.crawler.robots.RobotsCache;
import dev.ragcrawler.crawler.robots.RobotsPolicy;
import dev.ragcrawler.crawler.state.ContentDeduplicator;
import dev.ragcrawler.crawler.state.UrlFrontier;
import dev.ragcrawler.crawler.state.UrlNormalizer;
import dev.ragcrawler.crawler.state.VisitedUrlStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates the crawl lifecycle.
 */
public final class CrawlerApplication {

    private static final Logger log = LoggerFactory.getLogger(CrawlerApplication.class);

    private final CrawlConfig config;
    private final Path outputPath;

    public CrawlerApplication(CrawlConfig config, Path outputPath) {
        this.config = Objects.requireNonNull(config);
        this.outputPath = Objects.requireNonNull(outputPath);
    }

    public void run() throws IOException {
        Instant deadline = Instant.now().plus(config.maxTime());
        AtomicBoolean cancelled = new AtomicBoolean(false);

        if (Files.notExists(outputPath.getParent())) {
            Files.createDirectories(outputPath.getParent());
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             JsonlChunkWriter writer = new JsonlChunkWriter(outputPath);
             HttpFetcher httpFetcher = new HttpFetcher(config, deadline, cancelled);
        ) {
            UrlNormalizer normalizer = new UrlNormalizer(config.rootUri());
            UrlFrontier frontier = new UrlFrontier();
            VisitedUrlStore visited = new VisitedUrlStore();
            ContentDeduplicator deduplicator = new ContentDeduplicator();
            RobotsCache robotsCache = new RobotsCache(httpFetcher, config.userAgent());
            RobotsPolicy robotsPolicy = new RobotsPolicy(robotsCache);
            PerHostScheduler perHostScheduler = new PerHostScheduler(
                    config.perHostConcurrency(),
                    config.perHostMinDelayMillis(),
                    deadline,
                    cancelled
            );
            HtmlParser htmlParser = new HtmlParser();
            ContentExtractor contentExtractor = new ContentExtractor();
            ContentChunker chunker = new ContentChunker();
            AtomicInteger pagesCrawled = new AtomicInteger(0);

            String normalizedRoot = normalizer.normalize(config.rootUri());
            frontier.offer(new UrlFrontier.Task(normalizedRoot, 0));

            AtomicInteger inFlight = new AtomicInteger(0);

            log.info("Starting crawl: root={} maxTime={} maxPages={} maxDepth={}",
                    config.rootUri(), config.maxTime(), config.maxPages(), config.maxDepth());

            while (!cancelled.get()) {
                if (Instant.now().isAfter(deadline)) {
                    log.info("Global deadline reached; stopping crawl loop");
                    cancelled.set(true);
                    break;
                }
                if (config.maxPages() != null && pagesCrawled.get() >= config.maxPages()) {
                    log.info("Reached maxPages {}; stopping crawl loop", config.maxPages());
                    cancelled.set(true);
                    break;
                }

                Optional<UrlFrontier.Task> maybeTask = frontier.poll();
                if (maybeTask.isEmpty()) {
                    if (inFlight.get() == 0) {
                        log.info("Frontier empty and no in-flight tasks; crawl complete");
                        break;
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        cancelled.set(true);
                        break;
                    }
                    continue;
                }

                UrlFrontier.Task task = maybeTask.get();

                if (config.maxDepth() != null && task.depth() > config.maxDepth()) {
                    continue;
                }

                if (visited.markVisited(task.normalizedUrl())) {
                    continue;
                }

                if (config.maxPages() != null && pagesCrawled.get() >= config.maxPages()) {
                    continue;
                }

                inFlight.incrementAndGet();
                executor.submit(new PageCrawlTask(
                        task,
                        config,
                        deadline,
                        cancelled,
                        normalizer,
                        frontier,
                        pagesCrawled,
                        inFlight,
                        httpFetcher,
                        robotsPolicy,
                        perHostScheduler,
                        htmlParser,
                        contentExtractor,
                        chunker,
                        deduplicator,
                        writer
                ));
            }

            while (inFlight.get() > 0 && !cancelled.get()) {
                if (Instant.now().isAfter(deadline)) {
                    log.info("Deadline reached while waiting for tasks; setting cancelled");
                    cancelled.set(true);
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            log.info("Crawl finished: pagesCrawled={}", pagesCrawled.get());
        }
    }
}

