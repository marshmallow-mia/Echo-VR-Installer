# Center Images Horizontally Below Step Headers

## TL;DR
> **Quick Summary**: Center the step images horizontally within their 500px-wide step sections in both FramePCPatcher and FrameQuestPatcher.

---

## Math

Each step header is 500px wide starting at x=40. Images should be centered in that span:

- 182px wide image → x = 40 + (500 − 182)/2 = **199**
- 279px wide image → x = 40 + (500 − 279)/2 = **150**

---

## Changes

### File 1: FramePCPatcher.java

**Edit A** — `reactToMessageImg`:
Change `reactToMessageImg.setLocation(40, 220)` → `reactToMessageImg.setLocation(199, 220)`

**Edit B** — `copyLinkImg`:
Change `copyLinkImg.setLocation(40, 465)` → `copyLinkImg.setLocation(150, 465)`

### File 2: FrameQuestPatcher.java

**Edit C** — `questReactImg`:
Change `questReactImg.setLocation(40, 215)` → `questReactImg.setLocation(199, 215)`

**Edit D** — `copyLinkQuestImg`:
Change `copyLinkQuestImg.setLocation(40, 465)` → `copyLinkQuestImg.setLocation(150, 465)`

---

## QA
- [ ] Both files compile (`./gradlew compileJava`)
- [ ] Image X coordinates match centered values

**Evidence**: `.sisyphus/evidence/centered-images.txt`

## Commit
- Message: `fix: center step images horizontally below their headers`
- Files: `FramePCPatcher.java`, `FrameQuestPatcher.java`
