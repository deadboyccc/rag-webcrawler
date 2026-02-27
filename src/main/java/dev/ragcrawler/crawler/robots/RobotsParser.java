package dev.ragcrawler.crawler.robots;

import dev.ragcrawler.crawler.robots.RobotsCache.RobotsRules;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RobotsParser {

    private RobotsParser() {}

    static RobotsRules parse(String body, String userAgent) {
        String[] lines = body.split("\\R");
        String agentLower = userAgent.toLowerCase(Locale.ROOT);
        List<String> disallows = new ArrayList<>();
        Duration crawlDelay = Duration.ZERO;

        boolean inRelevantSection = false;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("user-agent:")) {
                String value = line.substring("user-agent:".length()).trim();
                String vLower = value.toLowerCase(Locale.ROOT);
                inRelevantSection = vLower.equals("*") || agentLower.contains(vLower);
                continue;
            }
            if (!inRelevantSection) continue;

            if (lower.startsWith("disallow:")) {
                String path = line.substring("disallow:".length()).trim();
                disallows.add(path);
            } else if (lower.startsWith("crawl-delay:")) {
                String v = line.substring("crawl-delay:".length()).trim();
                try {
                    long seconds = Long.parseLong(v);
                    crawlDelay = Duration.ofSeconds(seconds);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new RobotsRules(disallows, crawlDelay);
    }
}

