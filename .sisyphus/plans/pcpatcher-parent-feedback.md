# FramePCPatcher: Notify Parent on Patch Complete

## Problem

`FramePCPatcher` is a modal dialog opened from `FrameGuidance` step 3 (owner flow). When the user patches inside it, the parent `FrameGuidance` gets no notification. The user sees the same two buttons with no visual feedback that the license patch was applied.

## Fix

Add an `Runnable` callback parameter to `FramePCPatcher` that fires when the patch download completes, and pass the status bar update from `FrameGuidance`.

### FramePCPatcher.java

1. Add field: `private Runnable onPatchComplete;`
2. Add constructor parameter: `public FramePCPatcher(Runnable onPatchComplete)`
3. In the downloader's `setOnCompleteListener` (around line 200), call `onPatchComplete` before the existing update logic
4. Keep the no-arg constructor as a fallback: `public FramePCPatcher() { this(null); }`

**In the `setOnCompleteListener`:**
```java
// Around line 200, after the existing onComplete code:
if (onPatchComplete != null) {
    SwingUtilities.invokeLater(onPatchComplete);
}
```

### FrameGuidance.java

Change the button listener from:
```java
new FramePCPatcher();
```
To:
```java
new FramePCPatcher(() -> {
    dlProgressLabel.setText("License patch applied!");
    stepCompleted = true;
    stepInProgress = false;
    progressAnimator.stop();
    progPanel.repaint();
    statusBarBox.repaint();
});
```

This turns the status bar green (stepCompleted=true), shows "License patch applied!", and keeps both buttons visible.

## Files

| File | Change |
|------|--------|
| `FramePCPatcher.java` | Add `onPatchComplete` callback field, constructor parameter, invoke in download complete listener |
| `FrameGuidance.java` | Pass callback to `FramePCPatcher` constructor that updates status bar |

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Run as owner, go to step 3, click "No Licence Patch"
- [ ] Complete patching in FramePCPatcher
- [ ] Verify status bar turns green and shows "License patch applied!"
- [ ] Verify both buttons remain visible
- [ ] Click "Steam Patch (Revive)" — still functional
