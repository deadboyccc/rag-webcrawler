package dev.ragcrawler.crawler;

import dev.ragcrawler.crawler.http.HttpFetcher;
import dev.ragcrawler.crawler.http.PerHostScheduler;
import dev.ragcrawler.crawler.output.JsonlChunkWriter;
import dev.ragcrawler.crawler.parsing.ContentChunker;
import dev.ragcrawler.crawler.parsing.ContentExtractor;
import dev.ragcrawler.crawler.parsing.ExtractedDocument;
import dev.ragcrawler.crawler.parsing.HtmlParser;
import dev.ragcrawler.crawler.parsing.OutputChunk;
import dev.ragcrawler.crawler.robots.RobotsPolicy;
import dev.ragcrawler.crawler.state.ContentDeduplicator;
import dev.ragcrawler.crawler.state.UrlFrontier;
import dev.ragcrawler.crawler.state.UrlNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class PageCrawlTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PageCrawlTask.class);

    private final UrlFrontier.Task task;
    private final CrawlConfig config;
    private final Instant deadline;
    private final AtomicBoolean cancelled;
    private final UrlNormalizer normalizer;
    private final UrlFrontier frontier;
    private final AtomicInteger pagesCrawled;
    private final AtomicInteger inFlight;
    private final HttpFetcher httpFetcher;
    private final RobotsPolicy robotsPolicy;
    private final PerHostScheduler perHostScheduler;
    private final HtmlParser htmlParser;
    private final ContentExtractor contentExtractor;
    private final ContentChunker chunker;
    private final ContentDeduplicator deduplicator;
    private final JsonlChunkWriter writer;

    public PageCrawlTask(
            UrlFrontier.Task task,
            CrawlConfig config,
            Instant deadline,
            AtomicBoolean cancelled,
            UrlNormalizer normalizer,
            UrlFrontier frontier,
            AtomicInteger pagesCrawled,
            AtomicInteger inFlight,
            HttpFetcher httpFetcher,
            RobotsPolicy robotsPolicy,
            PerHostScheduler perHostScheduler,
            HtmlParser htmlParser,
            ContentExtractor contentExtractor,
            ContentChunker chunker,
            ContentDeduplicator deduplicator,
            JsonlChunkWriter writer
    ) {
        this.task = task;
        this.config = config;
        this.deadline = deadline;
        this.cancelled = cancelled;
        this.normalizer = normalizer;
        this.frontier = frontier;
        this.pagesCrawled = pagesCrawled;
        this.inFlight = inFlight;
        this.httpFetcher = httpFetcher;
        this.robotsPolicy = robotsPolicy;
        this.perHostScheduler = perHostScheduler;
        this.htmlParser = htmlParser;
        this.contentExtractor = contentExtractor;
        this.chunker = chunker;
        this.deduplicator = deduplicator;
        this.writer = writer;
    }

    @Override
    public void run() {
        try {
            if (cancelled.get()) return;
            if (Instant.now().isAfter(deadline)) {
                cancelled.set(true);
                return;
            }

            URI uri = URI.create(task.normalizedUrl());
            if (!robotsPolicy.isAllowed(uri)) {
                return;
            }

            if (!perHostScheduler.beforeRequest(uri)) {
                return;
            }

            HttpFetcher.Response response = httpFetcher.fetch(uri);
            if (!response.isSuccessHtml()) {
                return;
            }

            int pageIndex = pagesCrawled.incrementAndGet();

            String body = response.body();
            ExtractedDocument doc = contentExtractor.extract(
                    htmlParser.parse(body, uri.toString()),
                    uri.toString(),
                    response.effectiveUri().toString(),
                    task.depth()
            );

            List<OutputChunk> chunks = chunker.chunk(doc);
            for (OutputChunk chunk : chunks) {
                if (deduplicator.isDuplicate(chunk.chunkHash())) {
                    continue;
                }
                writer.writeChunk(chunk);
            }

            if (config.maxPages() != null && pageIndex >= config.maxPages()) {
                cancelled.set(true);
            }

            List<String> links = htmlParser.extractLinks(body, uri.toString());
            for (String link : links) {
                if (cancelled.get()) break;
                Optional<String> norm = normalizer.normalizeIfSameHost(link);
                if (norm.isEmpty()) continue;
                int nextDepth = task.depth() + 1;
                if (config.maxDepth() != null && nextDepth > config.maxDepth()) {
                    continue;
                }
                frontier.offer(new UrlFrontier.Task(norm.get(), nextDepth));
            }

        } catch (Exception e) {
            log.warn("Error while crawling {}: {}", task.normalizedUrl(), e.toString());
        } finally {
            inFlight.decrementAndGet();
        }
    }
}

