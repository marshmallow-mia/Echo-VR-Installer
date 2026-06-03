# Task 16: Wizard Flow Integration - Issues

## Issues Found
1. **WizardState not propagated from UserTypeDialog**: FrameMain fetched WizardState from UserTypeDialog but then discarded it by calling the single-arg FramePCDownload constructor which creates a new empty WizardState.
2. **Wrong branching in Next button**: Both OWNER and NEW_PLAYER paths used no-arg `FramePCPatcher()` constructor, losing the stored path and user type.
3. **Two-arg constructor missing setVisible**: `FramePCDownload(FrameMain, WizardState)` doesn't call `setVisible(true)`, making the dialog invisible when used. Fixed by having FrameMain call it after construction.
