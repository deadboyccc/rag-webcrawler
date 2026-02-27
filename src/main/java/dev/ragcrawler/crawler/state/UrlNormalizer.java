package dev.ragcrawler.crawler.state;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Normalizes URLs and enforces same-host policy.
 */
public final class UrlNormalizer {

    private final URI root;
    private final String rootHost;
    private final String rootScheme;
    private final int rootPort;

    public UrlNormalizer(URI root) {
        this.root = Objects.requireNonNull(root);
        this.rootHost = root.getHost();
        this.rootScheme = root.getScheme();
        this.rootPort = effectivePort(root);
    }

    public String normalize(URI uri) {
        URI noFrag = stripFragment(uri);
        return noFrag.normalize().toString();
    }

    public String normalize(String url) {
        return normalize(URI.create(url));
    }

    public Optional<String> normalizeIfSameHost(String candidate) {
        URI abs = toAbsolute(candidate);
        if (abs == null) return Optional.empty();
        if (!isSameHost(abs)) return Optional.empty();
        return Optional.of(normalize(abs));
    }

    private URI toAbsolute(String url) {
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute()) {
                uri = root.resolve(uri);
            }
            return uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private boolean isSameHost(URI uri) {
        if (!Objects.equals(rootScheme.toLowerCase(Locale.ROOT),
                nullToEmpty(uri.getScheme()).toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!Objects.equals(rootHost, uri.getHost())) {
            return false;
        }
        return effectivePort(uri) == rootPort;
    }

    private static int effectivePort(URI uri) {
        int port = uri.getPort();
        if (port != -1) return port;
        String scheme = nullToEmpty(uri.getScheme()).toLowerCase(Locale.ROOT);
        return switch (scheme) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }

    private static URI stripFragment(URI uri) {
        try {
            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );
        } catch (URISyntaxException e) {
            return uri;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

