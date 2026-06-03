package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectorConfigTest {

    private SelectorConfig config;

    @BeforeEach
    void setUp() {
        config = new SelectorConfig();
    }

    @Test
    void testServerInvite() {
        assertEquals("https://discord.gg/KqjqdNUaHR", config.getServerInvite());
    }

    @Test
    void testChannelName() {
        assertEquals("quest-patch", config.getChannelName());
    }

    @Test
    void testChannelSelector() {
        String selector = config.getChannelSelector();
        assertNotNull(selector);
        assertFalse(selector.isEmpty());
    }

    @Test
    void testReactionEmoji() {
        assertNotNull(config.getReactionEmoji());
    }

    @Test
    void testMessageSelector() {
        String selector = config.getMessageSelector();
        assertNotNull(selector);
        assertFalse(selector.isEmpty());
    }

    @Test
    void testThreadSelector() {
        String selector = config.getThreadSelector();
        assertNotNull(selector);
        assertFalse(selector.isEmpty());
    }

    @Test
    void testUrlPattern() {
        String pattern = config.getUrlPattern();
        assertNotNull(pattern);
        assertFalse(pattern.isEmpty());
    }

    @Test
    void testTimeoutSeconds() {
        assertEquals(30, config.getTimeoutSeconds());
    }

    @Test
    void testAllGettersReturnNonNull() {
        assertNotNull(config.getServerInvite());
        assertNotNull(config.getChannelName());
        assertNotNull(config.getChannelSelector());
        assertNotNull(config.getReactionEmoji());
        assertNotNull(config.getMessageSelector());
        assertNotNull(config.getThreadSelector());
        assertNotNull(config.getUrlPattern());
        assertNotNull(config.getTimeoutSeconds());
    }

    @Test
    void testDefaultConstructorDoesNotThrow() {
        assertDoesNotThrow(() -> new SelectorConfig());
    }
}
