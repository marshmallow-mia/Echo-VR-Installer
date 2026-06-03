# OAuth2 Status Progress — Show "Patching..." During Server Wait

## Problem

The status bar shows "Discord authorization opened in your browser. Complete it there." for the entire OAuth2 flow including the long server-side file generation (up to 1 minute). User has no indication that authorization succeeded and the patch is being generated.

## Fix

3-phrase status progression visible in the status bar:

| Phase | Status text | When |
|-------|-------------|------|
| 1 | "Discord authorization opened in your browser." | Button pressed → user authorizing |
| 2 | "Generating your patch file..." | Callback received → server generating |
| 3 | "Downloading patch file..." | URL received → downloading |

## Changes

### `DiscordOAuth2Flow.java` — add status callback

Add `Consumer<String>` parameter to `start()` and call it at phase transitions:

```java
public CompletableFuture<String> start(Consumer<String> onStatus) {
    ...
    // Phase 1: waiting for callback
    SwingUtilities.invokeLater(() -> onStatus.accept("Discord authorization opened in your browser."));
    
    // Open browser, start callback server
    Desktop.getDesktop().browse(new URI(authUrl));
    String authCode = waitForCallback(serverSocket);
    
    if (authCode == null) { ... }
    
    // Phase 2: exchanging code (server generating)
    SwingUtilities.invokeLater(() -> onStatus.accept("Generating your patch file..."));
    
    String patchUrl = exchangeCode(authCode);
    
    // Phase 3: caller handles download
    ...
}
```

### `FrameGuidance.java` — pass status callback

Replace:
```java
dlProgressLabel.setText("Discord authorization opened in your browser. Complete it there.");
DiscordOAuth2Flow flow = new DiscordOAuth2Flow("dll");
String patchUrl = flow.start().get(300, TimeUnit.SECONDS);
```

With:
```java
DiscordOAuth2Flow flow = new DiscordOAuth2Flow("dll");
String patchUrl = flow.start(status -> 
    SwingUtilities.invokeLater(() -> dlProgressLabel.setText(status))
).get(300, TimeUnit.SECONDS);
```

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Run installer, authorize: status bar shows "Discord authorization opened..." then → "Generating your patch file..." when browser redirects → "Downloading patch file..." when URL received → "Patch applied successfully!"
