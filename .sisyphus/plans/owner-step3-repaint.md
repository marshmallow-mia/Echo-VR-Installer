# Fix Owner Step 3 — Steam Patch Button Not Shown After License Patch

## Problem

In step 3 for OWNER users, clicking "No Licence Patch" opens `FramePCPatcher` (a modal JDialog). After closing it, the parent `FrameGuidance` content panel isn't repainted correctly because `Background` uses a custom image-painted `JPanel`. The "Steam Patch (Revive)" button exists in the component hierarchy but doesn't visually repaint.

## Root Cause

`FramePCPatcher.setModal(true)` blocks the EDT. On dispose, Swing repaints the parent but `Background.paintComponent()` draws the image first then calls `super.paintComponent` — the button children may get skipped if the parent clipping region or dirty rect isn't refreshed.

## Fix

Add `revalidate()` + `repaint()` after `FramePCPatcher` closes. Since it's created inline, wrap in a variable and call after.

### FrameGuidance.java — lines 397-399

**Current:**
```java
nl.addMouseListener(new MouseAdapter() { 
    public void mouseReleased(MouseEvent e) { 
        new FramePCPatcher(); 
    } 
});
```

**Fixed:**
```java
nl.addMouseListener(new MouseAdapter() { 
    public void mouseReleased(MouseEvent e) { 
        new FramePCPatcher();
        contentPanel.revalidate();
        contentPanel.repaint();
    } 
});
```

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Run installer as OWNER, go to step 3
- [ ] Click "No Licence Patch", close the dialog
- [ ] Verify "Steam Patch (Revive)" button is still visible
- [ ] Both buttons remain functional
