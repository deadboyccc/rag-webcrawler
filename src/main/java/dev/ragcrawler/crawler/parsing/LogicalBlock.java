package dev.ragcrawler.crawler.parsing;

public record LogicalBlock(
        BlockType type,
        String text,
        String codeLanguage
) {
    public enum BlockType {
        HEADING,
        PARAGRAPH,
        LIST,
        CODE
    }
}

