package dev.ragcrawler.crawler.parsing;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OutputChunk(
        String id,
        String url,
        String canonicalUrl,
        String rootUrl,
        String title,
        List<String> headings,
        int chunkIndex,
        int chunkCount,
        String content,
        String contentType,
        List<String> blockTypes,
        String codeLanguage,
        String pageHash,
        String chunkHash,
        int depth,
        List<String> hPath,
        String lang,
        Instant crawledAt,
        String source,
        Map<String, Object> metadata
) {
}

