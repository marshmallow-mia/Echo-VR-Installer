# SteamVR Integration — PlayStyle Step + Steam Patch Substep

## Overview

Add a new wizard step asking how the user plays Echo VR (SteamVR vs Meta Link), and move the Steam Patch from a standalone dialog to an inline substep of the Patch step.

## New Wizard Flow

```
Step 0: "Choose your player type"      → Owner / New Player
Step 1: "How do you play Echo VR?"      → SteamVR / Meta Link   ← NEW
Step 2: "Choose your install path"                                 (was step 1)
Step 3: "Ready to download"                                        (was step 2)
Step 4: "Patch"                                                    (was step 3)
  └─ Sub 0: Authorize & Patch (new player) / Optional Patches (owner)
  └─ Sub 1: Steam Patch (only if playStyle=SteamVR)                ← NEW
Step 5: "All Done"                                                 (was step 4)
```

Chips: `Type → Play → Path → Download → Patch → Done`

## Detailed Changes

### 1. New Step 1 — "How do you play Echo VR?"

**`WizardState.java`:**
- Add `PlayStyle` enum: `STEAMVR`, `META_LINK`
- Add field `private PlayStyle playStyle;`

**`FrameGuidance.java` — `buildStep1(int cx)`:**
- Two buttons: "I play via SteamVR (Revive)" and "I play via Meta Link"
- Hover tips: 
  - SteamVR: "Use this if you launch Echo through SteamVR with Revive. A special patch will be applied."
  - Meta Link: "Use this if you run Echo directly through the Meta Quest Link app on PC."
- Uses `button_up.png` font 18 (same as step 0)

**`updateStatusText`:** add "How do you launch Echo VR?" for step 1

**`getSubstepName`:**
- Step 2 → "Choose Path"
- Step 3 → "Download"  
- Step 4 → "Patch" (was step 3)
- Step 5 → "All Done" (was step 4)

### 2. Shift all subsequent steps by +1

- All `showStep(s, sub)` calls need s incremented by 1 for steps ≥ 1
- `updateStatusText` switch needs reindexing
- `getSubstepName` switch needs reindexing
- `buildContent` switch needs reindexing
- All existing `buildStep1` → `buildStep2`, `buildStep2` → `buildStep3`, `buildStep3` → `buildStep4`, `buildStep4` → `buildStep5`

### 3. Steam Patch as Patch Substep (Step 4, Sub 1)

Only enabled when `playStyle == PlayStyle.STEAMVR`.

The Steam Patch substep shows the FrameSteamPatcher content **inline** in the Patch step (not as a separate dialog). It already embeds a Discord join link, reaction instructions, path chooser, and patch button.

Alternatively, if refactoring FrameSteamPatcher is too complex, keep it as a button that opens the dialog — but show it as part of the substep flow, not a random button on the optional patches screen.

### 4. Update sidebar

```
Step 4: Patch
  ● Authorize & Patch (or Optional Patches for owners)
  ○ Steam Patch (only if SteamVR)
```

### 5. PlayStyle-aware SteamVR path

If the user selected SteamVR, the installer could default to checking the Revive path (`C:\Program Files\Revive\`) as a secondary install location indicator.

## Files Affected

| File | Change |
|------|--------|
| `WizardState.java` | Add `PlayStyle` enum + field |
| `FrameGuidance.java` | New step 1, reindex all steps +1, add Steam Patch substep to step 4 |
| `FrameSteamPatcher.java` | May need to embed inline instead of dialog (or keep as-is) |
