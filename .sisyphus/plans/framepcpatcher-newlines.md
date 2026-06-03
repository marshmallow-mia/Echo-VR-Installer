# FramePCPatcher — Support \n in Step Header Text

## TL;DR
> **Quick Summary**: Replace `\n` with `<br>` in the HTML table wrapper so manual newlines in step text render as line breaks.

---

## Context

The user added `\n` inside step 3 text. In HTML, `\n` is just whitespace. Need to convert to `<br>` for actual line breaks.

## Change

**File**: `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`

**Line 189** — Change:
```java
String htmlText = "<html><table width='460' align='center'><tr><td align='center'>" + text + "</td></tr></table></html>";
```
to:
```java
String htmlText = "<html><table width='460' align='center'><tr><td align='center'>" + text.replace("\n", "<br>") + "</td></tr></table></html>";
```

This replaces every `\n` with `<br>` before building the HTML, so manual line breaks render correctly.

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] Line uses `text.replace("\n", "<br>")`

**Evidence**: `.sisyphus/evidence/framepcpatcher-newlines.txt`

## Commit
- Message: `fix: support manual \n line breaks in step header text`
- Files: `FramePCPatcher.java`
