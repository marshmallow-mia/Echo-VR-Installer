# FrameQuestPatcher — Rounded Image Corners + Border

## TL;DR
> **Quick Summary**: Replace the two `addBackgroundImage` calls in FrameQuestPatcher with inline anonymous `Background` subclasses (same pattern as FramePCPatcher) that clip images to rounded corners + draw border. Remove the now-unused `addBackgroundImage` helper.

---

## Changes

**File**: `/home/mia/IdeaProjects/Echo-VR-Installer/src/main/java/bl00dy_c0d3_/echovr_installer/FrameQuestPatcher.java`

### Edit A — Replace `addImages` method (lines 208-211)
Replace:
```java
    private void addImages(JPanel back) {
        addBackgroundImage(back, "quest_react.png", 40, 215, 182, 108);
        addBackgroundImage(back, "copy_linkQuest.png", 40, 465, 279, 177);
    }
```
with:
```java
    private void addImages(JPanel back) {
        Background questReactImg = new Background("quest_react.png") {
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
        questReactImg.setLocation(40, 215);
        questReactImg.setSize(182, 108);
        questReactImg.setVisible(true);
        back.add(questReactImg);

        Background copyLinkQuestImg = new Background("copy_linkQuest.png") {
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
        copyLinkQuestImg.setLocation(40, 465);
        copyLinkQuestImg.setSize(279, 177);
        copyLinkQuestImg.setVisible(true);
        back.add(copyLinkQuestImg);
    }
```

### Edit B — Remove `addBackgroundImage` helper (lines 199-205)
Remove:
```java
    private void addBackgroundImage(@NotNull JPanel back, String imagePath, int x, int y, int width, int height) {
        Background image = new Background(imagePath);
        image.setLocation(x, y);
        image.setSize(width, height);
        image.setVisible(true);
        back.add(image);
    }
```

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] No `addBackgroundImage` references remain
- [ ] Both images use `setClip(RoundRectangle2D)` + `drawRoundRect` border

**Evidence**: `.sisyphus/evidence/framequestpatcher-image-borders.txt`

## Commit
- Message: `feat: add rounded image corners + border to FrameQuestPatcher`
- Files: `FrameQuestPatcher.java`
