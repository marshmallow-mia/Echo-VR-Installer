# Echo VR Installer — Design Rules

## 1. Colors

| Element | Value |
|---------|-------|
| Section box fill | `Color(200, 0, 150, 90)` |
| Section box border | `Color(50, 50, 50, 150)` |
| TipBox panel | `Color(200, 0, 150, 200)` |
| TipBox inner label bg | `Color(70, 70, 70, 180)`, border `Color(50, 50, 50, 200)` |
| Progress bar box | `Color(100, 0, 50, 220)` |
| Sidebar box fill | `Color(100, 0, 50, 220)` — same as progress bar box |
| Status bar — idle (ready) | `Color(50, 90, 150, 255)` — blue |
| Status bar — working (pulse) | `Color(50,90,150)` ↔ `Color(90,140,210)` — oscillating blue |
| Status bar — done | `Color(40, 130, 40, 255)` — green |
| Step chip — done | `Color(60, 60, 60)` |
| Step chip — current (idle) | `Color(0, 180, 0)` |
| Step chip — current (working pulse) | `Color(0,140,0)` ↔ `Color(0,220,0)` — oscillating green |
| Step chip — upcoming | `Color(40, 40, 40)` |
| Path labels | bg `Color(255, 255, 255, 200)`, fg `Color.BLACK`, rounded arc **8** |
| Path label (valid path) | bg `Color(200, 255, 200, 200)` — green tint |
| Done text | `Color(0, 255, 0)` |
| Error | **red reserved for errors only** |
| Button text idle | `Color(230, 230, 230)` |
| Button text hover | `Color(250, 250, 250)` |

### Alpha usage across the project
| Window | Alpha | Why |
|--------|-------|-----|
| Most dialogs | **90** | Standard |
| FrameMain panels | **150** | Busy background |
| Patchers (complex) | **40** | Overlapping boxes |
| TipBox itself | **200** | Must be readable |
| TipBox inner label | **180** | Distinct from outer bg |
| Progress bar | **220** | Distinct, stands out |
| Sidebar | **220** | Same as progress bar |
| Status bar | **255** | Must be read easily over background image |

## 2. Spacing

- **10px** between any two section boxes
- **10px** minimum from box edge to window edge
- **20px** padding inside a section box (top and bottom margin relative to innermost content)
- **10px** gap between status bar bottom and combined content+tipbox section box top:
  - Status bar: y=**10**, h=**32** → bottom at **42**
  - Combined box top (`bY`): `contentPanel.getY() - 20` = `72 - 20` = **52**
  - Gap: **10px**
- Content + TipBox: **one combined section box**, not separate
- Sidebar + content layout: `[10px edge | sidebar(130px) | 10px gap | content box | 10px edge]`
- Content panel inside section box: **10px** horizontal padding each side (`x = contentBoxX + 10`, `w = contentBoxW - 20`)
- Content panel **top**: at `72` from window top (40px below status bar bottom)

## 3. Sizing

- Backgrounds: `new Background("name.jpg", -1, height)` — **never force-width**
- Frame width: read `back.getWidth()` after background construction, stored in `FW`
- Frame height: `FH` = **594** (PC guidance), dynamic from image ratio (Quest guidance)
- Section boxes: `FW - 30` wide, centered (exceptions: sidebar and content box use fixed x positioning)
- Sidebar: x=**10**, w=**130** (`SIDEBAR_W + 10`), not centered — left-anchored
- Content box: x=**150** (`SIDEBAR_W + 30`), w=**`FW - 160`** (to 10px from right edge)
- Content panel: x=**160**, w=**`contentBoxW - 20`**, y=**72**, h=**245** (hardcoded)
- Combined section box (content+tipbox): full **contentBoxW** wide at x=**contentBoxX** — **not** tight to tipbox width
- TipBox section box: Still full **contentBoxW** wide — overridden from earlier tight-fit design
- Status bar: x=**contentBoxX**, w=**contentBoxW**, h=**32**, arc **8**, at y=**10**
- Section box rounded arc: **15**
- Step chip rounded arc: **8**
- Path label rounded arc: **8**
- Rounded image arc: **10**

## 4. Progress Bar (Navigation Bar)

- **Always at the bottom** of the window. Last item visually. Positioned at y = **FH - 74**.
- Section box height: **42px** (tight wrap of 25px buttons + 24px chips)
- Box fill: `Color(100, 0, 50, 220)` with border `Color(50, 50, 50, 150)`, arc **15**
- Box width: **contentBoxW** (aligned with content section box, not full window)
- Box x: **contentBoxX** (same left edge as content box)
- Nav buttons: `button_up_small.png` series (image is **141×25px**), font **11**, centered in box at `itemY = barY + (42 - 25) / 2`
  - Back button: left-aligned within available left gap
  - Next button: right-aligned within available right gap
- Chips: drawn in a `progPanel`, centered horizontally between nav buttons
  - Gap between chips: **12px**
  - Chip width: dynamic, `Math.max(40, Math.min(74, textWidth + 16))`
  - Arrow between chips: **">"** using `Arial 12`, color `Color.GRAY`
- **Gap** between nav buttons and chips: dynamically computed from remaining space
- **Clickable**: any chip can be clicked to jump to that step (with confirmation if work in progress)

### Chip Labels (per platform)

**PC guidance** (`FrameGuidancePC` — 6 steps):
```
Type → Play → Path → Download → Patch → Done
```

**Quest guidance** (`FrameGuidanceQuest` — 4 steps):
```
Type → Download → Install → Done
```

### Step Chip Colors

| State | bg | fg |
|-------|----|----|
| Done (completed, i < currentStep) | `Color(60, 60, 60)` | `Color.LIGHT_GRAY` |
| Current (idle) | `Color(0, 180, 0)` | `Color.WHITE` |
| Current (working) | pulsing green `(0,140,0)` ↔ `(0,220,0)` | `Color.WHITE` |
| Upcoming | `Color(40, 40, 40)` | `Color.WHITE` |

- **Pulsing**: when the current step is actively working (downloading, extracting, patching), the chip's green channel oscillates via a `javax.swing.Timer` at **50ms** using `Math.sin(animPhase)`. The same timer also drives the status bar animation.

## 5. Status Bar

- **Position**: top of window at y=**10**, spans content area width (x=**contentBoxX**, w=**contentBoxW**)
- **Height**: **32px**
- **Arc**: **8**
- **Label**: `Arial` bold **14**, white `Color.WHITE`, centered, `setOpaque(false)` — **z-ordered on top** of the bar box
- **Z-order**: always topmost (`setComponentZOrder(label, 0)`)
- **Text**: context-aware per step/substep, updated via `updateStatusText()`:

  **PC guidance:**
  - Step 0: "Choose your player type"
  - Step 1: "How do you launch Echo VR?"
  - Step 2: "Choose your Echo VR install path"
  - Step 3: "Ready to download" → downloader updates → "Complete"
  - Step 4 sub 0 (owner): "Apply optional patches"
  - Step 4 sub 0 (new player): "Authorize with Discord to generate your patch"
  - Step 4 sub 1: "Apply Steam patch for Revive compatibility"
  - Step 5: "Echo VR installation complete!"
  - During OAuth2: "Discord authorization opened in your browser." → "Generating your patch file..." → "Downloading patch file..." → "License patch applied!" / "Patch applied successfully!"
  - During Steam patch: "Installing Revive..." → "Revive installed successfully!"

  **Quest guidance:**
  - Step 0: "Choose your player type"
  - Step 1: "Ready to download" → "Downloading..." → "Complete"
  - Step 2: "Install to Quest" → "Installation complete!" / "Installation failed"
  - Step 3: "Echo VR installation complete!"

- **Three-state coloring** (background fill):
  | State | Color | Trigger |
  |-------|-------|---------|
  | **Idle** (ready) | `Color(50, 90, 150, 255)` — blue | Default; `showStep()` resets here |
  | **Working** (pulse) | `Color(50,90,150)` ↔ `Color(90,140,210)` | `stepInProgress=true` |
  | **Done** | `Color(40, 130, 40, 255)` — green | `stepCompleted=true` after work finishes |
- **Animation**: a single `javax.swing.Timer` at **50ms** increments `animPhase` and calls `progPanel.repaint()` + `statusBarBox.repaint()`. The `Math.sin()` function produces a smooth sine-wave pulse. Started on work begin, stopped on completion.
- **State resets**: navigating via `showStep()` (back, forward, chip click) resets both `stepInProgress=false` and `stepCompleted=false`, returning to idle blue.

## 6. Sidebar

- **Position**: left-anchored at x=**10** (10px from window edge), width **130px** (`SIDEBAR_W + 10` for 5px internal padding each side)
- **Height**: matches the combined content+tipbox section box height (`bH`), aligned at same `bY`
- **Fill**: `Color(100, 0, 50, 220)` — same as progress bar box
- **Inner panel**: y+**10**, h-**20** (10px top/bottom padding), x+5, w=`SIDEBAR_W`=**120**
- **Step number label**: `conthrax-sb.otf` bold **13**, white, at (8, 12) in sidebar panel
- **Substep labels**: `Arial` **14** (plain), bounds: x=**8**, y=`38 + i * 22`, w=`SIDEBAR_W - 16`, h=**22**
  - Completed: `✓ ` prefix, `Color.GRAY` — **clickable** navigates to that substep (y hit test: `38 + i * 18` with 16px height)
  - Current: `● ` prefix, `Color(0, 180, 0)`
  - Upcoming: `○ ` prefix, `Color.WHITE`
  - Substep label array dynamically resized if step has more substeps than initially allocated (min 3)
- Shows only the **current step**'s substeps (updated via `updateSidebar()`)

## 7. Headers (Step Questions)

- Background: `tipbox_top.png`, **450px** wide (auto-scaled to maintain aspect ratio, min height 55px)
- Font: `conthrax-sb.otf`, size **14**, white text
- **Centered horizontally** in content panel
- HTML table wrapping for multi-line text using `<table width='400'>` wrapper
- `setHorizontalTextPosition`, `setVerticalTextPosition` both `CENTER`

## 8. Buttons

| Role | Image series | Font size | Example |
|------|-------------|-----------|---------|
| Primary | `button_up.png` | **18** | "I own Echo on Meta", "Start Download" |
| Secondary | `button_up_middle.png` | **14** | "No Licence Patch", "Steam Patch (Revive)" |
| Small action | `button_up_small.png` | **11–12** | "Choose path", "← Back" |
| Nav | `button_up_small.png` | **11** | "Back", "Next", "Finish" |

- All buttons **centered horizontally** in their section
- Every button has a **TipBox hover tip** via `mouseEntered`/`mouseExited` listeners
- Button image series: 3-state (up/down/highlighted) with corresponding image name pattern

## 9. Navigation

- Back: **disabled on step 0 sub 0**, enabled on all others
- Next: **enabled after step requirements met** (checked in `canAdvanceFrom()`)
- Last step (step 5 PC / step 3 Quest): button text **"Finish"**, disposes dialog
- Step 4 "Patch" for PC: on chip click shows confirmation if Echo VR not installed yet
- Advancing from Step 3 (Download) PC: checks if `echovr.exe` exists, prompts to download if not

### Validation gates (`canAdvanceFrom`):

**PC guidance:**
- Step 0: requires `wizardState.getUserType() != null`
- Step 1: requires `wizardState.getPlayStyle() != null`
- Step 3: if echovr.exe not found, prompts to start download
- Other steps: always return true

**Quest guidance:**
- Step 2: requires `stepCompleted` (installation must finish)
- Other steps: always return true

## 10. Content Panel

- `JPanel` with null layout, `opaque=false`
- Content switches per step: `removeAll()` → `buildContent()` → `revalidate()` + `repaint()`
- Base content area: 245px height, located at y=72
- Path labels: `makeRoundedLabel()` — `SpecialLabel` subclass with white bg `(255,255,255,200)`, black text, rounded arc **8**
- Default path: Windows → `C:/EchoVR`, Linux/Mac → `<userdir>/echovr`
- Download URL: `https://files.echovr.de/` (base)
- Download files: `ready-at-dawn-echo-arena.zip` (PC), `r15_26-06-25.apk` + `_data.zip` (Quest)
- Extract/unzip only for PC (platform=0) — Quest download does not auto-extract
- Download progress: `SpecialLabel` with percentage text
- **Path validation**: `updatePathStatus()` shows a ✓ (green, bold 28) or ✗ (red, bold 18) indicator next to the path. Valid paths tint the label background green `Color(200, 255, 200, 200)`. Invalid paths keep default white background. Validation checks for existence of `echovr.exe` at `<path>/ready-at-dawn-echo-arena/bin/win10/echovr.exe`.

## 11. PC Guidance — Step Structure (`FrameGuidancePC`)

### Step 0: Type Selection
- Header: "Do you own Echo VR on your Meta account?"
- Two buttons: "I own Echo on Meta" (OWNER) / "I'm a new player" (NEW_PLAYER)
- Sets `wizardState.setUserType()` and auto-advances

### Step 1: Play Style
- Header: "How do you play Echo VR?"
- Two buttons: "SteamVR (Revive)" / "Meta Link"
- Sets `wizardState.setPlayStyle()` — only shown for PC (Quest skips this)
- SteamVR path has 2 substeps in step 4, Meta Link has 1

### Step 2: Path Selection
- Header: "Choose your Echo VR install path"
- Path label (440px wide) + "Choose path" button
- `pathFolderChooser()` opens JFileChooser for directories

### Step 3: Download
- Header: "Download Echo VR client files"
- Path label + "Start Download" button
- Downloads `ready-at-dawn-echo-arena.zip` via `Downloader`
- Unzips automatically (platform=0)
- Button toggles between "Start Download" / "Cancel Download"

### Step 4: Patch (Master-Detail)
**Master view (sub 0):**
- Owner: shows "Optional patches" header with two buttons:
  - "No Licence Patch" → detail view (OAuth2 + download `pnsovr.dll`)
  - "Steam Patch (Revive)" → detail view (download + run ReviveInstaller.exe)
- New Player: shows "Patch Menu" header:
  - "Licence Patch" button → OAuth2 + download flow
  - "Steam Patch (Revive)" button (if playstyle=STEAMVR)
  - Auto-advances to Licence Patch inline on first arrival (when `justArrivedAtStep4`)

**Detail views:**
- Each has a "← Back" button returning to master view
- Licence Patch inline: path chooser with validation, OAuth2 authorization button
- Steam Patch detail: info text, "Start Install" button → downloads `ReviveInstaller.exe` from GitHub, runs it silently. Requires admin rights.

### Step 5: Done
- "You're all set!" in green
- "Echo VR is ready to play."
- Next button says "Finish"

### Auto-advance logic:
- Owner + SteamVR on Step 3→4: automatically shows steam patch detail (substep 1)
- New Player arriving at Step 4: auto-shows Licence Patch inline view
- After OAuth2 download for new player: returns to patch menu (`buildStep4AfterOAuth`)

## 12. Quest Guidance — Step Structure (`FrameGuidanceQuest`)

### Step 0: Type Selection
- Same as PC: "I own Echo on Meta" / "I'm a new player"

### Step 1: Download
- Owner: Downloads `r15_26-06-25.apk` + `_data.zip` in parallel
- New Player: OAuth2 flow → downloads patched APK → then downloads `_data.zip`
- Two progress labels: APK and data
- Tracks `downloadCompleteCount`, both must finish before advancing

### Step 2: Install to Quest
- Header: "Install Echo VR to your Quest"
- "Install to Quest" button → uses `InstallerQuest` class
- Requires `stepCompleted` from download step
- Installation runs on background thread

### Step 3: Done
- "You're all set!" + "Echo VR is ready to play on your Quest."

## 13. OAuth2 Discord Flow (Licence Patch)

- Triggered by "Authorize with Discord" or "Licence Patch" button
- Opens **system default browser** (not embedded WebView) to Discord OAuth2 authorize URL
  - Client ID: `1326594571584409650`
  - Redirect URI: `http://127.0.0.1:53124/callback`
  - Scopes: `identify guilds`
- Starts a **temporary HTTP server on localhost:53124** to capture the OAuth2 callback (300s timeout)
- On redirect, extracts `?code=XXX` from the URL, sends to `POST https://files.echovr.de/api/exchange`
  - Body: `{"code":"<code>","type":"<fileType>"}`
  - File types: `"dll"` (PC), `"apk"` (Quest)
- Server exchanges code → verifies guild membership → generates patch file → returns `{"patchUrl": "..."}`
- **No cookie persistence needed** — flow is fully stateless
- Status bar shows progress: "Discord authorization opened..." → "Generating your patch file..." → "Downloading patch file..." → Success text
- On error:
  - `403 not_in_guild`: "You must join the Echo VR Patcher server first."
  - `409 busy`: "Bot is busy. Try again in 30 seconds."
  - Other errors: generic error dialog
- Button re-enables after completion or error via `resetAfterError()`

## 14. Wizard State Model

```
WizardState (base)
├── userType: OWNER | NEW_PLAYER
├── installPath: String (normalized, forward slashes)
├── getInstallPath() / setInstallPath()
│
├── PCWizardState extends WizardState
│   ├── playStyle: STEAMVR | META_LINK
│   ├── getPlayStyle() / setPlayStyle()
│   └── getBinPath() → "<installPath>/ready-at-dawn-echo-arena/bin/win10"
│
└── QuestWizardState extends WizardState
    ├── apkFilename (default: "r15_26-06-25.apk")
    ├── adbDeviceStatus (-1 = unknown)
    ├── isPatchedApk (true after OAuth2 download)
    └── getters/setters
```

## 15. FrameMain Entry Points

- **"Install Echo VR" button** (PC side, centered left): opens `new FrameGuidancePC(this)`
- **"Update Echo (PC)" button**: opens `new FramePCEchoUpdate(this)` — standalone legacy dialog
- **"Quest Install Echo" button** (right side): opens `new FrameGuidanceQuest(this)`
- Background frames panel (below buttons, hidden by default): legacy `FramePCPatcher` and `FrameSteamPatcher`
- Utility buttons (hidden by default): "Get Quest Logs", "Delete cache"
- FrameMain stays unchanged behind modal guidance dialogs

## 16. General

- All interactive elements MUST have hover tips
- Every logical section gets a rounded section box
- Font: `conthrax-sb.otf` for headers/chips, `Arial` for body text
- Helpers utility class provides: `loadGUI()`, `centerFrame()`, `pathFolderChooser()`, `pause()`, `prepareAdb()`, `checkForAdmin()`, OS detection booleans (`isWindows`, `linux`, `mac`)
- `BaseWizard` abstract class provides shared wizard infrastructure:
  - Window init with background, status bar, content panel, tip box, combined section box
  - `sectionBox()` and `sectionBoxAt()` for creating rounded section panels
  - `makeHeader()` for step question headers
  - `makeRoundedLabel()` for path labels with rounded background
  - `addRoundedImage()` for images with rounded corners and 10px arc border
  - `buildSidebar()` and `updateSidebar()` for sidebar panel
  - `buildBar()` for progress bar with nav buttons and step chips
  - `showStep()`, `advance()`, `goBack()`, `onChipClick()` for navigation
  - `confirmAbortDownload()` — confirmation dialog when navigating during active work
  - `resetAfterError()` — resets state after failed operation
- `Downloader` supports: resume (Range header), cancel, progress label updates, platform-based unzip, hash verification
- `SpecialButton` — image-based 3-state button with conthrax font
- `SpecialLabel` — label with font loading and configurable size
- `TipBox` — composite component with `tipbox_top.png` header image and grey tip label
