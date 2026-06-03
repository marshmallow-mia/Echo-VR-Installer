# FramePCPatcher TipBox Integration

## TL;DR
> **Quick Summary**: Add TipBox to FramePCPatcher with same pattern as FramePCDownload/FramePCEchoUpdate — replace `labelPcOculusPathExplaination` inline guide label, add hover tip listeners to all 3 buttons, place TipBox at bottom center.

> **Deliverables**:
> - Modified `FramePCPatcher.java` with TipBox, hover listeners, inline label removed

> **Estimated Effort**: Quick
> **Parallel Execution**: NO — single file

---

## Context

### Original Request
User: "For FramePCPatcher: Replace the guidience text, with the tipbox_top.png. Same logic like inside the tipbox for sizing it."

Meaning: Replace the inline guide label (`labelPcOculusPathExplaination` at line 123) with a TipBox (using `tipbox_top.png` header image, same default sizing logic). Add hover listeners to buttons to show tips in the TipBox.

### Reference Pattern
From FramePCDownload/FramePCEchoUpdate:
1. Create `TipBox tipBox = new TipBox();` early in `initComponents()`
2. Remove inline guide labels
3. Add separate `addMouseListener` calls for hover (`mouseEntered`/`mouseExited`)
4. Position at bottom: `(frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 60`

This window (1280x720) is larger, so no need to shift items or resize — the bottom-center placement doesn't overlap with existing content (left column content at x=40-319, TipBox centered at x≈490).

---

## Work Objectives

### Core Objective
Replace inline guide text in FramePCPatcher with TipBox + hover tip system

### Concrete Deliverables
- Modified `FramePCPatcher.java`

### Definition of Done
- [ ] `./gradlew compileJava` passes
- [ ] `labelPcOculusPathExplaination` removed
- [ ] TipBox at bottom center of window
- [ ] All 3 buttons have hover listeners pointing to TipBox
- [ ] Window size unchanged (1280x720) — no items need repositioning

### Must Have
- TipBox created BEFORE button code so hover adapters can reference it
- Separate `addMouseListener` for hover (keep existing `mouseReleased` intact)
- `labelPcOculusPathExplaination` removed

### Must NOT Have
- Do NOT modify other files
- Do NOT merge click + hover listeners
- Do NOT resize or reposition existing items (they have room)

---

## TODOs

- [ ] 1. Apply all edits to FramePCPatcher.java

  **What to do**:
  Apply these edits to `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`:

  **Edit 1** — Add TipBox creation after `this.setContentPane(back);`:
  After line 37 (`this.setContentPane(back);`), insert:
  ```
  
        //Tipbox erstellen (muss vor den Buttons sein, damit hover listener darauf zugreifen können)
        TipBox tipBox = new TipBox();
  
  ```

  **Edit 2** — Remove `labelPcOculusPathExplaination` (lines 123-125):
  Remove this block:
  ```
        SpecialLabel labelPcOculusPathExplaination = new SpecialLabel("Choose this to use the original Oculus path", 14);
        labelPcOculusPathExplaination.setLocation(814,225);
        back.add(labelPcOculusPathExplaination);
  ```

  **Edit 3** — Add hover listener to `pcChooseOriginalPath` (after its existing mouseReleased, before `back.add`):
  Old:
  ```java
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForAdminAndOculusPath(outFrame);
                if (!newPath.matches("")) {
                    labelPcPatchDownloadPath.setText(newPath + "Software\\Software\\ready-at-dawn-echo-arena");
                    outFrame.repaint();
                }
            }
        });
        back.add(pcChooseOriginalPath);
  ```
  New:
  ```java
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForAdminAndOculusPath(outFrame);
                if (!newPath.matches("")) {
                    labelPcPatchDownloadPath.setText(newPath + "Software\\Software\\ready-at-dawn-echo-arena");
                    outFrame.repaint();
                }
            }
        });
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Automatically find the original Oculus installation path");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcChooseOriginalPath);
  ```

  **Edit 4** — Add hover listener to `pcPatchChoosePath`:
  Old:
  ```java
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcPatchDownloadPath, outFrame);
            }
        });
        back.add(pcPatchChoosePath);
  ```
  New:
  ```java
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcPatchDownloadPath, outFrame);
            }
        });
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Choose a custom folder for the Echo VR patch installation");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcPatchChoosePath);
  ```

  **Edit 5** — Add hover listener to `pcStartPatch`:
  Old — find the `addMouseListener` block for the click handler on `pcStartPatch` (the one with `mouseReleased`), then before `back.add(pcStartPatch)` insert the hover listener. The old block ends at:
  ```java
            }
        });
        back.add(pcStartPatch);
  ```
  New:
  ```java
            }
        });
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Start patching Echo VR with the provided Discord link");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcStartPatch);
  ```

  **Edit 6** — Add TipBox positioning before `//Alles fertig machen...`:
  Before line `        //Alles fertig machen...`, insert:
  ```
        //Tipbox positionieren und hinzufügen...
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 60);
        back.add(tipBox);
  
  ```

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**: NO (sequential edits on one file)

  **References**:
  - `FramePCDownload.java` — Reference for same pattern
  - `TipBox.java` — TipBox API

  **Acceptance Criteria**:
  - [ ] `./gradlew compileJava` passes
  - [ ] No `labelPcOculusPathExplaination` in file
  - [ ] 3 buttons have hover handlers referencing `tipBox`
  - [ ] TipBox positioned at bottom via `setLocation`

  **QA Scenarios**:
  ```
  Scenario: Compilation succeeds
    Tool: Bash
    Steps:
      1. Run `./gradlew compileJava`
    Expected Result: BUILD SUCCESSFUL
    Evidence: .sisyphus/evidence/framepcpatcher-compile.txt

  Scenario: Verify tipBox references
    Tool: Bash
    Steps:
      1. Run `grep -c "tipBox" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`
    Expected Result: Shows 7+ (creation, 3 showTip, 3 showDefault, positioning, add)
    Evidence: .sisyphus/evidence/framepcpatcher-tipbox-refs.txt
  ```

  **Commit**: YES
  - Message: `feat: add TipBox to FramePCPatcher with hover listeners`
  - Files: `FramePCPatcher.java`

---

## Success Criteria

### Verification Commands
```bash
./gradlew compileJava
grep -n "tipBox" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java
grep -c "labelPcOculusPathExplaination" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java
```

### Final Checklist
- [ ] Compilation succeeds
- [ ] `labelPcOculusPathExplaination` removed from file (count = 0)
- [ ] `tipBox` referenced 7+ times
- [ ] All 3 buttons have mouseEntered/mouseExited handlers
- [ ] TipBox positioned at bottom center
