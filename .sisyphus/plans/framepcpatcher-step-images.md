# FramePCPatcher — Replace Step Text with tipbox_top.png Headers

## TL;DR
> **Quick Summary**: Remove my mistaken TipBox component from FramePCPatcher, then replace all numbered step-by-step text labels (1. through 6.) with `tipbox_top.png` image labels that display the step text overlaid on the image — same visual pattern as TipBox's header ("Tipbox" text on tipbox_top.png).

> **Deliverables**:
> - FramePCPatcher.java: TipBox component removed, step text replaced with image-based headers

> **Estimated Effort**: Quick

---

## Context

### Original Request
User: *"For FramePCPatcher: Replace the guidience text, with the tipbox_top.png. Same logic like inside the tipbox for sizing it."*
Then clarified: *"I want to get rid of the onscreen text. the text starting withy 1.,2. and so on and replace it with the png file and the text should shown inside the png. Like the Tipbox text inside the tipbox"*

### What Was Misunderstood
I added a `TipBox` component with hover listeners (from the FramePCDownload pattern). The user actually wants to:
1. Remove the TipBox component (never requested for this window)
2. Replace the numbered step text (1., 2., etc.) with `tipbox_top.png` images that have the text overlaid on them — same visual as TipBox's header label showing "Tipbox" on `tipbox_top.png`

### Image Reference
- `tipbox_top.png` native: **802 × 72** pixels
- Used in TipBox header as: `JLabel("Tipbox", icon, CENTER)` with conthrax-sb font 20pt, white
- Will be scaled to **500px wide** (auto-height ≈ 45px) to fit within the 1280×720 window layout

---

## Work Objectives

### Core Objective
Replace 6 groups of step text labels (left column: steps 1-3, right column: steps 4-6) with `tipbox_top.png` image banners showing step text on top

### Concrete Deliverables
- Modified FramePCPatcher.java with:
  - TipBox component removed (creation, hover listeners, positioning)
  - `labelPcOculusPathExplaination` restored
  - 6 `tipbox_top.png` image labels replacing step text groups

### Definition of Done
- [ ] `./gradlew compileJava` passes
- [ ] No `tipBox` references remain in file
- [ ] 6 image labels using `tipbox_top.png` icon present
- [ ] All numbered step text (1.-6.) removed from plain labels and shown on images

### Must Have
- Image labels use `tipbox_top.png` scaled to 500px width (auto-height)
- Text overlaid on image with conthrax-sb font (or Arial bold fallback), white, centered
- Same font loading pattern as TipBox.java header label
- Positions match original step heading Y coordinates
- All `back.add()` calls for removed labels also removed

### Must NOT Have
- No `tipBox` variable, hover handlers, or positioning remaining
- Do NOT modify other files

---

## TODOs

- [ ] 1. Apply all edits to FramePCPatcher.java

  **What to do**: Apply edits to `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`

  **Edit A** — Remove TipBox component creation (line 40):
  Remove: `        TipBox tipBox = new TipBox();\n`

  **Edit B** — Restore `labelPcOculusPathExplaination`:
  After the `pcChooseOriginalPath` block (after `back.add(pcChooseOriginalPath);` at line 131), add:
  ```java

        SpecialLabel labelPcOculusPathExplaination = new SpecialLabel("Choose this to use the original Oculus path", 14);
        labelPcOculusPathExplaination.setLocation(814,225);
        back.add(labelPcOculusPathExplaination);
  ```

  **Edit C** — Remove hover listener from pcChooseOriginalPath:
  Remove the SECOND `addMouseListener` block (lines 123-130):
  ```java
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Automatically find the original Oculus installation path");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
  ```

  **Edit D** — Remove hover listener from pcPatchChoosePath:
  Remove lines 147-154:
  ```java
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Choose a custom folder for the Echo VR patch installation");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
  ```

  **Edit E** — Remove hover listener from pcStartPatch:
  Remove lines 207-214:
  ```java
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Start patching Echo VR with the provided Discord link");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
  ```

  **Edit F** — Remove tipBox positioning (lines 217-219):
  Remove:
  ```
        //Tipbox positionieren und hinzufügen...
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 60);
        back.add(tipBox);
  ```

  **Edit G** — Helper method for creating step header image labels.
  
  At the class level (or within initComponents), create a helper that loads `tipbox_top.png`, scales it to 500px wide (auto-height proportional), and returns a JLabel with the text centered on it:
  
  ```java
  private JLabel createStepHeader(String text, int x, int y) {
      ImageIcon icon = new ImageIcon(loadGUI("tipbox_top.png"));
      int w = 500;
      int h = (int) ((double) icon.getIconHeight() * w / icon.getIconWidth());
      Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
      JLabel label = new JLabel(text, new ImageIcon(scaled), SwingConstants.CENTER);
      label.setHorizontalTextPosition(JLabel.CENTER);
      label.setVerticalTextPosition(JLabel.CENTER);
      label.setBounds(x, y, w, h);
      label.setForeground(Color.WHITE);
      try {
          InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
          if (fontStream != null) {
              Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
              label.setFont(font.deriveFont(Font.PLAIN, 18f));
              fontStream.close();
          } else {
              label.setFont(new Font("Arial", Font.BOLD, 18));
          }
      } catch (Exception e) {
          label.setFont(new Font("Arial", Font.BOLD, 18));
      }
      return label;
  }
  ```

  **Edit H** — Replace step 1 (left column, was `discordLink` at y=40):
  Remove:
  ```java
        SpecialLabel discordLink = new SpecialLabel("1. Join the Echo VR Patcher Disocrd Server:", 16);
        discordLink.setLocation(40, 40);
        back.add(discordLink);
  ```
  Replace with:
  ```java
        back.add(createStepHeader("1. Join the Echo VR Patcher Discord Server:", 40, 40));
  ```

  **Edit I** — Replace step 2 (left column, was `react_discord1` + `react_discord2` at y=135):
  Remove:
  ```java
        SpecialLabel react_discord1 = new SpecialLabel("2. React to the message  on Discord", 16);
        react_discord1.setLocation(40, 135);
        back.add(react_discord1);

        SpecialLabel react_discord2 = new SpecialLabel("by clicking on the disc:", 16);
        react_discord2.setLocation(40, 165);
        back.add(react_discord2);
  ```
  Replace with:
  ```java
        back.add(createStepHeader("2. React to the message on Discord by clicking on the disc:", 40, 135));
  ```

  **Edit J** — Replace step 3 (left column, was `copyLink1`+`copyLink2`+`copyLink3` at y=352):
  Remove:
  ```java
        SpecialLabel copyLink1 = new SpecialLabel("3. You will receive a private Message from the", 16);
        copyLink1.setLocation(40, 352);
        back.add(copyLink1);

        SpecialLabel copyLink2 = new SpecialLabel("\"EchoSignUp\" Bot. Right Click on the file", 16);
        copyLink2.setLocation(40, 382);
        back.add(copyLink2);

        SpecialLabel copyLink3 = new SpecialLabel("and select Copy Link. NOT COPY MESSAGE LINK!", 16);
        copyLink3.setLocation(40, 412);
        back.add(copyLink3);
  ```
  Replace with:
  ```java
        back.add(createStepHeader("3. Right Click the file and select Copy Link - NOT COPY MESSAGE LINK!", 40, 352));
  ```

  **Edit K** — Replace step 4 (right column, was `enterLink` at y=40):
  Remove:
  ```java
        SpecialLabel enterLink = new SpecialLabel("4. Paste the link with CTRL-V:", 16);
        enterLink.setLocation(582, 40);
        back.add(enterLink);
  ```
  Replace with:
  ```java
        back.add(createStepHeader("4. Paste the link with CTRL-V:", 582, 40));
  ```

  **Edit L** — Replace step 5 (right column, was `choosePath1`+`choosePath2` at y=135):
  Remove:
  ```java
        SpecialLabel choosePath1 = new SpecialLabel("5. Choose your path or leave it ", 16);
        choosePath1.setLocation(582, 135);
        back.add(choosePath1);

        SpecialLabel choosePath2 = new SpecialLabel("as it is, if it is correct already:", 16);
        choosePath2.setLocation(582, 165);
        back.add(choosePath2);
  ```
  Replace with:
  ```java
        back.add(createStepHeader("5. Choose your path or leave it as it is, if it is correct already:", 582, 135));
  ```

  **Edit M** — Replace step 6 (right column, was `startPatch_btn1`+`startPatch_btn2` at y=333):
  Remove:
  ```java
        SpecialLabel startPatch_btn1 = new SpecialLabel("6. Start Patching by pressing", 16);
        startPatch_btn1.setLocation(582, 333);
        back.add(startPatch_btn1);

        SpecialLabel startPatch_btn2 = new SpecialLabel("this button:", 16);
        startPatch_btn2.setLocation(582, 363);
        back.add(startPatch_btn2);
  ```
  Replace with:
  ```java
        back.add(createStepHeader("6. Start Patching by pressing this button:", 582, 333));
  ```

  **Important**: Apply edits in this order for the file to stay consistent:
  1. Remove TipBox creation (Edit A) — top of file
  2. Add the `createStepHeader` method before `initComponents` or after it, as a private method of FramePCPatcher
  3. Replace step 1 (Edit H) — near top
  4. Replace step 2 (Edit I) — after hyperlinkPC
  5. Replace step 3 (Edit J) — after copyLinkImg
  6. Replace step 4 (Edit K) — right column
  7. Replace step 5 (Edit L) — right column
  8. Replace step 6 (Edit M) — right column, before buttons
  9. Remove hover listeners + positioning (Edits C-F)
  10. Restore labelPcOculusPathExplaination (Edit B)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **References**:
  - `TipBox.java` — Reference for font loading pattern (conthrax-sb.otf) and image overlay pattern
  - `SpecialLabel.java` — Reference for conthrax-sb font loading

  **QA Scenarios**:
  ```
  Scenario: Compilation succeeds
    Tool: Bash
    Steps:
      1. Run `./gradlew clean compileJava`
    Expected Result: BUILD SUCCESSFUL
    Evidence: .sisyphus/evidence/framepcpatcher-stepheaders-compile.txt

  Scenario: No tipBox references remain
    Tool: Bash
    Steps:
      1. Run `grep -c "tipBox" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`
    Expected Result: 0
    Evidence: .sisyphus/evidence/framepcpatcher-stepheaders-refs.txt

  Scenario: Step text labels removed
    Tool: Bash
    Steps:
      1. Run `grep -c "discordLink\|react_discord1\|react_discord2\|copyLink1\|copyLink2\|copyLink3\|enterLink\|choosePath1\|choosePath2\|startPatch_btn1\|startPatch_btn2" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`
    Expected Result: 0
    Evidence: .sisyphus/evidence/framepcpatcher-stepheaders-gone.txt

  Scenario: Step headers exist
    Tool: Bash
    Steps:
      1. Run `grep -c "createStepHeader" src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`
    Expected Result: 7 (method def + 6 calls)
    Evidence: .sisyphus/evidence/framepcpatcher-stepheaders-count.txt
  ```

  **Commit**: YES
  - Message: `feat: replace step text with tipbox_top.png headers in FramePCPatcher`
  - Files: `FramePCPatcher.java`
