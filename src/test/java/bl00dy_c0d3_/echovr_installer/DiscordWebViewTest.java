package bl00dy_c0d3_.echovr_installer;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class DiscordWebViewTest {

    @Test
    void testWebViewCreated() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        DiscordWebView view = new DiscordWebView();
        assertNotNull(view);
        view.close();
    }

    @Test
    void testNavigateToUrl() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        DiscordWebView view = new DiscordWebView();
        assertDoesNotThrow(() -> view.navigateTo("https://example.com"));
        // WebEngine normalizes URLs (adds trailing slash)
        String location = view.getWebEngine().getLocation();
        assertTrue(location.startsWith("https://example.com"),
                "Expected location to start with https://example.com but was: " + location);
        view.close();
    }

    @Test
    void testGetWebEngine() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        DiscordWebView view = new DiscordWebView();
        assertNotNull(view.getWebEngine());
        view.close();
    }

    @Test
    void testCloseDisposesStage() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        DiscordWebView view = new DiscordWebView();
        assertDoesNotThrow(view::close);
        assertFalse(Platform.isImplicitExit());
    }

    @Test
    void testImplicitExitFalse() {
        // Force DiscordWebView static initializer to run
        Class<?> clazz = DiscordWebView.class;
        assertFalse(Platform.isImplicitExit(),
                "Platform.setImplicitExit(false) must be set by DiscordWebView static initializer");
    }
}
