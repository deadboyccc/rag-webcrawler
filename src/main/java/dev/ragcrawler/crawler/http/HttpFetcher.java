package dev.ragcrawler.crawler.http;

import dev.ragcrawler.crawler.CrawlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HttpFetcher implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcher.class);

    private final HttpClient client;
    private final CrawlConfig config;
    private final Instant deadline;
    private final AtomicBoolean cancelled;

    public HttpFetcher(CrawlConfig config, Instant deadline, AtomicBoolean cancelled) {
        this.config = config;
        this.deadline = deadline;
        this.cancelled = cancelled;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Response fetch(URI uri) throws IOException, InterruptedException {
        int maxAttempts = 3;
        int attempt = 0;
        IOException lastIo = null;
        while (attempt < maxAttempts && !cancelled.get()) {
            attempt++;
            if (Instant.now().isAfter(deadline)) {
                cancelled.set(true);
                break;
            }
            Duration remaining = Duration.between(Instant.now(), deadline);
            if (remaining.isNegative() || remaining.isZero()) {
                cancelled.set(true);
                break;
            }
            Duration timeout = remaining.compareTo(Duration.ofSeconds(10)) < 0
                    ? remaining
                    : Duration.ofSeconds(10);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", config.userAgent())
                    .timeout(timeout)
                    .GET()
                    .build();

            try {
                HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();
                if (code >= 200 && code < 300) {
                    return new Response(uri, resp.uri(), code,
                            resp.headers().firstValue("Content-Type").orElse(""),
                            resp.body());
                }
                if (code == 502 || code == 503 || code == 504) {
                    Thread.sleep(200L * attempt);
                    continue;
                }
                return new Response(uri, resp.uri(), code,
                        resp.headers().firstValue("Content-Type").orElse(""),
                        resp.body());
            } catch (IOException e) {
                lastIo = e;
                log.warn("HTTP attempt {} failed for {}: {}", attempt, uri, e.toString());
                Thread.sleep(200L * attempt);
            }
        }
        if (lastIo != null) {
            throw lastIo;
        }
        throw new IOException("Cancelled or deadline exceeded before successful fetch: " + uri);
    }

    @Override
    public void close() {
        // HttpClient does not need explicit close.
    }

    public record Response(
            URI requestedUri,
            URI effectiveUri,
            int statusCode,
            String contentType,
            String body
    ) {
        public boolean isSuccessHtml() {
            if (statusCode < 200 || statusCode >= 300) return false;
            String ct = contentType.toLowerCase();
            return ct.contains("text/html");
        }
    }
}

