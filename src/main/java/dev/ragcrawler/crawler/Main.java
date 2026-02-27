package dev.ragcrawler.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            CliOptions options = CliOptions.parse(args);
            validateOptions(options);

            URI rootUri = URI.create(options.url());
            Duration maxTime = Duration.ofSeconds(options.maxTimeSeconds());
            Path outputPath = Path.of(options.outputPath());

            CrawlConfig config = new CrawlConfig(
                    rootUri,
                    maxTime,
                    options.maxPages(),
                    options.maxDepth(),
                    options.perHostConcurrency(),
                    options.perHostMinDelayMillis(),
                    options.userAgent()
            );

            CrawlerApplication app = new CrawlerApplication(config, outputPath);
            app.run();
        } catch (CliOptions.CliException e) {
            System.err.println("Argument error: " + e.getMessage());
            CliOptions.printUsage(System.err);
            System.exit(2);
        } catch (Exception e) {
            log.error("Crawler failed with unexpected error", e);
            System.exit(1);
        }
    }

    private static void validateOptions(CliOptions opts) throws CliOptions.CliException {
        if (opts.url() == null) {
            throw new CliOptions.CliException("--url is required");
        }
        if (opts.outputPath() == null) {
            throw new CliOptions.CliException("--output is required");
        }
        if (opts.maxTimeSeconds() <= 0) {
            throw new CliOptions.CliException("--maxtime must be > 0");
        }
        if (opts.maxPages() != null && opts.maxPages() <= 0) {
            throw new CliOptions.CliException("--maxPages must be > 0");
        }
        if (opts.maxDepth() != null && opts.maxDepth() < 0) {
            throw new CliOptions.CliException("--maxDepth must be >= 0");
        }
    }
}

