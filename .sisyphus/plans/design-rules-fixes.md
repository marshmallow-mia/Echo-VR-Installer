# Fix Design Rule Violations — Step 3 New Player Layout

## Context

Audit of `FrameGuidance.java` `buildStep3()` against `.sisyphus/design-rules.md` found 5 violations in the new player branch.

## Fixes

All changes are in `buildStep3()` inside the `else` block (new player branch), file `FrameGuidance.java`.

### 1. Add missing header (Rules 8, 2)
Add `makeHeader("Authorize with Discord to patch Echo VR")` at y=5, matching all other steps.

### 2. Move path label + indicator to y=70 (Rules 2, 3)
Change path label y from `5` to `70`, matching steps 1 and 2.

### 3. Fix validation indicator font (Rule 6)
Change initial font from `Font.BOLD, 14` to `Font.BOLD, 18` (matches what `updatePathStatus` sets on first call — avoids wasted render pass).

### 4. Move buttons down to maintain 10px gaps (Rule 2)
- Choose path button: y `32` → `102` (matches step 1's button position)
- Authorize button: y `65` → `137` (10px below choose path)

### 5. Upgrade OAuth button to primary style (Rule 9)
Change from `"button_up_middle.png"` (font 14) to `"button_up.png"` (font 18), consistent with "Start Download" and "I own Echo" primary action buttons.

## Final Layout (y-positions)

```
y=5   Header ("Authorize with Discord to patch Echo VR")
y=70  Path label + ✓/✗ indicator
y=102 "Choose path" button
y=137 "Authorize with Discord" button (primary)
```

## Verification

- [ ] `./gradlew compileJava` — BUILD SUCCESSFUL
- [ ] Run installer, go through wizard to step 3 as new player
- [ ] Header visible at top
- [ ] Path label at same height as step 1 and step 2 path labels
- [ ] ✓/✗ indicator visible next to path
- [ ] "Choose path" button works, updates path and validation
- [ ] "Authorize with Discord" button uses larger primary button style
- [ ] All buttons have hover tips
