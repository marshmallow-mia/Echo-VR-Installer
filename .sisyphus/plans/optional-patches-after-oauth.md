# Show Optional Patches After OAuth2 Patch Completes

## Problem

New player completes the OAuth2 flow → patch downloads → status bar says "Patch applied successfully!" — but the screen stays on the OAuth2 layout (path chooser + authorize button). The user has no access to the "No Licence Patch" and "Steam Patch (Revive)" buttons that owners see.

## Fix

In `FrameGuidance.buildStep3()`, after a new player's OAuth2 patch download completes successfully, **rebuild the content** to show a hybrid layout:
- Path label + ✓ indicator (read-only, showing where the patch was applied)
- "No Licence Patch" button
- "Steam Patch (Revive)" button
- Status bar already reads "Patch applied successfully!" in green

This replaces the OAuth2 button + statusLabel with the optional patches buttons, while preserving the path feedback.

### Changed behavior at download completion

In the `setOnCompleteListener` of the OAuth2 download (around line 463-471), after setting `stepCompleted = true`, **also rebuild the step 3 content** with optional patches by calling `buildStep3(cx, 0)` (not `buildContent(3, 0)`, just rebuild inside the same step).

## Implementation

### FrameGuidance.java

Change the OAuth2 download's `setOnCompleteListener` from:
```java
stepInProgress = false;
stepCompleted = true;
progressAnimator.stop();
nextBtn.setEnabled(true);
dlProgressLabel.setText("Patch applied successfully!");
progPanel.repaint();
statusBarBox.repaint();
```

To:
```java
stepInProgress = false;
stepCompleted = true;
progressAnimator.stop();
nextBtn.setEnabled(true);
dlProgressLabel.setText("Patch applied successfully!");
progPanel.repaint();
statusBarBox.repaint();

// Rebuild content to show optional patches
contentPanel.removeAll();
buildStep3AfterOAuth(cx);
contentPanel.revalidate();
contentPanel.repaint();
```

### New helper method

Add `buildStep3AfterOAuth(int cx)` that renders:
- The existing path label (from `wizardState.getInstallPath()`) with ✓ green indicator
- "No Licence Patch" button → `new FramePCPatcher()`
- "Steam Patch (Revive)" button → `new FrameSteamPatcher(frameMain)`

Same button styling and positioning as the owner branch (y=73 for license, y=121 for steam).

## Files

| File | Change |
|------|--------|
| `FrameGuidance.java` | After OAuth2 download completes, rebuild step 3 content to show optional patches |
| `FrameGuidance.java` | Add `buildStep3AfterOAuth(int cx)` helper method |

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Run as new player, go through OAuth2 flow to patch
- [ ] After patch downloads, optional patches appear (No Licence Patch + Steam Patch)
- [ ] Status bar stays green with "Patch applied successfully!"
- [ ] Both optional patch buttons are functional
- [ ] Next button navigates to step 4 (Done)
