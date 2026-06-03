# Place Images 10px Below Step Headers

## TL;DR
> **Quick Summary**: Adjust Y positions of step images in FramePCPatcher and FrameQuestPatcher so they sit 10px below the bottom of their respective step headers, calculated from the smart height formula.

---

## Calculations

Smart height = `Math.max(45, ceil(text.length/40) * 24 + 8)`

### FramePCPatcher

| Step | Text | Length | Lines | Height | Header Y | Bottom | Image Y (now) | Image Y (new) |
|---|---|---|---|---|---|---|---|---|
| 2 | "2. React ...\nby clicking on the disc:" | 60 | 2 | 56 | 150 | 206 | 235 | **216** |
| 3 | "3. Right Click ...\n- NOT COPY MESSAGE LINK!" | 69 | 2 | 56 | 352 | 408 | 465 | **418** |

### FrameQuestPatcher

| Step | Text | Length | Lines | Height | Header Y | Bottom | Image Y (now) | Image Y (new) |
|---|---|---|---|---|---|---|---|---|
| 2 | "2. React ...\nby clicking on the smiley:" | 62 | 2 | 56 | 135 | 191 | 215 | **201** |
| 3 | "3. You will receive ...\n\"EchoSignUp\" ...\nand select Copy Link..." | 143 | 4 | 104 | 335 | 439 | 465 | **449** |

---

## Changes

### File 1: FramePCPatcher.java

**Edit A**: `reactToMessageImg.setLocation(199, 235)` → `reactToMessageImg.setLocation(199, 216)`

**Edit B**: `copyLinkImg.setLocation(150, 465)` → `copyLinkImg.setLocation(150, 418)`

### File 2: FrameQuestPatcher.java

**Edit C**: `questReactImg.setLocation(199, 215)` → `questReactImg.setLocation(199, 201)`

**Edit D**: `copyLinkQuestImg.setLocation(150, 465)` → `copyLinkQuestImg.setLocation(150, 449)`

---

## QA
- [ ] `./gradlew compileJava` passes
- [ ] Image Y values match calculated positions

**Evidence**: `.sisyphus/evidence/images-10px-below.txt`

## Commit
- Message: `fix: position step images 10px below their headers`
- Files: `FramePCPatcher.java`, `FrameQuestPatcher.java`
