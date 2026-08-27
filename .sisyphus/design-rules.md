# Echo VR Installer — Design Rules

> **Last updated**: June 2026 — reflects actual implementation as of v0.9.3b.
> Sections marked **→ IMPLEMENTED** were previously in "Future Features" and are now live in code.

---

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
| Path indicator valid (✓) | `Color(80, 255, 0)` — bright green, Arial Bold 28 |
| Path indicator invalid (✗) | `Color(255, 80, 80)` — error red, Arial Bold 18 |
| Status bar label fg | `Color.WHITE`, Arial Bold 14, centered, opaque=false |
| SpecialLabel default bg | `Color(60, 70, 100, 200)` |
| SpecialTextfield bg | `Color(30, 30, 30, 200)`, fg `Color.WHITE` |
| Rahmen1 panel fill | `Color(200, 0, 150, 150)` |

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
- Rahmen1 panel inner padding: **15px**
- Rahmen1 corner arc: **20**

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

### Animation timing
- Progress animator (`javax.swing.Timer`): **50ms** interval, `animPhase += 0.15f` per tick
- `Math.sin(animPhase)` produces smooth sine-wave pulse for both status bar and step chip animations

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
- **Chip font**: conthrax-sb.otf (fallback Arial) at **9px** for text, arrow separator at **12px**

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
- **Label** (`dlProgressLabel`): `Arial` bold **14**, white `Color.WHITE`, centered, `setOpaque(false)` — **z-ordered on top** of the bar box
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
  - During Steam patch (chained, per selected row): "Installing Revive..." → "Creating Revive shortcut..." → "Updating Revive manifest..." → "Restoring dashboard entry..." → "Installing game artwork..." → "Revive setup complete!"

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
- **Animation**: a single `javax.swing.Timer` at **50ms** increments `animPhase += 0.15f` and calls `progPanel.repaint()` + `statusBarBox.repaint()`. The `Math.sin()` function produces a smooth sine-wave pulse. Started on work begin, stopped on completion.
- **State resets**: navigating via `showStep()` (back, forward, chip click) resets both `stepInProgress=false` and `stepCompleted=false`, returning to idle blue.

## 6. Sidebar

- **Position**: left-anchored at x=**10** (10px from window edge), width **130px** (`SIDEBAR_W + 10` for 5px internal padding each side)
- **Height**: matches the combined content+tipbox section box height (`bH`), aligned at same `bY`
- **Fill**: `Color(100, 0, 50, 220)` — same as progress bar box
- **Inner panel**: y+**10**, h-**20** (10px top/bottom padding), x+5, w=`SIDEBAR_W`=**120**
- **Step number label**: `conthrax-sb.otf` bold **13**, white, at (8, 12) in sidebar panel
- **Substep labels**: `Arial` **14** (plain), x=**8**, w=`SIDEBAR_W - 16`. **Long names wrap to multiple lines** instead of cropping, via `BaseWizard.wrapToWidth(lbl, prefix, text, maxPx)` — a deterministic `FontMetrics` word-wrap that emits HTML with explicit `<br>` breaks (Swing's CSS `width` wrapping is honoured inconsistently, so it's not relied on). The ✓/●/○ **prefix sits in its own table cell** so wrapped lines **hang-indent** — every text line starts at the same x, just past the prefix. Each row is sized to its wrapped `getPreferredSize().height` (min 22) and the rows below **reflow** down (cumulative y from 38, +4px gap). Names are HTML-escaped (`& < >`).
  - Completed: `✓ ` prefix, `Color.GRAY` — **clickable** navigates to that substep (hit-test against each label's actual bounds, since row heights vary)
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
| Primary | `button_up.png` | **18** | "I own Echo on Meta", "Start Download", "Authorize with Discord", "Add Desktop Shortcut", "Quest Install Echo" |
| Secondary | `button_up_middle.png` | **14** | "No Licence Patch", "Steam Patch (Revive)", "Start Install", "Update Echo (PC)", "Update Echo (Quest)" |
| Small action | `button_up_small.png` | **11–12** | "Choose path", "← Back" |
| Nav | `button_up_small.png` | **11** | "Back", "Next", "Finish" |

- All buttons **centered horizontally** in their section
- Every button has a **TipBox hover tip** via `mouseEntered`/`mouseExited` listeners
- Button image series: 3-state (up/down/highlighted) with corresponding image name pattern
- FrameMain uses font size **20** for main Install buttons, **15** for "Update Echo (PC)" and "Update Echo (Quest)", **17** for utility buttons
- Step 5 Done step: "Add Desktop Shortcut" and "Open Install Folder" use primary image series, font size **18**

## 9. Special Components

| Component | Default Background | Default Foreground | Font | Notes |
|-----------|-------------------|-------------------|------|-------|
| SpecialLabel (base) | `Color(60, 70, 100, 200)` | `Color.WHITE` | conthrax-sb.otf → Arial | Base class default; **overridden in path labels** (see §11) |
| SpecialHyperlink | none (transparent) | `Color.WHITE` | conthrax-sb.otf | Hand cursor, opens URL via `Desktop.browse()` |
| SpecialCheckBox | none (opaque=false) | `Color.WHITE` | conthrax-sb.otf | Flat border paint. Used for the Step 4 Steam Patch row toggles (size 14), each paired with a ○/●/✓/✗ status glyph (see §12) |
| SpecialTextfield | `Color(30, 30, 30, 200)` | `Color.WHITE` | conthrax-sb.otf → Arial | Dark translucent bg, sized via `specialTextfield(w,h,x,y,textSize)`. **Not opaque** + overrides `paintComponent` to fill a rounded (arc 8) translucent bg each repaint — same anti-ghosting pattern as SpecialLabel (an opaque JTextField filling a translucent colour leaves old text visible on change). Supports grey **placeholder** text via `setPlaceholder()` (drawn only while empty, so `getText()` stays genuinely empty). Used for the editable/pastable path fields and the patch-options URL box (the latter tints green/red via `setBackground` on live validation). |
| SpecialButtonSmall | 3-state image panel | `Color(230, 230, 230)` / `Color(250, 250, 250)` | conthrax-sb.otf **20** | Identical to SpecialButton but hardcoded font size 20; **unused in current wizard flow** |
| SpecialButtonInvisible | opaque true | label fg `Color.WHITE`, label bg `Color.BLUE` | n/a (hardcoded) | **Incomplete/debug component** — not used in production flow |

> **Cross-reference**: `makeRoundedLabel()` in `BaseWizard` (§11) overrides SpecialLabel background to `Color(255, 255, 255, 200)` for path labels. `updatePathStatus()` further overrides to `Color(200, 255, 200, 200)` for valid paths.

## 10. Navigation

- Back: **disabled on step 0 sub 0**, enabled on all others
- Next: **enabled after step requirements met** (checked in `canAdvanceFrom()`)
- Last step (step 5 PC / step 3 Quest): button text **"Finish"**, disposes dialog
- Step 4 "Patch" for PC: on chip click shows confirmation if Echo VR not installed yet
- Advancing from Step 3 (Download) PC: checks if `echovr.exe` exists, prompts to download if not
- Advancing with active download: calls `advanceWithConfirm()` → shows confirmation dialog → cancels download if confirmed

### Validation gates (`canAdvanceFrom`):

**PC guidance:**
- Step 0: requires `wizardState.getUserType() != null`
- Step 1: requires `wizardState.getPlayStyle() != null`
- Step 3: if echovr.exe not found, prompts to start download; if user declines, advances to step 4
- Other steps: always return true

**Quest guidance:**
- Step 0: requires `questState.getUserType() != null`
- Step 2: requires `stepCompleted` (installation must finish)
- Other steps: always return true

### Confirmation dialogs
- `confirmAbortDownload()`: "Installation is still in progress. Abort and continue?" — YES cancels download and resets, NO stays
- Step 3 PC existing install: "Echo VR is already installed. Overwrite?" — YES proceeds, NO cancels
- Step 4 chip click (PC): if `echovr.exe` not found — "Echo needs to be installed first. Go to download step?"
- Step 5 PC "Add Desktop Shortcut": if no install path — error dialog "Please download Echo VR first"

## 11. Content Panel

- `JPanel` with null layout, `opaque=false`
- Content switches per step: `removeAll()` → `buildContent()` → `revalidate()` + `repaint()`
- Base content area: 245px height, located at y=72
- Path labels: `makeRoundedLabel()` — `SpecialLabel` subclass with white bg `(255,255,255,200)`, black text, rounded arc **8**
- Default path: Windows → `C:/EchoVR`, Linux/Mac → `<userdir>/echovr`
- Download URL: `https://files.echovr.de/` (base)
- Download files: `ready-at-dawn-echo-arena.zip` (PC), `r15_26-06-25.apk` + `_data.zip` (Quest)
- Extract/unzip only for PC (platform=0) — Quest download does not auto-extract
- Download progress: `SpecialLabel` with percentage text
- **Multi-server**: auto-selects fastest of 2 CDN mirrors (`files.echovr.de`, `evr.echo.taxi`) via speed test before download
- **Editable/pastable path fields**: path fields are **editable `SpecialTextfield`s** (dark bg `Color(30,30,30,200)`, white text), not read-only labels — users can type or paste a path. Re-validation runs on **Enter** and on **focus loss** (`commitPathField()`): the typed/pasted value is resolved to the install root, stored, saved, and the ✓/✗ indicator refreshed. Because the textfield has a fixed dark background, the old green "valid path" tint no longer applies; the `markIcon()` ✓/✗ indicator is the sole validity signal.
- **Failproof path resolution**: `Helpers.resolveEchoInstallRoot(selected)` accepts the install root, the `ready-at-dawn-echo-arena` folder itself, any folder deeper inside it (bin, win10…), a sibling subfolder, or a folder one–two levels above, and resolves the actual install ROOT (walk-up + bounded downward search for `ready-at-dawn-echo-arena/bin/win10/echovr.exe`). Used by the path fields and the "Choose path" picker so a mis-pick no longer triggers a "Check your path" dead end.
- **Patch-stage path display**: licence-patch path field shows the **full path up to `ready-at-dawn-echo-arena`** (root + `/ready-at-dawn-echo-arena`); install/download stages show the root.
- **Path validation**: `updatePathStatus()` shows a **vector-drawn** check (`Color(80, 255, 0)`, 26px) or cross (`Color(255, 80, 80)`, 22px) icon next to the path via `markIcon()` — drawn with `Graphics2D` strokes, **not** a unicode glyph, because Windows Arial lacks U+2713/U+2717 (they render as empty "tofu" boxes). The same `markIcon()` draws the ✓/✗ states in the Steam Patch checklist (§12). Validation checks for existence of `echovr.exe` at `<path>/ready-at-dawn-echo-arena/bin/win10/echovr.exe`.
- **Clear-on-click indicators**: on every user-editable input field (path fields + the patch-options URL box), the ✓/✗ `markIcon` indicator doubles as a **one-click clear** (`BaseWizard.wireClearOnClick()`) — clicking it empties the field and re-validates (hand cursor; TipBox: *"Click the check / cross icon to clear this field"*). Pairs with the clipboard Paste affordance.
- **Path persistence**: install path saved to `~/.echovr_installer/paths.properties` via `Helpers.saveInstallPath()` / `Helpers.loadInstallPath()`

## 12. PC Guidance — Step Structure (`FrameGuidancePC`)

### Step 0: Type Selection
- Header: "Do you own Echo VR on your Meta account?"
- Two buttons: "I own Echo on Meta" (OWNER) / "I'm a new player" (NEW_PLAYER)
- Sets `wizardState.setUserType()` and auto-advances (`advance()`)

### Step 1: Play Style
- Header: "How do you play Echo VR?"
- Two buttons: "SteamVR (Revive)" / "Meta Link"
- Sets `wizardState.setPlayStyle()` — only shown for PC (Quest skips this)
- SteamVR path has 2 substeps in step 4, Meta Link has 1

### Step 2: Path Selection
- Header: "Choose your Echo VR install path"
- Path label (440px wide) + "Choose path" button + "Detect Meta path" button (both `button_up_small`, 11, stacked & centered)
- `pathFolderChooser()` opens JFileChooser for directories
- On path chosen: saves via `Helpers.saveInstallPath()`, auto-advances to step 3
- Loads saved path on entry, or defaults to `C:/EchoVR` (Windows) / `<cwd>/echovr` (other)
- **"Detect Meta path"** (`detectMetaInstallPath()`, Windows-only): reads the Meta/Oculus install base from the registry via `Helpers.checkForAdminAndOculusPath()` (needs admin), sets the path to `<Base>/Software/Software` and saves it **regardless** of whether Echo is present. If `echovr.exe` is not found there, shows a warning dialog telling the user to install Echo from the Meta Store **and start it once** to use their own licence. Errors if no Meta/Oculus install is in the registry.

### Step 3: Download
- Header: "Download Echo VR client files"
- Path label (440px wide) + path indicator (✓/✗) + "Start Download" button
- Downloads `ready-at-dawn-echo-arena.zip` via `Downloader`
- Unzips automatically (platform=0)
- Button toggles between "Start Download" / "Cancel Download"
- Existing install detection: if `echovr.exe` already exists, asks to overwrite
- `triggerDownload()`: sets `stepInProgress=true`, starts animator, updates status to "Downloading..."

### Step 4: Patch (Master-Detail)
**Master view (sub 0):**
- Owner: shows "Optional patches" header with two buttons:
  - "No Licence Patch" → detail view (OAuth2 + download `pnsovr.dll`)
  - "Steam Patch (Revive)" → detail view (download + run ReviveInstaller.exe)
- New Player: shows "Patch Menu" header:
  - "Licence Patch" button → OAuth2 + download flow
  - "Steam Patch (Revive)" button — **always shown** (not playstyle-gated), so the back button from any patch substep always returns to the full overview with every patch button visible
  - Auto-advances to Licence Patch inline on first arrival (when `justArrivedAtStep4`)

**Detail views:**
- Each has a "← Back" button returning to master view
- Licence Patch inline: editable/pastable arena-path field (full path up to `ready-at-dawn-echo-arena`) with live validation + ✓/✗ indicator, "Choose path" picker, and a **patch-options panel**. The patched `pnsovr.dll` is staged in the temp dir and **reused on Retry** (no re-auth); on a path/copy failure the temp file is kept so a corrected Retry reuses it. **Every element is centred on the window's vertical axis** (path field, Choose path, the panel and its contents) — varied widths read as intentional when symmetric.
  - **Patch-options panel** (`BaseWizard.buildPatchOptionsPanel()`, shared with Quest): one tinted, bordered `sectionBoxAt` (440-wide, centred) that groups the **"Authorize with Discord"** primary button (becomes **"Retry"** after the first attempt), a centred **"Advanced Options"** toggle `SpecialCheckBox`, and a URL row. The panel's edges give the varied-width buttons a shared alignment. URL row = a `SpecialTextfield` with placeholder *"Paste patch URL here…"*, a live ✓/✗ validity indicator (**click it to clear the field**, see §11), and a **clipboard icon** (`clipboardIcon()`, drawn — not an asset) that pastes from the clipboard. **The URL row is hidden and the box collapses until "Advanced Options" is ticked, then the box expands and the row becomes usable** (`collapsedH` = pad+button+gap+checkbox+pad ≈ 94; `expandedH` adds gap+24 ≈ 124) — so it never shows dead space or an inactive field. **Ticking it also relabels the primary button from "Authorize with Discord" → "Start Patching"** (and the click switches from OAuth to using the pasted URL), so the button always matches what it will do. The custom-patch path is taken when the field is **enabled** (checkbox ticked); a valid URL (Discord-CDN `pnsovr.dll` link or `files.echovr.de`) **bypasses OAuth**. Hovering the URL row shows the TipBox: *"Paste a direct URL to use a custom patch instead of generating one."*
  - **Live URL validation** (`BaseWizard.wireUrlValidation()`): as the user types/pastes, a ✓ (`Color(80,255,0)`) / ✗ (`Color(255,80,80)`) `markIcon` appears beside the field and the field tints **done-green `Color(40,130,40,210)`** / **error-red `Color(150,45,45,210)`**; empty resets to the neutral dark bg with no icon — the same idiom as the path indicator.
  - Layout (content height 245px; header y=4/h=42 like the Steam-patch detail, Back button shows through; **even 8px gaps**): 440-wide editable arena-path field at y=54 (✓/✗ indicator right); **"Choose path" (small)** at y=86; **patch-options panel** at y=119 (collapsed h≈94 · expanded h≈124 → ends ≈243 < 245). (Note: PC content width is wide — the background scales to FH so `FW`≈1055 → content ≈875px — so path/URL fields use the fixed 440px column, never `cx`-relative widths.)
- Steam Patch detail: **checkbox-driven, chained Revive setup** (covers the `steampatch.md` megathread). Four `SpecialCheckBox` rows (each with a ○/●/✓/✗ status glyph to its right) **and** the **"Install & Configure"** button (`button_up_middle`, 14) are all **enclosed in one rounded section box** via `sectionBoxAt(...)` (fill `Color(200,0,150,90)`, border `Color(50,50,50,150)`, arc 15, `opaque=false`, `contains()→false` — visual only; rows/button sit on top as siblings, box sent to back via `setComponentZOrder`). A download-% `SpecialLabel` lives inside the box too but is **hidden until the chain runs** (a `SpecialLabel` always paints its bg, so leaving it visible-but-empty would render as a stray box). Requires admin rights and a present `echovr.exe`; Windows-only.
- Step 4 master view with **no type selected yet** (e.g. reached via chip click before choosing OWNER/NEW_PLAYER): renders the same canonical "Optional patches" menu as OWNER (No Licence Patch + Steam Patch (Revive)), so the menu is consistent in every scenario rather than dropping into the new-player licence-inline view.
  - **Patch rows & default state** (defaults are global, user-overridable):

    | Row (execution order) | Action | Default |
    |---|---|---|
    | Install Revive | download + run `ReviveInstaller.exe` (**pinned to v3.1.1**) | ✅ on |
    | Revive injector shortcut | desktop `.lnk` → `ReviveInjector.exe "<echovr.exe>" -nosymbollookup /app ready-at-dawn-echo-arena` | ✅ on |
    | Restore Dashboard entry | `.json`/`.mini` → `Meta Horizon\Manifests` (returning players) | ☐ off |
    | Fix game artwork | download + unzip assets → `…\StoreAssets\ready-at-dawn-echo-arena_assets` | ✅ on |

  - **Removed**: the `Patch revive.vrmanifest` row (gson upsert) was removed from the chain/UI. `ReviveSetup.patchVrManifest()` / `AdminBroker.patchVrManifest()` remain in the backend but are no longer invoked by the wizard.

  - **Status glyphs**: ○ pending `Color.LIGHT_GRAY`, ● working `Color(0,180,0)`, ✓ done `Color(0,255,0)`, ✗ failed `Color(255,80,80)` (Arial Bold 14).
  - **Chain**: runs only the checked rows in fixed order on a worker thread; checkboxes disabled during the run; reuses `stepInProgress`/`progressAnimator`/`stepCompleted` wiring. Backed by `ReviveSetup` (registry-based `findReviveDir()` + verify, `createInjectorShortcut()`, `installArtwork()`, guarded `restoreDashboardManifests()`). Revive is no longer auto-started/stopped during the chain (that only existed to populate the now-removed vrmanifest patch).

### Step 5: Done → IMPLEMENTED (was Future Feature)
- "You're all set!" in green (Arial Bold 24, at y=20)
- "Echo VR is ready to play." (Arial Plain 16, at y=70)
- **"Add Desktop Shortcut" button**: creates Windows `.lnk` or Linux `.desktop` shortcut via `Helpers.createDesktopShortcut(wizardState.getExePath())`
- **"Open Install Folder" button**: opens `bin/win10` folder via `Helpers.openFolder(wizardState.getBinPath())`
- Both buttons use `button_up.png` (primary image series), font size 18, centered
- Both show ErrorDialog if no install path is set
- Next button says "Finish"

### Auto-advance logic:
- Owner + SteamVR on Step 3→4: automatically shows steam patch detail (substep 1)
- New Player arriving at Step 4: auto-shows Licence Patch inline view
- After OAuth2 download for new player: returns to patch menu (`buildStep4AfterOAuth`)

### Substep names (sidebar):
| Step s | sub=0 | sub=1 |
|--------|-------|-------|
| 0 | "Choose Type" | — |
| 1 | "How do you play?" | — |
| 2 | "Choose Path" | — |
| 3 | "Download" | — |
| 4 (Owner) | "Optional Patches" | "Steam Patch" |
| 4 (New Player) | "Authorize & Patch" | "Steam Patch" |
| 5 | "All Done" | — |

## 13. Quest Guidance — Step Structure (`FrameGuidanceQuest`)

### Step 0: Type Selection
- Same as PC: "I own Echo on Meta" / "I'm a new player"
- Resets `questState.setUserType(null)` on every entry, forcing re-selection

### Step 1: Download
- Owner: Downloads the APK named by the Quest manifest's `# BASE_APK:` header + `_data.zip` in parallel via two `Downloader` instances. The manifest is fetched on entry (`fetchQuestManifest()`); when it can't be reached, `QuestWizardState.apkFilename`'s built-in default is used and the post-install update is skipped.
- New Player: OAuth2 flow → downloads patched APK → then downloads `_data.zip`
- Two progress labels: APK and data (white bg, black text, 440px wide)
- Tracks `downloadCompleteCount` (synchronized), both must finish before advancing
- **On all downloads complete**: `onAllDownloadsComplete()` stops animator, enables Next, sets status "Complete"
- Owner button: "Start Download" / "Cancel Download"
- New Player button: "Authorize with Discord" / "Cancel"

### Step 2: Install to Quest
- Header: "Install Echo VR to your Quest"
- **"Connect to Quest" button** + status row with a ✓/✗ `markIcon` hook: on entry the page auto-runs `InstallerQuest.checkConnection()` (background thread) and shows the hook (✓ "Quest connected" / ✗ "not authorized" / ✗ "No Quest detected"). Pressing "Connect to Quest" re-checks and, if unauthorized/not-detected, shows the styled guidance dialog.
- "Install to Quest" button → uses `InstallerQuest` class
- Requires `stepCompleted` from download step
- Installation runs on background thread
- Progress label shows: "Not started yet" → "Installation started! Wait!" → "Installation is complete!" / "Installation did not finish!"
- Status bar: "Installation complete!" / "Installation failed"
- **ADB flow** (detailed in `InstallerQuest.java`):
  1. `prepareAdb()` — extracts platform-tools per platform
  2. `checkQuestStatus()` — verifies device is connected (0=OK, 1=unauthorized, -1=not found)
  3. Verify APK and _data files exist
  4. `adb kill-server`
  5. `adb devices`
  6. `adb uninstall com.readyatdawn.r15`
  7. `adb shell rm -rf /sdcard/readyatdawn` — **legacy** cleanup of the pre-`Android/media` location
  8. `adb install -g <apk>`
  9. `adb shell mkdir -p /sdcard/Android/media/com.readyatdawn.r15/files/_local`
  10. `adb shell chmod -R 777 /sdcard/Android/media/com.readyatdawn.r15/files`
  11. `adb push _data.zip /data/local/tmp` (with transfer validation)
  12. `adb shell mv /data/local/tmp/_data.zip /sdcard/Android/media/com.readyatdawn.r15/files/`
  13. `adb shell cd /sdcard/Android/media/com.readyatdawn.r15/files/; unzip _data.zip`
  14. `adb shell cd /sdcard/Android/media/com.readyatdawn.r15/files/; rm _data.zip`
  15. `adb shell chmod -R 777 /sdcard/Android/media/com.readyatdawn.r15/files`
  16. Grant permissions: `appops`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `RECORD_AUDIO`
  17. `adb kill-server`
  18. `recordInstalledVersion()` — hashes the staged APK locally and writes the base-version marker to the headset
  19. `QuestUpdateService.applyUpdates(...)` — applies the Quest manifest (label reads "Applying update...")

  Steps 4–17 live in `InstallerQuest.installAPK`; 18–19 in `FrameGuidanceQuest`. All adb paths resolve through `Adb.path()` / `Adb.binary()`.
- Includes auto-reconnect on device disconnect during push/install phases
- Post-install warning: "DON'T CLICK ON RESTORE IF YOU WILL GET ASKED TO OR YOU NEED TO REINSTALL AGAIN!"

### Step 3: Done
- "You're all set!" in green (Arial Bold 24, at y=55 — lower than PC's y=20)
- "Echo VR is ready to play on your Quest." (Arial Plain 16, at y=105)
- Next button says "Finish"

### Quest Update Wizard (`FrameQuestUpdate`)

Standalone Quest update, mirroring `FramePCUpdate`. Reached from FrameMain's "Update Echo (Quest)" button. **3 steps** — "Connect" stands in for PC's "Path", because the on-device location is fixed and needs no user choice.

| Step | Chip | Substep | Content |
|------|------|---------|---------|
| 0 | "Connect" | "Connect to Quest" | Header "Connect your Quest"; ✓/✗ status row + "Connect to Quest" button, auto-checked on entry via the shared `BaseWizard.refreshQuestConnection(...)`. Next enabled only when status == 0 |
| 1 | "Update" | "Update" | Header "Update Echo VR on your Quest". Version gate runs on entry; "Start Update" / "Cancel" |
| 2 | "Done" | "All Done" | "Update applied!" green (Arial Bold 24, y=55) + "Echo VR on your Quest has been updated." (Arial Plain 16, y=105) |

- **Manifest**: `Helpers.QUEST_MANIFEST_URL` = `https://files.echovr.de/updates/quest/update.manifest`. Parsed by `UpdateManifest`, shared with the PC path. Quest-only headers: `# BASE_APK: <name> <sha256>` and `# Target: <abs path>`. Entry paths are validated against a strict charset and the target root against `/sdcard/Android/media/com.readyatdawn.<x>` — both reach `adb shell`, including `rm -rf`.
- **Version gate** (`QuestUpdateService.checkVersion` → pure `decide(...)`): a Quest update is only valid on top of the exact APK the manifest was built for. Because Discord-patched APKs are repacked and can never hash to `BASE_APK`, a marker file on the **headset** (not the PC, so it works from any machine) records which base version an install came from.
- **Marker**: `/sdcard/Android/media/com.readyatdawn.r15/.echo_installer_version`, `key=value` lines (`base_apk`, `base_sha256`, `installed_sha256`, `patched`, `installed_at`, `installer_version`). Written by push, read by `adb shell cat`. Android wipes `/sdcard/Android/media/<pkg>` on uninstall, so it can never outlive its install.

| Installed | Marker | Condition | Result |
|-----------|--------|-----------|--------|
| no | — | — | `NOT_INSTALLED` |
| yes | present | base matches **and** installed hash agrees | **OK** |
| yes | present | base matches, installed hash differs | `MISMATCH` (APK replaced) |
| yes | present | base differs | `MISMATCH` (version out of date) |
| yes | absent | installed hash == manifest base | **OK** + marker back-filled |
| yes | absent | otherwise | `MISMATCH` (no provenance) |

- **Mismatch UX**: `JOptionPane` with "Reinstall Echo VR" / "Cancel". Choosing reinstall `dispose()`s this wizard and opens `FrameGuidanceQuest` via `invokeLater` (so the new modal isn't stacked behind the old one). `NOT_INSTALLED` shows the same dialog — the remedy is identical.
- **Apply** (`QuestUpdateService.applyUpdates`): `del` entries (`rm -rf`) first, then `add` entries. Existing files are hashed on-device in one batched `sha256sum` call and skipped when current; when `sha256sum` is unavailable, skipping is disabled and everything is pushed. Each file downloads to a temp, is SHA-256-verified locally, then `mkdir -p` + `adb push` to the full destination path. `chmod -R 777` runs last and is **never fatal** — `/sdcard` is a synthesized FUSE mount where it may be a no-op.
- **Failure/cancel**: `applyUpdates` takes an `onFailure` callback so an abort returns the step to a retryable state instead of stranding the wizard mid-progress. Cancel takes effect at the next file boundary ("Cancelling after the current file...").

## 14. OAuth2 Discord Flow (Licence Patch)

- Triggered by "Authorize with Discord" or "Licence Patch" button
- Opens **system default browser** (not embedded WebView) to Discord OAuth2 authorize URL
  - Client ID: `1326594571584409650`
  - Redirect URI: `http://127.0.0.1:53124/callback`
  - Scopes: `identify guilds`
- Starts a **temporary HTTP server on localhost:53124** to capture the OAuth2 callback (**60s timeout** — kept short because Discord's in-browser "Service got rate limited" page never redirects back, so the only signal is the callback not arriving; a 60s fail-fast beats a 5-minute hang)
- **Callback socket lifecycle**: bound via `new ServerSocket()` + `setReuseAddress(true)` + `bind(...)` with a short bind-retry loop, and **always closed in a `finally`** (success, timeout, error, cancel). `DiscordOAuth2Flow.cancel()` closes the socket — used before a **Retry** so the port is freed and any pending `accept()` unblocks. This fixes the old "Failed: Already in use: Bind" on retry.
- On redirect, extracts `?code=XXX` from the URL, sends to `POST https://files.echovr.de/api/exchange`
  - Body: `{"code":"<code>","type":"<fileType>"}`
  - File types: `"dll"` (PC), `"apk"` (Quest)
- Server exchanges code → verifies guild membership → generates patch file → returns `{"patchUrl": "..."}`
- **No cookie persistence needed** — flow is fully stateless
- **Patched file staging**: the returned patch is downloaded into `%TEMP%/echo/` (`Helpers.PATCH_TEMP_DIR`) and reused on Retry without re-authorizing. **Reuse is gated on a per-session "downloaded this session" flag** (`licenceTempReady` / `patchedApkReady`), set only when a download actually completes — so a stale temp file from a previous run can NOT short-circuit OAuth on the first click (this was a real bug: OAuth never ran). **Toggling "Advanced Options" resets the flag** (`onToggle` callback) so switching modes always runs the chosen mode rather than reusing the other mode's staged file (e.g. unchecking → "Authorize with Discord" actually runs OAuth, not the URL-downloaded file). A JVM shutdown hook (`Helpers.deletePatchTempFiles()`) wipes the staged `pnsovr.dll` / patched APK when the installer exits or is killed.
- **Button states**: the primary button shows **"Please Wait..."** while an attempt is running, then relabels to **"Retry"** after a failed/aborted attempt and to **"Done"** only on a successful patch (PC + Quest). Toggling "Advanced Options" overrides this with the mode label ("Authorize with Discord" ↔ "Start Patching").
- **Patch-options panel**: a bordered, centred container (`BaseWizard.buildPatchOptionsPanel()`, shared PC+Quest) grouping the "Authorize with Discord" button, an "Advanced Options" toggle checkbox, and a custom-URL row (placeholder field + live ✓/✗ `markIcon` & green/red tint via `wireUrlValidation()` + a drawn clipboard paste icon). The URL row is **hidden and the box collapsed until "Advanced Options" is ticked** (box expands + row becomes usable, and the primary button relabels "Authorize with Discord" → "Start Patching" with its click switching from OAuth to the pasted URL). PC accepts a Discord-CDN `pnsovr.dll` or `files.echovr.de` URL; Quest accepts `files.echovr.de`. When enabled, a valid URL bypasses OAuth and downloads directly. URL-row hover shows a TipBox explaining the alternative.
- Status bar shows progress: "Discord authorization opened..." → "Generating your patch file..." → "Downloading patch file..." → Success text
- On error:
  - `403 not_in_guild`: "You must join the Echo VR Patcher server first." — shows "Join Server" / "Close" option dialog
  - `409 busy`: "Bot is busy. Try again in 30 seconds."
  - `phone_verification_required`: "Discord requires a verified phone number to interact in this server."
  - `timeout`: "Try again in a minute" — likely Discord rate-limiting (callback never arrived)
  - `port_in_use`: callback port couldn't be bound after retries — wait and Retry
  - `cancelled`: silent (user/retry-initiated), no dialog
  - Other errors: generic error dialog
- Button re-enables after completion or error via `resetAfterError()` or `OAuth2ErrorHandler.handleError()`

### OAuth2ErrorHandler Class
- Centralized OAuth2 error handling with machine-readable error codes
- `handleError(Throwable, JDialog, SpecialButton)` — checks OAuth2Exception type and shows appropriate dialog
- `phone_verification_required` error code handled separately (distinct from `not_in_guild`)
- Button re-enabled after any error via `triggerBtn.setEnabled(true)`

### DiscordWebView (JavaFX Embedded Browser)

An alternative embedded-browser approach for Discord interaction, distinct from the OAuth2 system-browser flow.

- **Window**: **1150×680** minimum
- **Sidebar**: **220px** wide, background `#1a1a2e` (dark navy), border `#333`
- **Channel list**: white text, light gray secondary, yellow accents
- **Requires**: JavaFX runtime on classpath (`javafx.embed.swing.JFXPanel`)
- **Note**: This path is not used in the current BaseWizard wizard flow. The primary Discord path is OAuth2 via system browser (this section).

### SelectorConfig — Discord DOM Selectors
- Loads from `discord-selectors.properties` (classpath resource), falls back to defaults
- Configurable selectors: server invite, channel name, channel CSS selector, reaction emoji, message selector, thread selector, URL pattern, timeout
- Used by `DiscordNavigator` for JS-based Discord interaction

### DiscordNavigator — JS-based Discord Automation
- Builds on `DiscordWebView` for programmatic Discord navigation
- Methods: `navigateToServer()`, `findChannel()`, `findReactionMessage()`, `highlightReactionButton()`, `detectPrivateThread()`, `extractPatchUrl()`, `waitForManualPaste()`
- All JavaScript DOM queries via `webView.getEngine().executeScript()`
- Timeout and polling configurable via SelectorConfig
- **Note**: This automated navigation path is experimental/alternative and not used in the current BaseWizard flow

## 15. Auxiliary Modal Dialogs

| Dialog | Size | Background | Purpose |
|--------|------|------------|---------|
| ErrorDialog | **800×200** | `Marcelus.png` | Generic error display. Centered white text + optional centered, **underlined white** help link (transparent — not a blue-on-white box) + centered Close button. `hyperlink` codes: 1=enable Developer Mode, 2=Java Runtime download, 3=allow USB debugging on Quest |
| UserTypeDialog | **500×300** | `Echox720.png` | Pre-wizard owner/new-player selection (legacy, replaced by wizard step 0) |
| OptionalPatchesPanel | **500×350** | `Echox720.png` | Post-install optional patch menu (legacy, replaced by wizard step 4) |
| UnzipDialog | **900×200** | `vr_lounge_banner.png` | Unzip progress dialog (legacy — unzip now inline in downloader) |

> **Known dependency**: `OptionalPatchesPanel` instantiates `FramePCPatcher` and `FrameSteamPatcher`. If these legacy frames are deleted, OptionalPatchesPanel must be updated first or in the same pass.
> **Legacy dialogs**: `FrameQuestDownload`, `FrameQuestPatcher`, `GetLogFilesFromQuest` are all `@Deprecated` (replaced by `FrameGuidanceQuest`).

## 16. Wizard State Model

```
WizardState (base)
├── userType: OWNER | NEW_PLAYER          (enum, can be null)
├── installPath: String (normalized, forward slashes, no trailing /)
├── getInstallPath() / setInstallPath()
├── normalizePath() → replaces backslashes, strips trailing /
│
├── PCWizardState extends WizardState
│   ├── playStyle: STEAMVR | META_LINK   (enum, can be null)
│   ├── getPlayStyle() / setPlayStyle()
│   ├── getBinPath() → "<installPath>/ready-at-dawn-echo-arena/bin/win10"
│   └── getExePath() → getBinPath() + "/echovr.exe"
│
└── QuestWizardState extends WizardState
    ├── apkFilename (default: "r15_26-06-25.apk")
    ├── adbDeviceStatus (-1 = unknown/disconnected)
    ├── isPatchedApk (true after OAuth2 download)
    └── getters/setters
```

### Config Persistence
- Install path saved to `~/.echovr_installer/paths.properties`
- Key: `pc.install.path`
- Persisted via `Helpers.saveInstallPath(String)` / `Helpers.loadInstallPath()`
- Used by FrameGuidancePC step 2 (Path Selection) to pre-fill saved path

## 17. FrameMain Entry Points

### Layout (1280×720 fixed, non-resizable, `Echox720.png` background)

```
y=0   ─── Background (Echox720.png, 1280×720) ───
y=200 ─── "Install Echo VR" (PC, centered left half, button_up.png 20)  |  "Quest Install Echo" (right, x=819, button_up.png 20)
y=240 ─── TipBox (centered)
y=280 ─── "Update Echo (PC)" (same x as PC Install, button_up_middle.png 15)  |  "Update Echo (Quest)" (x=819, button_up_middle.png 15)
y=420 ─── rahmen1 [HIDDEN panel: "No licence patch" + "Steam Patch (Revive)" buttons]
y=430 ─── Easter egg zone (invisible JLabel, 100×100)
y=547 ─── "Get Quest Logs" [HIDDEN, button_up_middle.png 17, x=818]
y=595 ─── delete.png icon (x=770) | "Delete cache" button (VISIBLE, x=818, button_up_middle.png 17) | delete.png icon 2nd (x=770, VISIBLE, no listener)
```

- **"Install Echo VR" button** (PC side, centered left): opens `new FrameGuidancePC(this)`
- **"Update Echo (PC)" button**: opens `new FramePCUpdate(this)` — a `BaseWizard` (Path / Update / Done)
- **"Quest Install Echo" button** (right side): opens `new FrameGuidanceQuest(this)`
- **"Update Echo (Quest)" button** (right side): opens `new FrameQuestUpdate(this)` — a `BaseWizard` (Connect / Update / Done)
- Background frames panel (rahmen1, below buttons, **HIDDEN by default**): legacy `FramePCPatcher` and `FrameSteamPatcher`
- **"Get Quest Logs"**: x=818, y=547, **HIDDEN by default** (`setVisible(false)`)
- **"Delete cache"**: x=818, y=595, **VISIBLE by default** (`setVisible(true)`) — clears temp download files
- FrameMain stays unchanged behind modal guidance dialogs
- Window close: calls `javafx.application.Platform.exit()` then `System.exit(0)`
- Title: `"Echo VR Installer v0.9.3b"`

### Button Positions (detailed)

- **"Install Echo VR" button** (PC): x = `(FRAME_WIDTH / 2 - buttonWidth) / 2` (centered on left half), y=**200**
- **"Update Echo (PC)" button**: same x as PC Install button, y=**280**
- **"Quest Install Echo" button**: x=**819**, y=**200**
- **"Update Echo (Quest)" button**: same x as Quest Install button (**819**), y=**280**
- **Section panel "rahmen1"**: positioned at `((FRAME_WIDTH / 2 - pcPanelW) / 2, 420)`, fill `Color(200, 0, 150, 150)`, arc **20**, inner padding **15**
  - Contains: "No licence patch" (button_up.png, 20) + "Steam Patch (Revive)" (button_up.png, 19)
  - Hidden by default (`setVisible(false)`)

### Hidden Utility Buttons

- **"Get Quest Logs"**: x=**818**, y=**547** (hidden by default — `setVisible(false)`)
- **"Delete cache"**: x=**818**, y=**595** (VISIBLE by default — `setVisible(true)`)

### Other Elements

- **Easter egg**: position (590, 430), size 100×100, invisible JLabel. Click shows: "Never divide by 0!" with title "You found an Easter Egg"
- **TipBox**: centered horizontally at `(FRAME_WIDTH - tipBox.getWidth()) / 2`, y=**240**
- **Commented-out music buttons** (disabled): play.png at (590,90) → plays `EchoLobby.wav` (hardcoded path), stop.png at (657,90)

### Other Entry Points (outside FrameMain)
- **FramePCPatcher**: standalone No Licence Patch dialog (legacy, replaced by wizard step 4)
- **FrameSteamPatcher**: standalone Revive installer dialog (legacy, replaced by wizard step 4)

## 18. General

- All interactive elements MUST have hover tips
- Every logical section gets a rounded section box
- Font: `conthrax-sb.otf` for headers/chips, `Arial` for body text
- `Helpers` utility class provides:
  - `loadGUI(String)` — loads images from classpath resources (returns nullable)
  - `centerFrame(Window, int, int)` — centers a window on screen
  - `pathFolderChooser(SpecialLabel, JDialog)` — opens JFileChooser for directory selection
  - `jsonFileChooser(SpecialLabel, JDialog)` — opens FileDialog for JSON selection (legacy)
  - `pause(int)` — sleep with InterruptedException handling
  - `prepareAdb()` — initializes ADB for Quest communication (extracts platform-tools per OS to temp dir)
  - `checkForAdmin()` — detects admin/root privileges via PowerShell
  - `checkForAdminAndOculusPath(JDialog)` — admin check + reads Oculus registry path
  - `checkForEchoOnKnownPaths(JDialog)` — admin check + searches known install paths for echovr.exe
  - `saveInstallPath(String)` / `loadInstallPath()` — config persistence via `java.util.Properties` in `~/.echovr_installer/paths.properties`
  - `openUrl(String)` — opens URL in system browser with fallback strategies (Desktop.browse → xdg-open → rundll32 → open)
  - `createDesktopShortcut(String)` — creates desktop shortcut (.lnk on Windows via PowerShell, .desktop on Linux)
  - `openFolder(String)` — opens file explorer at given path
  - `runShellCommand(String)` — runs command and returns combined stdout+stderr
  - `runShellCommandWithExitCode(String)` — runs command, consumes output, returns exit code
  - `runShellCommandWithOutput(String)` — runs command, returns stdout
  - `getFileAndReturnArray(String, String)` — downloads a text file and returns lines as array
  - `checkIfChromeOs()` — detects ChromeOS by checking `/opt/google/cros-containers/etc/lsb-release`
  - OS booleans: `isWindows`, `linux`, `mac` (detected at class load via `os.name`)
  - `downloadFile(String, String)` — simple HTTP download utility
  - `readFileToArray(String)` — reads file lines into List
  - `findFileRecursive(File, String)` — recursive file search
- `BaseWizard` abstract class provides shared wizard infrastructure:
  - Window init with background, status bar, content panel, tip box, combined section box
  - `sectionBox()` and `sectionBoxAt()` for creating rounded section panels
  - `makeHeader()` for step question headers
  - `makeRoundedLabel()` for path labels with rounded background
  - `addRoundedImage()` for images with rounded corners and 10px arc border
  - `buildSidebar()` and `updateSidebar()` for sidebar panel
  - `buildBar()` for progress bar with nav buttons and step chips
  - `showStep()`, `advance()`, `goBack()`, `onChipClick()` for navigation
  - `advanceWithConfirm()` — checks `confirmAbortDownload()` before navigating
  - `confirmAbortDownload()` — confirmation dialog when navigating during active work
  - `resetAfterError()` — resets state after failed operation, enables trigger button
  - `buildPatchOptionsPanel()` — bordered centred panel grouping the primary action button + "Advanced Options" toggle checkbox + placeholder URL row (live ✓/✗ + clipboard paste icon); box **collapses when unticked / expands when ticked** (URL row hidden→shown+enabled) and the action button **relabels to the supplied custom label** (e.g. "Start Patching") while ticked; `wireUrlValidation()` — live ✓/✗ + green/red tint on a URL field (greyed when disabled), returns the updater; `wireClearOnClick()` — makes a ✓/✗ indicator clear its field on click (TipBox-explained); `clipboardIcon()` — drawn clipboard glyph; `readClipboardText()` — clipboard string or null
  - Abstract methods: `getBackgroundImage()`, `getWindowWidth()`, `getWindowHeight()`, `getWindowTitle()`, `getStepCount()`, `getSubstepCount(int)`, `getSubstepName(int,int)`, `getChipLabel(int)`, `buildContent(int,int,int)`, `updateStatusText(int,int)`
- `Downloader` supports: resume (Range header), cancel (flg_CancelDownload), progress label updates, platform-based unzip (platform=0 auto-extracts), hash verification (SHA-256 for update files), multi-server speed test, onComplete callback
- `SpecialButton` — image-based 3-state button with conthrax font, dynamically sized from button image
- `SpecialLabel` — label with font loading and configurable size, semi-transparent background painting (prevents text ghosting)
- `TipBox` — composite component with `tipbox_top.png` header image and grey tip label; supports configurable header/tip sizes
- `Background` — scaled image panel with proportional scaling modes (width, height, exact, or native)
- `UnzipFile` — utility class for extracting ZIP archives (used by Downloader for PC platform=0)

### Easter Eggs
- **Invisible click zone**: JLabel at (590, 430), 100×100, shows "Never divide by 0!" message
- **Clippy animation**: double-click the TipBox header or tip area triggers a Clippy character that rises from the TipBox, holds for 2s, then falls back (powered by `ClippyAnimation` — loads `/clippy/anim2.gif` frames)

### Legacy / Deprecated Components
All marked `@Deprecated` or `// TODO: Remove in v0.9.0`:
- `FrameQuestDownload` — replaced by `FrameGuidanceQuest`
- `FrameQuestPatcher` — replaced by `FrameGuidanceQuest`
- `GetLogFilesFromQuest` — replaced by `FrameGuidanceQuest`
- `FramePCPatcher` — replaced by wizard step 4 detail view
- `FrameSteamPatcher` — replaced by wizard step 4 detail view
- `OptionalPatchesPanel` — replaced by wizard step 4 master view
- `UserTypeDialog` — replaced by wizard step 0
- `UnzipDialog` — unzip is now inline in `Downloader`
- `DiscordWebView` / `DiscordNavigator` / `SelectorConfig` — experimental JavaFX path, not used in current flow

## 19. Future Features (Not Yet Implemented)

> **These features are planned but NOT YET IMPLEMENTED.** Design rules here serve as pre-specifications to guide implementation.

### Auto-Admin Privilege Elevation → IMPLEMENTED (was Future Feature)
- **Lazy, broker-based elevation** instead of asking the user to relaunch as admin. Admin-prone operations run in-process first; only if they fail for lack of rights does `AdminBroker` ask for consent (standard `JOptionPane` YES/NO — no more "restart as Admin" `ErrorDialog`) and launch an elevated helper.
- **`AdminHelper`** (`EchoVRInstaller` relaunched with `--admin-helper <portFile> <tokenFile> <parentPid>` via `Start-Process -Verb RunAs`): the privileged server. Listens on `127.0.0.1` only, accepts a single token-authenticated connection, executes a **fixed operation vocabulary** (`PATCH_VRMANIFEST`, `INSTALL_ARTWORK`, `RESTORE_DASHBOARD`, `CREATE_SHORTCUT`, `RUN_INSTALLER`, `PING`) by calling `ReviveSetup`, and exits when the parent dies or the client disconnects. Never executes an arbitrary command string. Logs to `%TEMP%/evr-admin-helper.log`.
- **`AdminBroker`** (client singleton): one UAC prompt launches the helper; it is **reused** for all later operations. `patchVrManifest`/`installArtwork` use the try-in-process-then-elevate pattern; the connection is torn down on app shutdown (sends `SHUTDOWN`).
- Token handshake: client generates a one-time token (delivered to the helper via a temp file), the helper publishes its chosen port via a temp file, the client connects and authenticates. Both temp files are deleted after handshake.
- "Detect Meta path" reads the registry **without** elevation (HKLM read needs no admin); elevation only kicks in when a later write into a protected folder fails.

### Discord Join Redirect
- When user is not in the patcher Discord server during OAuth2, redirect to join the server
- Verify phone verification was completed (server access, not just membership)

### SteamVR Bindings Guide
- Link to bindings help article on the PC Done step
- Default SteamVR bindings for Echo VR are known to be suboptimal

### Desktop Shortcut (Revive variant) → IMPLEMENTED (was Future Feature)
- **Revive shortcut**: `ReviveInjector.exe` with Echo VR args — now created by the Step 4 Steam Patch chain (`ReviveSetup.createInjectorShortcut()`, via `Helpers.createShortcut(name,target,args,workingDir,icon)`)
- **vrmanifest**: ~~create/update `revive.vrmanifest`~~ — **REMOVED from the wizard.** `ReviveSetup.patchVrManifest()` / `AdminBroker.patchVrManifest()` remain in the backend but are no longer wired into the Steam Patch chain or UI.
- **Game artwork**: `ReviveSetup.installArtwork()` downloads + unzips assets into the Meta Horizon StoreAssets folder
- **Revive version**: the installer download is **pinned to v3.1.1** (`releases/download/3.1.1/ReviveInstaller.exe`) in both `FrameGuidancePC` and the legacy `FrameSteamPatcher`, replacing `releases/latest`.
- **Still future**: Dashboard manifest restore (`.json`/`.mini`) is wired but guarded (`restoreDashboardManifests()` throws `UnsupportedOperationException`) pending hosted file URLs
