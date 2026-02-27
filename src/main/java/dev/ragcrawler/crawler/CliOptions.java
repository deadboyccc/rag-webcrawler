package dev.ragcrawler.crawler;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal CLI options parser to avoid extra dependencies.
 */
public record CliOptions(
        String url,
        long maxTimeSeconds,
        Integer maxPages,
        Integer maxDepth,
        String outputPath,
        int perHostConcurrency,
        long perHostMinDelayMillis,
        String userAgent
) {

    public static final long DEFAULT_MAX_TIME_SECONDS = 20;
    public static final int DEFAULT_PER_HOST_CONCURRENCY = 4;
    public static final long DEFAULT_PER_HOST_MIN_DELAY_MILLIS = 250;
    public static final String DEFAULT_USER_AGENT = "rag-webcrawler/0.1";

    public static CliOptions parse(String[] args) throws CliException {
        Map<String, String> flags = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg;
                String value = null;
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                }
                flags.put(key, value);
            }
        }

        String url = flags.get("--url");
        String output = flags.get("--output");

        long maxTime = parseLongOrDefault(flags.get("--maxtime"), DEFAULT_MAX_TIME_SECONDS);
        Integer maxPages = parseNullableInt(flags.get("--maxPages"));
        Integer maxDepth = parseNullableInt(flags.get("--maxDepth"));
        int perHostConcurrency = (int) parseLongOrDefault(flags.get("--perHostConcurrency"),
                DEFAULT_PER_HOST_CONCURRENCY);
        long perHostMinDelayMillis = parseLongOrDefault(flags.get("--perHostMinDelayMillis"),
                DEFAULT_PER_HOST_MIN_DELAY_MILLIS);
        String userAgent = flags.getOrDefault("--userAgent", DEFAULT_USER_AGENT);

        return new CliOptions(
                url,
                maxTime,
                maxPages,
                maxDepth,
                output,
                perHostConcurrency,
                perHostMinDelayMillis,
                userAgent
        );
    }

    private static long parseLongOrDefault(String value, long defaultVal) throws CliException {
        if (value == null) return defaultVal;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CliException("Invalid numeric value: " + value);
        }
    }

    private static Integer parseNullableInt(String value) throws CliException {
        if (value == null) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new CliException("Invalid integer value: " + value);
        }
    }

    public static void printUsage(PrintStream out) {
        out.println("Usage: java -jar rag-webcrawler.jar --url <URL> --output <FILE> [options]");
        out.println("Options:");
        out.println("  --maxtime <seconds>            Max crawl time (default " + DEFAULT_MAX_TIME_SECONDS + ")");
        out.println("  --maxPages <n>                 Max number of pages to crawl");
        out.println("  --maxDepth <n>                 Max crawl depth from root (0 = only root)");
        out.println("  --perHostConcurrency <n>       Max concurrent requests per host (default "
                + DEFAULT_PER_HOST_CONCURRENCY + ")");
        out.println("  --perHostMinDelayMillis <ms>   Minimum delay between requests per host (default "
                + DEFAULT_PER_HOST_MIN_DELAY_MILLIS + "ms)");
        out.println("  --userAgent <string>           User agent string (default " + DEFAULT_USER_AGENT + ")");
    }

    public static class CliException extends Exception {
        public CliException(String message) {
            super(message);
        }
    }
}

