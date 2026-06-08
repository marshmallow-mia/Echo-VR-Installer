package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DiscordNavigatorTest {

    private SelectorConfig config;

    @BeforeEach
    void setUp() {
        config = new SelectorConfig();
    }

    @Test
    void testNavigateToServer() {
        String[] capturedUrl = {null};
        DiscordNavigator nav = new DiscordNavigator(new DiscordWebView(), config) {
            @Override
            void runOnFxThread(Runnable action) {
                action.run();
            }
            @Override
            void doNavigate(String inviteUrl) {
                capturedUrl[0] = inviteUrl;
            }
        };
        nav.navigateToServer("https://discord.gg/test");
        assertEquals("https://discord.gg/test", capturedUrl[0]);
    }

    @Test
    void testFindChannel() {
        DiscordNavigator nav = new DiscordNavigator(new DiscordWebView(), config) {
            @Override
            Optional<String> executeScriptAndGet(String script) {
                return Optional.of("quest-patch");
            }
        };
        Optional<String> result = nav.findChannel("quest-patch");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("quest-patch", result.get());
    }

    @Test
    void testFindReactionMessage() {
        DiscordNavigator nav = new DiscordNavigator(new DiscordWebView(), config) {
            @Override
            Optional<String> executeScriptAndGet(String script) {
                return Optional.of("message with \uD83C\uDFAE reaction");
            }
        };
        Optional<String> result = nav.findReactionMessage();
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    void testDetectPrivateThread() {
        DiscordNavigator nav = new DiscordNavigator(new DiscordWebView(), config) {
            @Override
            Optional<String> executeScriptAndGet(String script) {
                return Optional.of("private-thread-content");
            }
        };
        Optional<String> result = nav.detectPrivateThread();
        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    void testExtractPatchUrl() {
        DiscordNavigator nav = new DiscordNavigator(new DiscordWebView(), config);
        String sampleUrl = "https://cdn.discordapp.com/attachments/123456789/987654321/pnsovr.dll";
        Optional<String> result = nav.extractPatchUrl(sampleUrl);
        assertTrue(result.isPresent(), "Should match CDN URL containing pnsovr.dll");
        assertTrue(result.get().contains("pnsovr.dll"), "Result should contain the filename");
    }

    @Test
    void testManualFallback() {
        DiscordNavigator nav = new DiscordNavigator(new DiscordWebView(), config) {
            @Override
            Optional<String> executeScriptAndGet(String script) {
                return Optional.empty();
            }
        };
        Optional<String> result = nav.waitForManualPaste(1);
        assertNotNull(result);
        assertFalse(result.isPresent(), "Should time out without paste input");
    }

    @Test
    void testTimeoutBehavior() {
        DiscordNavigator nav = new DiscordNavigator(new DiscordWebView(), config) {
            @Override
            public Optional<String> detectPrivateThread() {
                return Optional.empty();
            }
        };
        Optional<String> result = nav.detectPrivateThread(100, 1);
        assertNotNull(result);
        assertFalse(result.isPresent(), "Should time out without thread element");
    }
}
