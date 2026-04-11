package com.openelements.crm.contact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialNetworkTypeTest {

    @Nested
    @DisplayName("GITHUB")
    class GitHub {

        @Test
        @DisplayName("resolves username")
        void username() {
            var r = SocialNetworkType.GITHUB.resolve("hendrikebbers");
            assertEquals("hendrikebbers", r.value());
            assertEquals("https://github.com/hendrikebbers", r.url());
        }

        @Test
        @DisplayName("strips @ prefix")
        void atPrefix() {
            var r = SocialNetworkType.GITHUB.resolve("@hendrikebbers");
            assertEquals("hendrikebbers", r.value());
            assertEquals("https://github.com/hendrikebbers", r.url());
        }

        @Test
        @DisplayName("extracts from full URL")
        void fullUrl() {
            var r = SocialNetworkType.GITHUB.resolve("https://github.com/hendrikebbers");
            assertEquals("hendrikebbers", r.value());
            assertEquals("https://github.com/hendrikebbers", r.url());
        }

        @Test
        @DisplayName("rejects empty")
        void empty() {
            assertThrows(ResponseStatusException.class, () -> SocialNetworkType.GITHUB.resolve(""));
        }
    }

    @Nested
    @DisplayName("LINKEDIN")
    class LinkedIn {

        @Test
        @DisplayName("resolves username")
        void username() {
            var r = SocialNetworkType.LINKEDIN.resolve("hendrik-ebbers");
            assertEquals("hendrik-ebbers", r.value());
            assertEquals("https://linkedin.com/in/hendrik-ebbers", r.url());
        }

        @Test
        @DisplayName("strips /in/ prefix")
        void inPrefix() {
            var r = SocialNetworkType.LINKEDIN.resolve("/in/hendrik-ebbers");
            assertEquals("hendrik-ebbers", r.value());
            assertEquals("https://linkedin.com/in/hendrik-ebbers", r.url());
        }

        @Test
        @DisplayName("extracts from full URL")
        void fullUrl() {
            var r = SocialNetworkType.LINKEDIN.resolve("https://linkedin.com/in/hendrik-ebbers");
            assertEquals("hendrik-ebbers", r.value());
            assertEquals("https://linkedin.com/in/hendrik-ebbers", r.url());
        }

        @Test
        @DisplayName("rejects wrong domain URL")
        void wrongDomain() {
            assertThrows(ResponseStatusException.class,
                    () -> SocialNetworkType.LINKEDIN.resolve("https://github.com/hendrik"));
        }
    }

    @Nested
    @DisplayName("X")
    class XNetwork {

        @Test
        @DisplayName("resolves username")
        void username() {
            var r = SocialNetworkType.X.resolve("hendrikEbworx");
            assertEquals("hendrikEbworx", r.value());
            assertEquals("https://x.com/hendrikEbworx", r.url());
        }

        @Test
        @DisplayName("strips @ prefix")
        void atPrefix() {
            var r = SocialNetworkType.X.resolve("@hendrikEbworx");
            assertEquals("hendrikEbworx", r.value());
            assertEquals("https://x.com/hendrikEbworx", r.url());
        }

        @Test
        @DisplayName("extracts from full URL")
        void fullUrl() {
            var r = SocialNetworkType.X.resolve("https://x.com/hendrikEbworx");
            assertEquals("hendrikEbworx", r.value());
            assertEquals("https://x.com/hendrikEbworx", r.url());
        }

        @Test
        @DisplayName("extracts from twitter.com URL")
        void twitterUrl() {
            var r = SocialNetworkType.X.resolve("https://twitter.com/hendrikEbworx");
            assertEquals("hendrikEbworx", r.value());
            assertEquals("https://x.com/hendrikEbworx", r.url());
        }
    }

    @Nested
    @DisplayName("MASTODON")
    class Mastodon {

        @Test
        @DisplayName("resolves full handle")
        void fullHandle() {
            var r = SocialNetworkType.MASTODON.resolve("@hendrik@mastodon.social");
            assertEquals("@hendrik@mastodon.social", r.value());
            assertEquals("https://mastodon.social/@hendrik", r.url());
        }

        @Test
        @DisplayName("extracts from full URL")
        void fullUrl() {
            var r = SocialNetworkType.MASTODON.resolve("https://mastodon.social/@hendrik");
            assertEquals("@hendrik@mastodon.social", r.value());
            assertEquals("https://mastodon.social/@hendrik", r.url());
        }

        @Test
        @DisplayName("rejects handle without instance")
        void noInstance() {
            assertThrows(ResponseStatusException.class,
                    () -> SocialNetworkType.MASTODON.resolve("@hendrik"));
        }
    }

    @Nested
    @DisplayName("BLUESKY")
    class BlueSky {

        @Test
        @DisplayName("resolves standard handle")
        void standardHandle() {
            var r = SocialNetworkType.BLUESKY.resolve("hendrik.bsky.social");
            assertEquals("hendrik.bsky.social", r.value());
            assertEquals("https://bsky.app/profile/hendrik.bsky.social", r.url());
        }

        @Test
        @DisplayName("resolves custom domain")
        void customDomain() {
            var r = SocialNetworkType.BLUESKY.resolve("hendrik.openelements.com");
            assertEquals("hendrik.openelements.com", r.value());
            assertEquals("https://bsky.app/profile/hendrik.openelements.com", r.url());
        }

        @Test
        @DisplayName("rejects handle without domain")
        void noDomain() {
            assertThrows(ResponseStatusException.class,
                    () -> SocialNetworkType.BLUESKY.resolve("hendrik"));
        }
    }

    @Nested
    @DisplayName("DISCORD")
    class Discord {

        @Test
        @DisplayName("resolves numeric ID")
        void numericId() {
            var r = SocialNetworkType.DISCORD.resolve("123456789012345678");
            assertEquals("123456789012345678", r.value());
            assertEquals("https://discord.com/users/123456789012345678", r.url());
        }

        @Test
        @DisplayName("rejects non-numeric input")
        void nonNumeric() {
            assertThrows(ResponseStatusException.class,
                    () -> SocialNetworkType.DISCORD.resolve("hendrik#1234"));
        }
    }

    @Nested
    @DisplayName("YOUTUBE")
    class YouTube {

        @Test
        @DisplayName("resolves @handle")
        void atHandle() {
            var r = SocialNetworkType.YOUTUBE.resolve("@hendrikebbers");
            assertEquals("hendrikebbers", r.value());
            assertEquals("https://youtube.com/@hendrikebbers", r.url());
        }

        @Test
        @DisplayName("resolves handle without @")
        void plainHandle() {
            var r = SocialNetworkType.YOUTUBE.resolve("hendrikebbers");
            assertEquals("hendrikebbers", r.value());
            assertEquals("https://youtube.com/@hendrikebbers", r.url());
        }

        @Test
        @DisplayName("extracts from full URL")
        void fullUrl() {
            var r = SocialNetworkType.YOUTUBE.resolve("https://youtube.com/@hendrikebbers");
            assertEquals("hendrikebbers", r.value());
            assertEquals("https://youtube.com/@hendrikebbers", r.url());
        }
    }

    @Nested
    @DisplayName("WEBSITE")
    class Website {

        @Test
        @DisplayName("accepts full https URL")
        void fullHttpsUrl() {
            var r = SocialNetworkType.WEBSITE.resolve("https://open-elements.com");
            assertEquals("https://open-elements.com", r.value());
            assertEquals("https://open-elements.com", r.url());
        }

        @Test
        @DisplayName("accepts full http URL")
        void fullHttpUrl() {
            var r = SocialNetworkType.WEBSITE.resolve("http://example.org");
            assertEquals("http://example.org", r.value());
            assertEquals("http://example.org", r.url());
        }

        @Test
        @DisplayName("rejects URL without protocol")
        void noProtocol() {
            assertThrows(ResponseStatusException.class,
                    () -> SocialNetworkType.WEBSITE.resolve("example"));
        }
    }

    @Test
    @DisplayName("valueOf rejects unknown network type")
    void unknownNetworkType() {
        assertThrows(IllegalArgumentException.class,
                () -> SocialNetworkType.valueOf("INSTAGRAM"));
    }
}
