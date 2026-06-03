# FramePCPatcher — Constrain Text to Inner 460px of Step Headers

## TL;DR
> **Quick Summary**: Modify `createStepHeader()` so text is constrained to a 460px area centered in the 500px image (20px decorative margin on each side), using HTML `<table>` for reliable Swing rendering.

---

## Context

The user wants the tipbox_top.png image to have decorative margins on left/right — text should only appear in the middle 460px (500px minus 20px on each side), centered.

## Change

**File**: `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`

**Method**: `createStepHeader` — line 189

Change the HTML text from:
```java
String htmlText = "<html><center>" + text + "</center></html>";
```
to:
```java
String htmlText = "<html><table width='460' align='center'><tr><td align='center'>" + text + "</td></tr></table></html>";
```

This creates a 460px wide table centered in the 500px label (20px margin each side). Text is centered inside the table cell and wraps within that constrained width.

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] Line 189 uses `<table width='460' align='center'>` instead of `<center>`

**Evidence**: `.sisyphus/evidence/framepcpatcher-text-padding.txt` (compilation output)

## Commit
- Message: `fix: constrain step header text to 460px centered area with 20px decorative margins`
- Files: `FramePCPatcher.java`
