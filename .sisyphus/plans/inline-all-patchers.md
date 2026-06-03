# Inline All Patchers — No Modal Windows

## Goal

Eliminate ALL modal dialog windows (FramePCPatcher, FrameSteamPatcher) from the wizard flow. Every patch interaction happens inline within the FrameGuidance content panel using the existing `showPatchDetail()` master-detail pattern.

## Current State

- Steam patch buttons → `showPatchDetail(2)` with inline view ✓
- Licence patch buttons (owner step + AfterOAuth) → still open `FramePCPatcher` modal dialog ✗
- `buildLicencePatchDetail()` exists but is missing download/patch logic ✗

## Changes

### 1. Wire licence patch to use `showPatchDetail(1)`

Replace `new FramePCPatcher(...)` calls with `showPatchDetail(1)` in TWO locations:
- Owner step 4 "No Licence Patch" button (buildStep4, sub=0)
- AfterOAuth screen "No Licence Patch" button (buildStep4AfterOAuth)

### 2. Complete `buildLicencePatchDetail` with download/patch logic

Add the missing "Start Patching" button and download logic, extracted from FramePCPatcher:
- A progress label showing %
- "Start Patching" button that:
  - Validates the URL (Discord CDN pattern)
  - Checks the path exists
  - Creates a `Downloader` with onComplete callback
  - Downloads to the chosen path

### 3. Add progress label and start button

After the path chooser in `buildLicencePatchDetail`, add:
- `SpecialLabel` for progress (0%)
- `SpecialButton` "Start Patching" with the download logic
- On complete: update status bar green, statusBarBox.repaint()

### Files

| File | Change |
|------|--------|
| `FrameGuidance.java` | 2 button listeners → `showPatchDetail(1)`, complete inline licence patcher |
| No changes to `FramePCPatcher.java` or `FrameSteamPatcher.java` — they remain for standalone use |

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Owner step 4: click "No Licence Patch" → inline view with [← Back]
- [ ] Inline licence patch: paste URL, choose path, click "Start Patching"
- [ ] Download completes inline, status bar turns green
- [ ] Click [← Back] → returns to master patch selection
- [ ] AfterOAuth: same inline behaviour for licence patch
- [ ] Steam patch inline view unchanged
- [ ] No modal dialogs open from wizard
