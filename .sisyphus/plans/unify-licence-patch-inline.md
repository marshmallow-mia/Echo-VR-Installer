# Unify Licence Patch — One Inline Screen

## TL;DR

> **Quick Summary**: Delete the wrong `buildLicencePatchDetail` (Discord link + instructions + textbox). Extract the correct inline patch screen from `buildStep4`'s `else` branch (path chooser + "Authorize with Discord" + OAuth2 auto-download) into `showLicencePatchInline()`. Wire both "No Licence Patch" buttons to it.
>
> **Deliverables**:
> - New `showLicencePatchInline(int cx)` method (path chooser + OAuth2 button, no redirect)
> - Owner "No Licence Patch" button → calls `showLicencePatchInline`
> - AfterOAuth "No Licence Patch" button → calls `showLicencePatchInline`
> - Dead `buildLicencePatchDetail` deleted
> - `showPatchDetail` mode 1 branch removed
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO — sequential (one file, four edits)
> **Critical Path**: One file, one method

---

## Context

### Problem
There are TWO licence patch UIs:
1. **WRONG** `buildLicencePatchDetail` — hyperlink + instructions + "Authorize with Discord" button (a confusing hybrid). Has a back button from `showPatchDetail(1)`.
2. **CORRECT** — the `else` branch in `buildStep4` (lines 437–570) — path chooser + validation + "Authorize with Discord" button. No back button. The only one the user wants.

Both "No Licence Patch" buttons (Owner step and AfterOAuth) currently call `showPatchDetail(1)` → `buildLicencePatchDetail` (the wrong one).

### Goal
Delete the wrong one. Both "No Licence Patch" buttons call the correct inline screen directly — no master-detail navigation, no back button, no extra Discord link.

---

## Changes

### File: `FrameGuidance.java`

### 1. Create `showLicencePatchInline(int cx)`

Extracted from lines 438–562 of `buildStep4`'s else branch. Same path chooser + "Authorize with Discord" button + full OAuth2 flow. Key difference from the else block: does NOT redirect to `buildStep4AfterOAuth` on completion — just updates the status bar.

Insert this method right before `buildSteamPatchDetail` (replacing the old `buildLicencePatchDetail`).

### 2. Wire Owner "No Licence Patch" button

**Line 431**, change:
```java
public void mouseReleased(MouseEvent e) { showPatchDetail(1); }
```
To:
```java
public void mouseReleased(MouseEvent e) { contentPanel.removeAll(); showLicencePatchInline(cx); contentPanel.revalidate(); contentPanel.repaint(); }
```

### 3. Wire AfterOAuth "No Licence Patch" button  

**Line 588**, change:
```java
public void mouseReleased(MouseEvent e) { showPatchDetail(1); }
```
To:
```java
public void mouseReleased(MouseEvent e) { contentPanel.removeAll(); showLicencePatchInline(cx); contentPanel.revalidate(); contentPanel.repaint(); }
```

### 4. Remove `mode == 1` branch from `showPatchDetail`

Delete:
```java
        if (mode == 1) {
            buildLicencePatchDetail(cx);
        } else
```
So that `showPatchDetail` only handles mode 0 (master) and mode 2 (steam).

### 5. NOT changed

- `buildStep4` else block (lines 437–570) — stays as-is, it needs the redirect to `buildStep4AfterOAuth`
- Steam Patch — remains `showPatchDetail(2)` → `buildSteamPatchDetail`
- `startLicenceOAuth2` method — untouched

---

## Verification

- [x] `./gradlew clean compileJava` — BUILD SUCCESSFUL
- [x] No `buildLicencePatchDetail` in source
- [x] No `showPatchDetail(1)` callers remaining
- [x] `showLicencePatchInline` method exists with path chooser + OAuth2 button
- [x] Owner step "No Licence Patch" → shows path chooser + "Authorize with Discord"
- [x] AfterOAuth "No Licence Patch" → same inline screen
- [x] Non-owner `buildStep4` unchanged (redirect on download complete)
- [x] `startLicenceOAuth2` method still exists (used by nothing — orphaned, fine to keep)
