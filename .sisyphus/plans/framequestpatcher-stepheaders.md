# FrameQuestPatcher — Replace Step Text with tipbox_top.png Headers

## TL;DR
> **Quick Summary**: Replace all numbered step-by-step text labels in FrameQuestPatcher with `tipbox_top.png` image headers (same pattern as FramePCPatcher), plus add the `createStepHeader` helper method.

---

## Context

FrameQuestPatcher has step text in `addSpecialLabels()` using `Helpers.createSpecialLabel()`. Same treatment as FramePCPatcher: replace with `tipbox_top.png` image headers with text overlaid, constrained to 460px inner area.

**Step headers to replace (6 groups):**

Left column (x=40):
- Step 1: "1. Join the Echo VR Patcher Discord Server:" at (40, 40)
- Step 2: "2. React to the message on Discord\nby clicking on the smiley:" at (40, 135) — combined from two labels
- Step 3: "3. You will receive a private Message from the\n\"EchoSignUp\" Bot. Right Click on the blue URL\nand select Copy Link. NOT COPY MESSAGE LINK!" at (40, 335) — combined from three labels

Right column (x=582):
- Step 4: "4. Paste the link with CTRL-V:" at (582, 40)
- Step 5: "5. Start the Download Process:" at (582, 170)
- Step 6: "6. After the Download above is finished, click this button:" at (582, 300)

---

## Changes

**File**: `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FrameQuestPatcher.java`

### Edit A — Add `createStepHeader` method
Add this method before the closing `}` of the class (after line 272):

```java
private JLabel createStepHeader(String text, int x, int y) {
    ImageIcon icon = new ImageIcon(loadGUI("tipbox_top.png"));
    int w = 500;
    int imgH = (int) ((double) icon.getIconHeight() * w / icon.getIconWidth());
    Image scaled = icon.getImage().getScaledInstance(w, imgH, Image.SCALE_SMOOTH);
    // HTML wrapping for multi-line with 20px decorative margins
    String htmlText = "<html><table width='460' align='center'><tr><td align='center'>"
        + text.replace("\n", "<br>") + "</td></tr></table></html>";
    JLabel label = new JLabel(htmlText, new ImageIcon(scaled), SwingConstants.CENTER);
    label.setHorizontalTextPosition(JLabel.CENTER);
    label.setVerticalTextPosition(JLabel.CENTER);
    // Smart height: at least image height, scaled to text length
    int approxLines = Math.max(1, (int) Math.ceil(text.length() / 40.0));
    int labelH = Math.max(imgH, approxLines * 24 + 8);
    label.setBounds(x, y, w, labelH);
    label.setForeground(Color.WHITE);
    try {
        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
        if (fontStream != null) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            label.setFont(font.deriveFont(Font.PLAIN, 15f));
            fontStream.close();
        } else {
            label.setFont(new Font("Arial", Font.BOLD, 15));
        }
    } catch (Exception e) {
        label.setFont(new Font("Arial", Font.BOLD, 15));
    }
    return label;
}
```

### Edit B — Replace step 1 (line 156)
Old: `        back.add(Helpers.createSpecialLabel("1. Join the Echo VR Patcher Discord Server:", 16, 40, 40));`
New: `        back.add(createStepHeader("1. Join the Echo VR Patcher Discord Server:", 40, 40));`

### Edit C — Replace step 2 (lines 157-158)
Old:
```java
        back.add(Helpers.createSpecialLabel("2. React to the message on Discord", 16, 40, 135));
        back.add(Helpers.createSpecialLabel("by clicking on the smiley:", 16, 40, 165));
```
New: `        back.add(createStepHeader("2. React to the message on Discord\nby clicking on the smiley:", 40, 135));`

### Edit D — Replace step 3 (lines 160-162)
Old:
```java
        back.add(Helpers.createSpecialLabel("3. You will receive a private Message from the", 16, 40, 335));
        back.add(Helpers.createSpecialLabel("\"EchoSignUp\" Bot. Right Click on the blue URL ", 16, 40, 365));
        back.add(Helpers.createSpecialLabel("and select Copy Link. NOT COPY MESSAGE LINK!", 16, 40, 395));
```
New: `        back.add(createStepHeader("3. You will receive a private Message from the\n\"EchoSignUp\" Bot. Right Click on the blue URL\nand select Copy Link. NOT COPY MESSAGE LINK!", 40, 335));`

### Edit E — Replace step 4 (line 164)
Old: `        back.add(Helpers.createSpecialLabel("4. Paste the link with CTRL-V:", 16, 582, 40));`
New: `        back.add(createStepHeader("4. Paste the link with CTRL-V:", 582, 40));`

### Edit F — Replace step 5 (line 165)
Old: `        back.add(Helpers.createSpecialLabel("5. Start the Download Process:", 16, 582, 170));`
New: `        back.add(createStepHeader("5. Start the Download Process:", 582, 170));`

### Edit G — Replace step 6 (line 167)
Old: `        back.add(Helpers.createSpecialLabel("6. After the Download above is finished, click this button:", 16, 582, 300));`
New: `        back.add(createStepHeader("6. After the Download above is finished, click this button:", 582, 300));`

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] `createStepHeader` method exists in FrameQuestPatcher
- [ ] No `Helpers.createSpecialLabel` step text calls remain (only non-step ones like progress labels)

**Evidence**: `.sisyphus/evidence/framequestpatcher-stepheaders.txt`

## Commit
- Message: `feat: replace step text with tipbox_top.png headers in FrameQuestPatcher`
- Files: `FrameQuestPatcher.java`
