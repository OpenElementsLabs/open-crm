package com.openelements.crm.contact;

import java.util.regex.Pattern;

/**
 * Supported social network types with URL construction and validation logic.
 */
public enum SocialNetworkType {

    GITHUB("github.com") {
        @Override
        public ResolvedLink resolve(final String input) {
            if (isFullUrl(input)) {
                requireDomain(input, "github.com");
                final String username = extractPathSegment(input, 1);
                return new ResolvedLink(username, "https://github.com/" + username);
            }
            final String username = stripAtPrefix(input);
            requireNonEmpty(username, "GitHub username");
            return new ResolvedLink(username, "https://github.com/" + username);
        }
    },

    LINKEDIN("linkedin.com") {
        @Override
        public ResolvedLink resolve(final String input) {
            if (isFullUrl(input)) {
                requireDomain(input, "linkedin.com");
                final String path = extractPath(input);
                final String slug;
                if (path.startsWith("/in/")) {
                    slug = path.substring(4).replaceAll("/+$", "");
                } else {
                    slug = extractPathSegment(input, 1);
                }
                return new ResolvedLink(slug, "https://linkedin.com/in/" + slug);
            }
            String slug = input.trim();
            if (slug.startsWith("/in/")) {
                slug = slug.substring(4);
            }
            requireNonEmpty(slug, "LinkedIn slug");
            return new ResolvedLink(slug, "https://linkedin.com/in/" + slug);
        }
    },

    X("x.com") {
        @Override
        public ResolvedLink resolve(final String input) {
            if (isFullUrl(input)) {
                requireDomain(input, "x.com", "twitter.com");
                final String username = extractPathSegment(input, 1);
                return new ResolvedLink(username, "https://x.com/" + username);
            }
            final String username = stripAtPrefix(input);
            requireNonEmpty(username, "X username");
            return new ResolvedLink(username, "https://x.com/" + username);
        }
    },

    MASTODON(null) {
        private static final Pattern HANDLE_PATTERN = Pattern.compile("^@([^@]+)@(.+)$");

        @Override
        public ResolvedLink resolve(final String input) {
            if (isFullUrl(input)) {
                final String host = extractHost(input);
                final String path = extractPath(input);
                if (!path.startsWith("/@")) {
                    throw badRequest("Mastodon URL must contain /@username path");
                }
                final String user = path.substring(2).replaceAll("/+$", "");
                return new ResolvedLink("@" + user + "@" + host, "https://" + host + "/@" + user);
            }
            final var matcher = HANDLE_PATTERN.matcher(input.trim());
            if (!matcher.matches()) {
                throw badRequest("Mastodon handle must be in @user@instance format");
            }
            final String user = matcher.group(1);
            final String instance = matcher.group(2);
            return new ResolvedLink("@" + user + "@" + instance, "https://" + instance + "/@" + user);
        }
    },

    BLUESKY("bsky.app") {
        @Override
        public ResolvedLink resolve(final String input) {
            if (isFullUrl(input)) {
                requireDomain(input, "bsky.app");
                final String handle = extractPathAfterPrefix(input, "/profile/");
                return new ResolvedLink(handle, "https://bsky.app/profile/" + handle);
            }
            final String handle = input.trim();
            if (!handle.contains(".")) {
                throw badRequest("BlueSky handle must include a domain (e.g., handle.bsky.social)");
            }
            return new ResolvedLink(handle, "https://bsky.app/profile/" + handle);
        }
    },

    DISCORD("discord.com") {
        private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

        @Override
        public ResolvedLink resolve(final String input) {
            if (isFullUrl(input)) {
                requireDomain(input, "discord.com");
                final String id = extractPathAfterPrefix(input, "/users/");
                if (!NUMERIC_PATTERN.matcher(id).matches()) {
                    throw badRequest("Discord user ID must be numeric");
                }
                return new ResolvedLink(id, "https://discord.com/users/" + id);
            }
            final String id = input.trim();
            if (!NUMERIC_PATTERN.matcher(id).matches()) {
                throw badRequest("Discord user ID must be numeric");
            }
            return new ResolvedLink(id, "https://discord.com/users/" + id);
        }
    },

    YOUTUBE("youtube.com") {
        @Override
        public ResolvedLink resolve(final String input) {
            if (isFullUrl(input)) {
                requireDomain(input, "youtube.com", "www.youtube.com");
                final String path = extractPath(input);
                final String handle;
                if (path.startsWith("/@")) {
                    handle = path.substring(2).replaceAll("/+$", "");
                } else {
                    handle = extractPathSegment(input, 1).replaceFirst("^@", "");
                }
                return new ResolvedLink(handle, "https://youtube.com/@" + handle);
            }
            final String handle = stripAtPrefix(input);
            requireNonEmpty(handle, "YouTube handle");
            return new ResolvedLink(handle, "https://youtube.com/@" + handle);
        }
    },

    WEBSITE(null) {
        @Override
        public ResolvedLink resolve(final String input) {
            final String trimmed = input.trim();
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                throw badRequest("Website URL must include protocol (http:// or https://)");
            }
            return new ResolvedLink(trimmed, trimmed);
        }
    };

    private final String expectedDomain;

    SocialNetworkType(final String expectedDomain) {
        this.expectedDomain = expectedDomain;
    }

    /**
     * Resolves user input into a normalized value and full URL.
     *
     * @param input the raw user input (username, handle, or full URL)
     * @return the resolved link with normalized value and full URL
     * @throws org.springframework.web.server.ResponseStatusException with 400 if input is invalid
     */
    public abstract ResolvedLink resolve(String input);

    public record ResolvedLink(String value, String url) {
    }

    // --- Utility methods ---

    static boolean isFullUrl(final String input) {
        return input.trim().startsWith("http://") || input.trim().startsWith("https://");
    }

    static String stripAtPrefix(final String input) {
        final String trimmed = input.trim();
        return trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
    }

    static void requireNonEmpty(final String value, final String fieldName) {
        if (value == null || value.isBlank()) {
            throw badRequest(fieldName + " must not be empty");
        }
    }

    static String extractHost(final String url) {
        try {
            final var uri = java.net.URI.create(url.trim());
            return uri.getHost();
        } catch (final Exception e) {
            throw badRequest("Invalid URL: " + url);
        }
    }

    static String extractPath(final String url) {
        try {
            final var uri = java.net.URI.create(url.trim());
            return uri.getPath() != null ? uri.getPath() : "";
        } catch (final Exception e) {
            throw badRequest("Invalid URL: " + url);
        }
    }

    static String extractPathSegment(final String url, final int index) {
        final String path = extractPath(url);
        final String[] segments = path.split("/");
        if (segments.length <= index) {
            throw badRequest("URL path does not contain expected segment");
        }
        return segments[index].replaceAll("/+$", "");
    }

    static String extractPathAfterPrefix(final String url, final String prefix) {
        final String path = extractPath(url);
        if (!path.startsWith(prefix)) {
            throw badRequest("URL path must start with " + prefix);
        }
        return path.substring(prefix.length()).replaceAll("/+$", "");
    }

    static void requireDomain(final String url, final String... expectedDomains) {
        final String host = extractHost(url);
        for (final String domain : expectedDomains) {
            if (domain.equalsIgnoreCase(host) || ("www." + domain).equalsIgnoreCase(host)) {
                return;
            }
        }
        throw badRequest("URL domain must be one of: " + String.join(", ", expectedDomains));
    }

    static org.springframework.web.server.ResponseStatusException badRequest(final String message) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, message);
    }
}
