# Task 16: Wizard Flow Integration - Decisions

## Architectural Decisions
1. **OWNER → OptionalPatchesPanel**: OWNER users who already have the game download proceed to OptionalPatchesPanel to choose which patches to apply, rather than going directly to the patcher.
2. **NEW_PLAYER → FramePCPatcher(WizardState)**: New players go directly to the patcher with the install path pre-populated from the download step.
3. **Dispose before creating next panel**: FramePCDownload disposes itself before the invokeLater creates the next panel, preventing dialog stacking issues.
4. **setVisible(true) managed by callers**: The WizardState-accepting constructors don't auto-show, giving callers control over dialog visibility timing.
