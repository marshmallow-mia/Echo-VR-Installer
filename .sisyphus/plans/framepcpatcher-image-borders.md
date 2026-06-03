# FramePCPatcher — Rounded Borders on Images

## TL;DR
> **Quick Summary**: Add semi-transparent rounded overlay + border (same style as TipBox) to the two `Background` images in FramePCPatcher (`pc_react.png` and `copy_linkPC.png`) using anonymous subclass overrides.

---

## Context

The user wants the same rounded border treatment from TipBox applied to the two screenshot images in FramePCPatcher. TipBox uses `fillRoundRect` (semi-transparent purple) + `drawRoundRect` (dark border) at 15px corners.

## Changes

**File**: `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`

**Current code** (lines 50-66 for reactToMessageImg, lines 80-85 for copyLinkImg):

```java
        Background reactToMessageImg = new Background("pc_react.png");
        back.setLayout(null);
        reactToMessageImg.setLocation(40, 220);
        reactToMessageImg.setSize(182,108);
        reactToMessageImg.setVisible(true);
        back.add(reactToMessageImg);
```

and:

```java
        Background copyLinkImg = new Background("copy_linkPC.png");
        back.setLayout(null);
        copyLinkImg.setLocation(40, 465);
        copyLinkImg.setSize(279,177);
        copyLinkImg.setVisible(true);
        back.add(copyLinkImg);
```

### Edit A — Add rounded overlay to `reactToMessageImg`:
Change to anonymous subclass with overridden `paintComponent`:
```java
        Background reactToMessageImg = new Background("pc_react.png") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(200, 0, 150, 100));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        back.setLayout(null);
        reactToMessageImg.setLocation(40, 220);
        reactToMessageImg.setSize(182,108);
        reactToMessageImg.setVisible(true);
        back.add(reactToMessageImg);
```

### Edit B — Add rounded overlay to `copyLinkImg`:
Same treatment:
```java
        Background copyLinkImg = new Background("copy_linkPC.png") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(200, 0, 150, 100));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        back.setLayout(null);
        copyLinkImg.setLocation(40, 465);
        copyLinkImg.setSize(279,177);
        copyLinkImg.setVisible(true);
        back.add(copyLinkImg);
```

**Corner radius**: 15px (matching TipBox overlay)
**Fill**: `Color(200, 0, 150, 100)` — semi-transparent purple (matches TipBox hue, lighter α so image shows through)
**Border**: `Color(50, 50, 50, 255)` — dark grey, opaque (matches TipBox border)

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] Both Background instantiations use anonymous subclasses with `fillRoundRect` + `drawRoundRect`

**Evidence**: `.sisyphus/evidence/framepcpatcher-image-borders.txt`

## Commit
- Message: `feat: add rounded purple overlay + border to images in FramePCPatcher`
- Files: `FramePCPatcher.java`
