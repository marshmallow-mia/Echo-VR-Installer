# FramePCPatcher — Fix Image Borders (Remove Overlay, Keep Rounded Corners + Border)

## TL;DR
> **Quick Summary**: Remove the purple `fillRoundRect` overlay from both images. Instead, clip the image to rounded corners via `setClip(RoundRectangle2D)` + draw border with `drawRoundRect`.

---

## Context

The current code adds a semi-transparent purple fill overlay over the images. The user only wants:
1. Image corners rounded (clipped via RoundRectangle2D)
2. Border color drawn around the image (Color(50,50,50,255))
3. NO purple fill overlay

## Change

**File**: `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`

### Edit A — Fix `reactToMessageImg` paintComponent (lines 50-62)

**Old:**
```java
        Background reactToMessageImg = new Background("pc_react.png") {
            @Override
            public void paintComponent(Graphics g) {
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
```

**New:**
```java
        Background reactToMessageImg = new Background("pc_react.png") {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
```

### Edit B — Fix `copyLinkImg` paintComponent (lines 73-85)

Same change — remove the fillRoundRect, add setClip with RoundRectangle2D, keep the drawRoundRect border.

**Old:**
```java
        Background copyLinkImg = new Background("copy_linkPC.png") {
            @Override
            public void paintComponent(Graphics g) {
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
```

**New:**
```java
        Background copyLinkImg = new Background("copy_linkPC.png") {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
```

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] No `fillRoundRect` calls in FramePCPatcher.java
- [ ] Both paintComponent overrides use `setClip(RoundRectangle2D)` before `super.paintComponent`
- [ ] Both still have `drawRoundRect` border

**Evidence**: `.sisyphus/evidence/framepcpatcher-image-fix.txt`

## Commit
- Message: `fix: round image corners with clip instead of overlay in FramePCPatcher`
- Files: `FramePCPatcher.java`
