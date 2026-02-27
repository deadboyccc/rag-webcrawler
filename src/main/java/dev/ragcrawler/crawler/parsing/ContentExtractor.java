package dev.ragcrawler.crawler.parsing;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ContentExtractor {

    public ExtractedDocument extract(Document doc,
                                     String url,
                                     String effectiveUrl,
                                     int depth) {
        String canonical = null;
        Element canonicalEl = doc.selectFirst("link[rel=canonical]");
        if (canonicalEl != null) {
            canonical = canonicalEl.attr("abs:href");
        }

        String title = doc.title();
        List<String> headings = new ArrayList<>();
        List<LogicalBlock> blocks = new ArrayList<>();

        Elements headingEls = doc.select("h1, h2, h3, h4");
        for (Element h : headingEls) {
            String text = h.text().trim();
            if (!text.isEmpty()) {
                headings.add(text);
                blocks.add(new LogicalBlock(LogicalBlock.BlockType.HEADING, text, null));
            }
        }

        Elements paraEls = doc.select("p");
        for (Element p : paraEls) {
            String text = p.text().trim();
            if (!text.isEmpty()) {
                blocks.add(new LogicalBlock(LogicalBlock.BlockType.PARAGRAPH, text, null));
            }
        }

        Elements listEls = doc.select("ul, ol");
        for (Element list : listEls) {
            String text = list.text().trim();
            if (!text.isEmpty()) {
                blocks.add(new LogicalBlock(LogicalBlock.BlockType.LIST, text, null));
            }
        }

        Elements codeEls = doc.select("pre, code");
        for (Element code : codeEls) {
            String text = code.text().trim();
            if (text.isEmpty()) continue;
            String lang = null;
            String classAttr = code.className();
            if (classAttr != null && !classAttr.isBlank()) {
                lang = classAttr;
            }
            blocks.add(new LogicalBlock(LogicalBlock.BlockType.CODE, text, lang));
        }

        return new ExtractedDocument(
                effectiveUrl,
                canonical,
                url,
                title,
                headings,
                blocks,
                depth,
                Instant.now()
        );
    }
}

