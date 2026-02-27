package dev.ragcrawler.crawler.parsing;

import java.time.Instant;
import java.util.List;

public record ExtractedDocument(
        String url,
        String canonicalUrl,
        String rootUrl,
        String title,
        List<String> headings,
        List<LogicalBlock> blocks,
        int depth,
        Instant crawledAt
) {
}

