## Task 13: FramePCPatcher Redesign — Embedded Discord WebView Integration

### Implementation Summary
- Replaced external Discord steps 1-3 (hyperlink + images) with embedded Discord WebView integration
- Added `WizardState` field + constructor `FramePCPatcher(WizardState)` — follows FramePCDownload pattern (does NOT call setVisible for testability)
- Added `openDiscordBrowser()` method that creates DiscordWebView + DiscordNavigator, navigates to server, auto-extracts URL
- Added progress indicator label, URL detected label, manual fallback toggle
- Made key UI components fields for testability (textfieldPCPatchLink, labelPcPatchDownloadPath, pcStartPatch, patchProgress, discordOpenButton, discordProgressLabel, urlDetectedLabel, manualFallbackLabel)
- Preserved no-arg constructor, patching logic, helper methods (createStepHeader, createSectionPanel, calcBounds, loadGUI)

### Test Strategy
- WizardState constructor (no setVisible) used for all component-inspection tests
- oldConstructorStillWorks tested via reflection (no-arg constructor blocks due to setVisible + modal)
- 9 tests cover: constructors, path defaults, Discord button, URL extraction, patching, labels
- All existing helpers and patching logic preserved

### Key Pattern
- FramePCDownload pattern: WizardState-accepting constructor omits setVisible(true) so tests can inspect components without blocking
