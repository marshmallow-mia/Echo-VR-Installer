# FramePCEchoUpdate TipBox Integration

## TL;DR
> **Quick Summary**: Apply the same TipBox integration pattern from FramePCDownload to FramePCEchoUpdate — remove inline guide labels, add hover listeners to buttons, shift items up by 30px, and adjust window height to 419px.

> **Deliverables**:
> - FramePCEchoUpdate.java with TipBox at bottom, hover listeners on all 3 buttons, inline labels removed, all items shifted up 30px, window at 419px

> **Estimated Effort**: Quick
> **Parallel Execution**: NO — single file
> **Critical Path**: Single task

---

## Context

### Original Request
User asked "Add the tipbox for FramePCEchoUpdate pls" — same treatment as FramePCDownload.

### FramePCDownload Pattern (Reference)
FramePCDownload was already transformed:
1. Added `TipBox tipBox = new TipBox();` early in `initComponents()`, before button code
2. Removed 2 inline guide labels (`labelPcOculusPathExplaination`, `labelPcDownloadPathExplaination`)
3. Added separate `addMouseListener` calls for hover (`mouseEntered`/`mouseExited`) to all 3 buttons
4. Shifted all Y coordinates up by 30px
5. frameHeight went from 394 → 434 → 419 (adjusted)
6. Placed tipBox at `(frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 60`

---

## Work Objectives

### Core Objective
Integrate TipBox into FramePCEchoUpdate following the exact FramePCDownload pattern

### Concrete Deliverables
- Modified `FramePCEchoUpdate.java`

### Definition of Done
- [ ] `./gradlew compileJava` compiles successfully
- [ ] TipBox renders at bottom of FramePCEchoUpdate window
- [ ] Hovering over `pcChooseOriginalPath` shows tip in TipBox
- [ ] Hovering over `pcChoosePath` shows tip in TipBox
- [ ] Hovering over `pcStartDownload` shows tip in TipBox
- [ ] Mouse exiting each button restores default tip
- [ ] 3 inline guide labels removed from window
- [ ] All items shifted up 30px from original positions
- [ ] Window height changed from 394 to 419

### Must Have
- TipBox creation before button code (so anonymous hover adapters can reference `tipBox`)
- Separate `addMouseListener` calls for hover (keep existing `mouseReleased` handlers intact)
- Hover tips: informative text for each button
- Items at their new Y positions (original - 30px)

### Must NOT Have (Guardrails)
- Do NOT modify FramePCDownload, FrameMain, TipBox, or any other file — only FramePCEchoUpdate.java
- Do NOT merge the two `addMouseListener` calls on any button — keep click and hover separate
- Do NOT change X coordinates, only Y coordinates

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed.

### QA Policy
- Compilation: `./gradlew compileJava` must pass
- Each edit verified by reading the file after the change

---

## TODOs

- [ ] 1. Apply all edits to FramePCEchoUpdate.java

  **What to do**:
  Make these exact edits to `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCEchoUpdate.java`:

  **Edit 1** — Change frameHeight from 394 to 419:
  ```
  old: int frameHeight = 394;
  new: int frameHeight = 419;
  ```

  **Edit 2** — Add TipBox creation after `this.setContentPane(back);`:
  Insert these lines right after `this.setContentPane(back);`:
  ```
  
        //Tipbox erstellen (muss vor den Buttons sein, damit hover listener darauf zugreifen können)
        TipBox tipBox = new TipBox();

  ```

  **Edit 3** — Update pcChooseOriginalPath: change Y from 70 to 40, add hover listener:
  Replace this block:
  ```java
        pcChooseOriginalPath.setLocation(20, 70);
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForEchoOnKnownPaths(outFrame);
                if (!newPath.matches("")) {
                    System.out.println("Echo found at path: " + newPath);
                    JOptionPane.showMessageDialog(outFrame, "<html>echovr.exe was found at the following path. If thats wrong, set the path manually!!!<br>" + newPath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
                    labelPcDownloadPath.setText(newPath);
                    outFrame.repaint();
                }
            }
        });
        back.add(pcChooseOriginalPath);
  ```
  With:
  ```java
        pcChooseOriginalPath.setLocation(20, 40);
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForEchoOnKnownPaths(outFrame);
                if (!newPath.matches("")) {
                    System.out.println("Echo found at path: " + newPath);
                    JOptionPane.showMessageDialog(outFrame, "<html>echovr.exe was found at the following path. If thats wrong, set the path manually!!!<br>" + newPath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
                    labelPcDownloadPath.setText(newPath);
                    outFrame.repaint();
                }
            }
        });
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Automatically search for Echo VR on known installation paths");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcChooseOriginalPath);
  ```

  **Edit 4** — Remove `labelPcOculusPathExplaination` block:
  Remove these lines:
  ```
        SpecialLabel labelPcOculusPathExplaination = new SpecialLabel("Choose this to search Echo on known paths", 14);
        labelPcOculusPathExplaination.setLocation(252,70);
        back.add(labelPcOculusPathExplaination);
  ```

  **Edit 5** — Update pcChoosePath: change Y from 130 to 100, add hover listener:
  Replace this block:
  ```java
        pcChoosePath.setLocation(20, 130);
        pcChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcDownloadPath, outFrame);
            }
        });
        back.add(pcChoosePath);
  ```
  With:
  ```java
        pcChoosePath.setLocation(20, 100);
        pcChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcDownloadPath, outFrame);
            }
        });
        pcChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Manually specify the location of echovr.exe");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcChoosePath);
  ```

  **Edit 6** — Remove `labelPcDownloadPathExplaination` and `labelPcDownloadPathExplaination2` block:
  Remove these lines:
  ```
        SpecialLabel labelPcDownloadPathExplaination = new SpecialLabel("Specify the echovr.exe location", 14);
        labelPcDownloadPathExplaination.setLocation(20,160);
        SpecialLabel labelPcDownloadPathExplaination2 = new SpecialLabel("Its located inside your echo install folder in  \"bin/win10\"", 14);
        labelPcDownloadPathExplaination2.setLocation(20,187);
        back.add(labelPcDownloadPathExplaination);
        back.add(labelPcDownloadPathExplaination2);
  ```

  **Edit 7** — Update labelPcDownloadPath Y from 130 to 100:
  ```
  old: labelPcDownloadPath.setLocation(170,130);
  new: labelPcDownloadPath.setLocation(170,100);
  ```

  **Edit 8** — Update labelPcProgress1 Y from 230 to 200:
  ```
  old: labelPcProgress1.setLocation(252,230);
  new: labelPcProgress1.setLocation(252,200);
  ```

  **Edit 9** — Update labelPcProgress2 Y from 230 to 200:
  ```
  old: labelPcProgress2.setLocation(407,230);
  new: labelPcProgress2.setLocation(407,200);
  ```

  **Edit 10** — Update pcStartDownload: change Y from 230 to 200, add hover listener:
  Replace this block:
  ```java
        pcStartDownload.setLocation(20, 230);
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                if (downloader != null){
                    downloader.cancelDownload();
                    pause(1);
                }
                pcStartDownload.changeText("Restart Download");

                String filePath  = labelPcDownloadPath.getText();
                if (Files.exists(Path.of(filePath + "echovr.exe"))) {
                    System.out.println("echovr.exe does exist: " + filePath);
                    String[] updateFiles = getFileAndReturnArray("https://files.echovr.de/updates/files", "updateFiles");
                    String URL = "https://files.echovr.de/updates/";
                    //Download all updated files
                    for (String file : updateFiles) {
                        System.out.println("Updatefile:" + file);

                        Thread downloadThread1 = new Thread(() -> {
                            downloader = new Downloader();
                            downloader.setOnCompleteListener(() -> {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(null, "Updating is successfull! ", "Update done", JOptionPane.INFORMATION_MESSAGE);

                                });
                            });
                            downloader.startDownload(URL + file, labelPcDownloadPath.getText(), file, labelPcProgress2, thisFrame, frameMain, 1, true, -1, true);
                        });

                        downloadThread1.start();  // This runs the download in a separate thread
                    }
                } else {
                    System.out.println("echovr.exe does not exist: " + filePath);
                    JOptionPane.showMessageDialog(null, "Wrong path to echovr.exe. Choose the right path please!", "Wrong path", JOptionPane.INFORMATION_MESSAGE);

                }



            }
        });
        back.add(pcStartDownload);
  ```
  With (Y changed and hover listener added):
  ```java
        pcStartDownload.setLocation(20, 200);
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                if (downloader != null){
                    downloader.cancelDownload();
                    pause(1);
                }
                pcStartDownload.changeText("Restart Download");

                String filePath  = labelPcDownloadPath.getText();
                if (Files.exists(Path.of(filePath + "echovr.exe"))) {
                    System.out.println("echovr.exe does exist: " + filePath);
                    String[] updateFiles = getFileAndReturnArray("https://files.echovr.de/updates/files", "updateFiles");
                    String URL = "https://files.echovr.de/updates/";
                    //Download all updated files
                    for (String file : updateFiles) {
                        System.out.println("Updatefile:" + file);

                        Thread downloadThread1 = new Thread(() -> {
                            downloader = new Downloader();
                            downloader.setOnCompleteListener(() -> {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(null, "Updating is successfull! ", "Update done", JOptionPane.INFORMATION_MESSAGE);

                                });
                            });
                            downloader.startDownload(URL + file, labelPcDownloadPath.getText(), file, labelPcProgress2, thisFrame, frameMain, 1, true, -1, true);
                        });

                        downloadThread1.start();  // This runs the download in a separate thread
                    }
                } else {
                    System.out.println("echovr.exe does not exist: " + filePath);
                    JOptionPane.showMessageDialog(null, "Wrong path to echovr.exe. Choose the right path please!", "Wrong path", JOptionPane.INFORMATION_MESSAGE);

                }



            }
        });
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Start downloading the Echo VR update to the selected path");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcStartDownload);
  ```

  **Edit 11** — Add tipBox positioning before `//Alles fertig machen...`:
  Insert before the line `//Alles fertig machen...`:
  ```
        //Tipbox positionieren und hinzufügen...
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 60);
        back.add(tipBox);

  ```

  **Important**: Apply edits sequentially, not in parallel, since later edits depend on earlier ones being applied (line numbers shift). Start from the top of the file (edits that change the earliest lines should run first, or be far enough apart not to conflict).

  **Recommended Agent Profile**:
  - **Category**: `quick` — Single-file edit with precise instructions
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (sequential edits on one file)
  - **Blocks**: None (only task)
  - **Blocked By**: None

  **References**:
  - `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java` — Reference implementation of same pattern (TipBox creation, hover listeners, positioning)
  - `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/TipBox.java` — TipBox API: `showTip(String)`, `showDefault()`, `getWidth()`, `getHeight()`

  **Acceptance Criteria**:
  - [ ] `./gradlew compileJava` passes
  - [ ] FramePCEchoUpdate opens with TipBox at bottom
  - [ ] All 3 buttons have hover handlers
  - [ ] Inline labels removed
  - [ ] Items at Y-30 coordinates

  **QA Scenarios**:
  ```
  Scenario: Compilation succeeds
    Tool: Bash
    Steps:
      1. Run `./gradlew compileJava`
    Expected Result: BUILD SUCCESSFUL
    Evidence: .sisyphus/evidence/framepcechoupdate-compile.txt

  Scenario: Verify structural changes in file
    Tool: Bash
    Preconditions: Compilation succeeded
    Steps:
      1. Run `grep -n "tipBox" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCEchoUpdate.java`
    Expected Result: Shows 5+ lines referencing tipBox (creation, hover listeners, positioning)
    Evidence: .sisyphus/evidence/framepcechoupdate-tipbox-refs.txt
  ```

  **Commit**: YES (groups with 1)
  - Message: `feat: add TipBox to FramePCEchoUpdate with hover listeners`
  - Files: `FramePCEchoUpdate.java`

---

## Success Criteria

### Verification Commands
```bash
./gradlew compileJava
grep -n "tipBox" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCEchoUpdate.java
```

### Final Checklist
- [ ] Compilation succeeds
- [ ] TipBox referenced in file (creation + usage)
- [ ] No inline labels remaining (`labelPcOculusPathExplaination`, `labelPcDownloadPathExplaination`)
- [ ] All 3 buttons have `mouseEntered`/`mouseExited` handlers
- [ ] Y coordinates all reduced by 30px
- [ ] frameHeight = 419
