# Restore Auto-OAuth2 Licence Patch

## Goal

Replace the manual `buildLicencePatchDetail` (URL-paste page) with the auto OAuth2 flow for BOTH licence patch entry points. No matter how the user reaches the licence patch — via the Owner step or the AfterOAuth optional screen — it launches Discord OAuth2 in the system browser, fetches the patch URL from the server, and downloads automatically.

## Current State

- **Step 3** (new player): Uses `DiscordOAuth2Flow("dll")` — works correctly, auto-fetches URL and downloads ✓
- **Owner step 4** "No Licence Patch" button (line 430): Calls `showPatchDetail(1)` → manual `buildLicencePatchDetail` ✗
- **AfterOAuth** "No Licence Patch" button (line 588): Calls `showPatchDetail(1)` → manual `buildLicencePatchDetail` ✗
- `buildLicencePatchDetail` (lines 652-730): Manual page with URL input field, path chooser, "Start Patching" button with CDN regex validation ✗

## Root Cause

The `inline-all-patchers` plan wrongly switched `new FramePCPatcher(...)` to `showPatchDetail(1)`, which goes to the manual URL-paste page. The OLD `FramePCPatcher` had a Discord WebView that auto-logged in — but we now have the even better `DiscordOAuth2Flow` (system browser, stateless).

## Changes

### File: `FrameGuidance.java`

### 1. Extract `startLicenceOAuth2()` helper method

Pull the ~100 lines of OAuth2→download logic from step 3's `mouseReleased` handler (lines 470-556) into a shared method:

```java
private void startLicenceOAuth2(SpecialButton triggerBtn) {
    triggerBtn.setEnabled(false);
    nextBtn.setEnabled(false);
    stepInProgress = true;
    progressAnimator.start();

    new Thread(() -> {
        try {
            DiscordOAuth2Flow flow = new DiscordOAuth2Flow("dll");
            String patchUrl = flow.start(status -> dlProgressLabel.setText(status)).get(300, TimeUnit.SECONDS);

            System.out.println("Licence-OAuth2 SUCCESS: URL=" + patchUrl);
            SwingUtilities.invokeLater(() -> {
                dlProgressLabel.setText("Downloading patch file...");

                String ep = wizardState.getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10";
                if (!new java.io.File(ep).exists()) {
                    new ErrorDialog().errorDialog(FrameGuidance.this, "Wrong path", "Check your path", 0);
                    resetAfterError(triggerBtn);
                    return;
                }
                if (downloadPatch != null) { downloadPatch.cancelDownload(); pause(1); }
                downloadPatch = new Downloader();
                downloadPatch.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                    stepInProgress = false;
                    stepCompleted = true;
                    progressAnimator.stop();
                    nextBtn.setEnabled(true);
                    triggerBtn.setEnabled(true);
                    dlProgressLabel.setText("License patch applied!");
                    statusBarBox.repaint();
                }));
                downloadPatch.startDownload(patchUrl, ep, "pnsovr.dll",
                    new SpecialLabel(" 0%", 13), FrameGuidance.this, null, 3, true, -1, false);
            });
        } catch (java.util.concurrent.ExecutionException ex) {
            // same OAuth2Exception handling as step 3
            ...
            SwingUtilities.invokeLater(() -> {
                // not_in_guild → "Join Server First" dialog
                // busy → "Bot Busy" dialog
                // other → "Authorization Failed"
                resetAfterError(triggerBtn);
            });
        } catch (Exception ex) {
            ...
            SwingUtilities.invokeLater(() -> {
                new ErrorDialog().errorDialog(FrameGuidance.this, "Error",
                    "Timed out or cancelled. Try again.", 0);
                resetAfterError(triggerBtn);
            });
        }
    }).start();
}
```

### 2. Wire Owner step button

**Line 430**, change:
```java
public void mouseReleased(MouseEvent e) { showPatchDetail(1); }
```
To:
```java
public void mouseReleased(MouseEvent e) { startLicenceOAuth2(nl); }
```

### 3. Wire AfterOAuth button

**Line 588**, change:
```java
public void mouseReleased(MouseEvent e) { showPatchDetail(1); }
```
To:
```java
public void mouseReleased(MouseEvent e) { startLicenceOAuth2(nl); }
```

### 4. Clean up dead code

- Remove `buildLicencePatchDetail` method (lines 652-730) — no longer called
- Remove `mode == 1` branch from `showPatchDetail` (line 643-644) — only mode 0 and 2 remain

### 5. Import check

Ensure `java.util.concurrent.TimeUnit` is imported (already used by step 3, confirmed at line 479).

## NOT changed

- Steam patch — stays `showPatchDetail(2)` with inline `buildSteamPatchDetail` → remains as-is
- Step 3 OAuth2 — already works, unchanged
- `FramePCPatcher.java` — not touched (still exists for standalone/debug use)

## Files

| File | Change |
|------|--------|
| `FrameGuidance.java` | Add `startLicenceOAuth2()` method, 2 button listener replacements, remove dead `buildLicencePatchDetail`, clean up `showPatchDetail` mode 1 |

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Owner step 4: click "No Licence Patch" → browser opens Discord OAuth2
- [ ] After authorization: DLL downloads, status bar shows "License patch applied!", green
- [ ] AfterOAuth screen: same OAuth2 flow for licence patch
- [ ] Error cases: `not_in_guild` shows "Join Server First", `busy` shows "Bot Busy"
- [ ] Button re-enables on error (user can retry)
- [ ] Steam Patch buttons unchanged
