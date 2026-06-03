# FramePCPatcher — Multi-line Step Headers

## TL;DR
> **Quick Summary**: Modify `createStepHeader()` in FramePCPatcher.java to support multi-line text via HTML `<center>` wrapping, and increase label height to 80px so longer step descriptions wrap properly instead of being clipped.

---

## Context

The current `createStepHeader` method uses plain text and a proportional image height (~45px). Long text like "3. Right Click the file and select Copy Link - NOT COPY MESSAGE LINK!" (~66 chars) overflows at 18pt on 500px width and gets clipped.

## Change

**File**: `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`

**Method**: `createStepHeader` (lines 183-206)

### Changes:
1. **Wrap text in HTML** — Change `new JLabel(text, ...)` to `new JLabel("<html><center>" + text + "</center></html>", ...)`
2. **Increase label height** — Change from `int h = imgH` (proportional ~45px) to `int labelH = Math.max(imgH, 80)` so multi-line text has room to wrap
3. **Variable rename** — Rename `h` to `imgH` for clarity, add `labelH` for the actual bounds height

### Before/After of the method:

**Before:**
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

**After:**
```java
    private JLabel createStepHeader(String text, int x, int y) {
        ImageIcon icon = new ImageIcon(loadGUI("tipbox_top.png"));
        int w = 500;
        int imgH = (int) ((double) icon.getIconHeight() * w / icon.getIconWidth());
        Image scaled = icon.getImage().getScaledInstance(w, imgH, Image.SCALE_SMOOTH);
        // HTML wrapping for multi-line text support
        String htmlText = "<html><center>" + text + "</center></html>";
        JLabel label = new JLabel(htmlText, new ImageIcon(scaled), SwingConstants.CENTER);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.CENTER);
        // Give enough height for multi-line text (at least image height, more for long text)
        int labelH = Math.max(imgH, 80);
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

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] `createStepHeader` uses `<html><center>` wrapping
- [ ] Label height is `Math.max(imgH, 80)` instead of just `h`

**Evidence**: `.sisyphus/evidence/framepcpatcher-multiline.txt`

## Commit
- Message: `fix: support multi-line text in step header images via HTML wrapping`
- Files: `FramePCPatcher.java`
