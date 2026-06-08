package bl00dy_c0d3_.echovr_installer;

import javafx.application.Platform;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DiscordNavigator {

    private final DiscordWebView webView;
    private final SelectorConfig config;

    public DiscordNavigator(DiscordWebView webView, SelectorConfig config) {
        this.webView = webView;
        this.config = config;
    }

    public void navigateToServer(String inviteUrl) {
        runOnFxThread(() -> doNavigate(inviteUrl));
    }

    void runOnFxThread(Runnable action) {
        Platform.runLater(action);
    }

    void doNavigate(String inviteUrl) {
        webView.getWebEngine().load(inviteUrl);
    }

    public Optional<String> findChannel(String channelName) {
        String script = buildFindChannelScript(config.getChannelSelector(), channelName);
        return executeScriptAndGet(script);
    }

    public Optional<String> findReactionMessage() {
        String script = buildFindReactionScript(config.getMessageSelector(), config.getReactionEmoji());
        return executeScriptAndGet(script);
    }

    public void highlightReactionButton() {
        String script = buildHighlightScript(config.getMessageSelector());
        executeScript(script);
    }

    public Optional<String> detectPrivateThread() {
        String script = buildDetectThreadScript(config.getThreadSelector());
        return executeScriptAndGet(script);
    }

    public Optional<String> detectPrivateThread(int pollIntervalMs, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Optional<String> result = detectPrivateThread();
            if (result.isPresent()) {
                return result;
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<String> extractPatchUrl(String pageContent) {
        Pattern pattern = Pattern.compile(config.getUrlPattern());
        var matcher = pattern.matcher(pageContent);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }

    public Optional<String> waitForManualPaste(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Optional<String> result = executeScriptAndGet(buildPasteCheckScript());
            if (result.isPresent() && !result.get().isEmpty()) {
                return result;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    Optional<String> executeScriptAndGet(String script) {
        CountDownLatch latch = new CountDownLatch(1);
        Object[] result = {null};

        Platform.runLater(() -> {
            try {
                result[0] = webView.getWebEngine().executeScript(script);
            } catch (Exception e) {
                result[0] = e;
            } finally {
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(config.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                return Optional.empty();
            }
            if (result[0] instanceof Throwable) {
                return Optional.empty();
            }
            return result[0] != null ? Optional.of(String.valueOf(result[0])) : Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    void executeScript(String script) {
        Platform.runLater(() -> {
            try {
                webView.getWebEngine().executeScript(script);
            } catch (Exception ignored) {
            }
        });
    }

    static String buildFindChannelScript(String selector, String channelName) {
        return String.format(
            "(function() {" +
            "  var items = document.querySelectorAll('%s');" +
            "  for (var i = 0; i < items.length; i++) {" +
            "    if (items[i].textContent.includes('%s')) {" +
            "      items[i].click();" +
            "      return items[i].textContent.trim();" +
            "    }" +
            "  }" +
            "  return null;" +
            "})()",
            escapeJs(selector), escapeJs(channelName)
        );
    }

    static String buildFindReactionScript(String messageSelector, String emoji) {
        return String.format(
            "(function() {" +
            "  var messages = document.querySelectorAll('%s');" +
            "  for (var i = 0; i < messages.length; i++) {" +
            "    if (messages[i].textContent.includes('%s')) {" +
            "      return messages[i].textContent.trim();" +
            "    }" +
            "  }" +
            "  return null;" +
            "})()",
            escapeJs(messageSelector), escapeJs(emoji)
        );
    }

    static String buildHighlightScript(String messageSelector) {
        return String.format(
            "(function() {" +
            "  var messages = document.querySelectorAll('%s');" +
            "  for (var i = 0; i < messages.length; i++) {" +
            "    var buttons = messages[i].querySelectorAll('[class*=\"reaction\"]');" +
            "    for (var j = 0; j < buttons.length; j++) {" +
            "      buttons[j].style.border = '3px solid red';" +
            "      buttons[j].style.cursor = 'pointer';" +
            "    }" +
            "  }" +
            "})()",
            escapeJs(messageSelector)
        );
    }

    static String buildDetectThreadScript(String threadSelector) {
        return String.format(
            "(function() {" +
            "  var el = document.querySelector('%s');" +
            "  return el ? el.textContent.trim() : null;" +
            "})()",
            escapeJs(threadSelector)
        );
    }

    static String buildPasteCheckScript() {
        return "(function() {" +
               "  var el = document.activeElement;" +
               "  if (el) {" +
               "    return el.value || el.textContent || '';" +
               "  }" +
               "  return '';" +
               "})()";
    }

    private static String escapeJs(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
