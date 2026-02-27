package dev.ragcrawler.crawler.parsing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public final class HtmlParser {

    public Document parse(String html, String baseUri) {
        return Jsoup.parse(html, baseUri);
    }

    public List<String> extractLinks(String html, String baseUri) {
        Document doc = Jsoup.parse(html, baseUri);
        List<String> links = new ArrayList<>();
        Elements anchors = doc.select("a[href]");
        for (Element a : anchors) {
            String href = a.attr("href");
            if (href == null || href.isBlank()) continue;
            links.add(href);
        }
        return links;
    }
}

