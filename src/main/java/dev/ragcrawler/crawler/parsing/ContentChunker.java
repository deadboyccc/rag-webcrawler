package dev.ragcrawler.crawler.parsing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ContentChunker {

    private static final int DEFAULT_MAX_CHARS = 1500;

    public List<OutputChunk> chunk(ExtractedDocument doc) {
        List<OutputChunk> out = new ArrayList<>();
        List<LogicalBlock> blocks = doc.blocks();
        if (blocks.isEmpty()) {
            return out;
        }

        List<String> currentLines = new ArrayList<>();
        List<String> currentBlockTypes = new ArrayList<>();
        String currentCodeLang = null;
        int charCount = 0;

        for (LogicalBlock block : blocks) {
            String text = block.text();
            if (text == null || text.isBlank()) continue;

            if (block.type() == LogicalBlock.BlockType.CODE) {
                if (!currentLines.isEmpty()) {
                    out.add(buildChunk(doc, out.size(), joinLines(currentLines), currentBlockTypes,
                            currentCodeLang));
                    currentLines.clear();
                    currentBlockTypes.clear();
                    currentCodeLang = null;
                    charCount = 0;
                }
                out.add(buildChunk(doc, out.size(), text,
                        List.of("code"),
                        block.codeLanguage()));
                continue;
            }

            String toAdd = text + "\n\n";
            if (charCount + toAdd.length() > DEFAULT_MAX_CHARS && !currentLines.isEmpty()) {
                out.add(buildChunk(doc, out.size(), joinLines(currentLines), currentBlockTypes,
                        currentCodeLang));
                currentLines.clear();
                currentBlockTypes.clear();
                currentCodeLang = null;
                charCount = 0;
            }
            currentLines.add(text);
            currentBlockTypes.add(block.type().name().toLowerCase());
            charCount += toAdd.length();
        }

        if (!currentLines.isEmpty()) {
            out.add(buildChunk(doc, out.size(), joinLines(currentLines), currentBlockTypes,
                    currentCodeLang));
        }

        int total = out.size();
        List<OutputChunk> adjusted = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            OutputChunk c = out.get(i);
            adjusted.add(new OutputChunk(
                    c.id(),
                    c.url(),
                    c.canonicalUrl(),
                    c.rootUrl(),
                    c.title(),
                    c.headings(),
                    i,
                    total,
                    c.content(),
                    c.contentType(),
                    c.blockTypes(),
                    c.codeLanguage(),
                    c.pageHash(),
                    c.chunkHash(),
                    c.depth(),
                    c.hPath(),
                    c.lang(),
                    c.crawledAt(),
                    c.source(),
                    c.metadata()
            ));
        }
        return adjusted;
    }

    private static OutputChunk buildChunk(ExtractedDocument doc,
                                          int chunkIndex,
                                          String content,
                                          List<String> blockTypes,
                                          String codeLang) {
        String pageHash = sha256(doc.url());
        String chunkHash = sha256(doc.url() + ":" + chunkIndex + ":" + content);
        String contentType = (blockTypes.size() == 1 && blockTypes.contains("code"))
                ? "code"
                : "text";
        return new OutputChunk(
                UUID.randomUUID().toString(),
                doc.url(),
                doc.canonicalUrl(),
                doc.rootUrl(),
                doc.title(),
                doc.headings(),
                chunkIndex,
                0,
                content,
                contentType,
                blockTypes,
                codeLang,
                pageHash,
                chunkHash,
                doc.depth(),
                doc.headings(),
                "en",
                doc.crawledAt(),
                "web-docs",
                Map.of(
                        "status_code", 200,
                        "content_type_header", "text/html"
                )
        );
    }

    private static String joinLines(List<String> lines) {
        return lines.stream().collect(Collectors.joining("\n\n"));
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

