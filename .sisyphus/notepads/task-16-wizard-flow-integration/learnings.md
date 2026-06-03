# Task 16: Wizard Flow Integration - Learnings

## Wiring Fixes Made

### 1. FrameMain.java (line 124)
**Issue**: WizardState was fetched from UserTypeDialog but NOT passed to FramePCDownload (used single-arg constructor).
**Fix**: Changed to use `new FramePCDownload(outFrame, wizardState)` (two-arg constructor) + `downloadFrame.setVisible(true)`.

### 2. FramePCDownload.java Next button (lines 197-216)
**Issue**: Both OWNER and NEW_PLAYER branches called `new FramePCPatcher()` (no-arg), losing the wizard state and path. Neither branch used OptionalPatchesPanel.
**Fix**: 
- OWNER → `OptionalPatchesPanel(null, wizardState)` (allows path-aware patching)
- NEW_PLAYER → `FramePCPatcher(wizardState)` (propagates install path to patcher)

## Key Observations
- FramePCDownload's two-arg constructor does NOT call `setVisible(true)` — the caller is responsible for that.
- FramePCPatcher(WizardState) constructor also does NOT call `setVisible(true)` — caller must do it.
- OptionalPatchesPanel handles null parent gracefully (positions centered on screen).
- All WizardState flow: UserTypeDialog → FramePCDownload → OptionalPatchesPanel/FramePCPatcher uses the SAME WizardState reference.
