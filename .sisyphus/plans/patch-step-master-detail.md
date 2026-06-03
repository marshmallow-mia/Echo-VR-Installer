# Patch Step Redesign — Master-Detail with Inline Patchers

## Concept

The Patch step (step 4) becomes a master-detail layout. Instead of opening `FramePCPatcher` / `FrameSteamPatcher` as modal dialogs, their content is embedded inline in the wizard content panel. A back button at the top-left of the content box returns to the main patch selection screen.

## Layout

### Main View (sub 0)
Shows available patches as button cards:
```
Header: "Optional Patches"

┌─────────────────────────────────────┐
│ No Licence Patch                    │  ← opens inline licence patcher
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ Steam Patch (Revive)                │  ← opens inline steam patcher
└─────────────────────────────────────┘
```

### Detail View (when a patch is selected)
```
[← Back]   Header: "No Licence Patch"

  [inline walkthrough: Discord link, react image, URL field,
   path chooser, Start Patching button]
```

The back button sits at the top-left of the content section box, styled like the existing `< Back` nav button.

## Implementation

### Approach

Each patcher (licence, steam) becomes a `JPanel` that implements its own layout — extracted from the existing `FramePCPatcher.initComponents()` and `FrameSteamPatcher.initComponents()` methods. These panels are shown/hidden within the content panel depending on the selected patch.

### Option A (simpler): Keep dialogs, but inline them

Create wrapper methods in `FrameGuidance` that render each patcher's content as a `JPanel` (replicating the dialog's initComponents logic but returning a panel instead of a dialog). Show/hide via `contentPanel.removeAll()` + rebuild.

### Option B (cleaner): Extract patcher panels

Refactor `FramePCPatcher` and `FrameSteamPatcher` to expose their content as a static or instance method returning a `JPanel`. FrameGuidance can then embed these panels directly.

### Recommended: Option A

Replicate just enough of each patcher's UI within FrameGuidance. This avoids touching FramePCPatcher/FrameSteamPatcher (which are also used standalone in other contexts).

## Detailed Changes

### `FrameGuidance.java`

**New helper: `buildPatchDetailBackButton(int cx)`**  
Renders a "← Back" label/button at the top-left of the content box that calls `showStep(4, 0)`.

**New helper: `buildLicencePatchPanel(int cx)`**  
Renders the licence patcher UI inline:
- Discord join hyperlink → `https://discord.gg/bMpsva6fmA`
- React image + copy link image (side by side)
- URL text field for patch link
- Path chooser with label + buttons
- "Start Patching" button that downloads and patches

**New helper: `buildSteamPatchPanel(int cx)`**  
Renders the steam patcher UI inline, same structure as licence but with FrameSteamPatcher's specific content.

**Modified: `buildStep4(int cx, int sub)`**  
- Sub 0: renders the main view (patch options), or serves as the "master" 
- When a patch button is clicked: calls a private method that switches to detail view
- Detail view: back button + specific patch panel

**Modified: `buildStep4AfterOAuth(int cx)`**  
Same master-detail behavior after OAuth2 completes.

### State tracking

Add a private field `int patchDetailMode = 0` (0 = master, 1 = licence detail, 2 = steam detail). This tracks which view is shown, allowing the back button to know where to return.

## Files

| File | Change |
|------|--------|
| `FrameGuidance.java` | Add inline patcher panels, back button, master-detail state |

No changes to `FramePCPatcher.java` or `FrameSteamPatcher.java` — they remain usable standalone.

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Step 4 shows "Optional Patches" with two patch options
- [ ] Click "No Licence Patch" → content switches to licence patcher with [← Back]
- [ ] Click [← Back] → returns to main patch selection
- [ ] Licence patch download works inline
- [ ] Steam patch opens inline
- [ ] Back button works on both
