# Echo VR Installer — Design Rules

## 1. Colors

| Element | Value |
|---------|-------|
| Section box fill | `Color(200, 0, 150, 90)` |
| Section box border | `Color(50, 50, 50, 150)` |
| TipBox panel | `Color(200, 0, 150, 200)` |
| Progress bar box (unique) | `Color(100, 0, 50, 220)` |
| Status bar — idle (ready) | `Color(50, 90, 150, 255)` — blue |
| Status bar — working (pulse) | `Color(50,90,150)` ↔ `Color(90,140,210)` — oscillating blue |
| Status bar — done | `Color(40, 130, 40, 255)` — green |
| Step chip — done | `Color(60, 60, 60)` |
| Step chip — current (idle) | `Color(0, 180, 0)` |
| Step chip — current (working pulse) | `Color(0,140,0)` ↔ `Color(0,220,0)` — oscillating green |
| Step chip — upcoming | `Color(40, 40, 40)` |
| Path labels | bg `Color(255, 255, 255, 200)`, fg `Color.BLACK`, rounded arc **8** |
| Done text | `Color(0, 255, 0)` |
| Error | **red reserved for errors only** |

### Alpha usage across the project
| Window | Alpha | Why |
|--------|-------|-----|
| Most dialogs | **90** | Standard |
| FrameMain panels | **150** | Busy background |
| Patchers (complex) | **40** | Overlapping boxes |
| TipBox itself | **200** | Must be readable |
| Progress bar | **220** | Distinct, stands out |
| Status bar | **255** | Must be read easily over background image |

## 2. Spacing

- **10px** between any two section boxes
- **10px** minimum from box edge to window edge
- **10px** vertical gap between all items within a step's content panel
- **20px** padding inside a section box (top and bottom equal)
- Progress bar box: **+4px** height above items (tight fit)
- Content + TipBox: **one combined box**, not separate
- Sidebar + content layout: `[10px edge | sidebar(130px) | 10px gap | content box | 10px edge]`
- Status bar + content: **10px** gap between status bar bottom and content section box top

## 3. Sizing

- Backgrounds: `new Background("name.jpg", -1, height)` — **never force-width**
- Frame width: read `back.getWidth()` after background construction
- Section boxes: `FW - 30` wide, centered (exceptions: sidebar and content box use fixed x positioning)
- Sidebar: x=**10**, w=**130** (`SIDEBAR_W + 10`), not centered — left-anchored
- Content box: x=**150** (`SIDEBAR_W + 30`), w=**`FW - 160`** (to 10px from right edge)
- Content panel: x=**160**, w=**`contentBoxW - 20`** (10px horizontal padding inside box)
- TipBox section box: `tipBox.getWidth() + 20px` wide only — **not full width**
- Status bar: x=**contentBoxX**, w=**contentBoxW**, h=**32**, arc **8**, on top of window at y=**10**
- Section box rounded arc: **15**
- Step chip rounded arc: **8**

## 4. Progress Bar

- **Always at the bottom** of the window. Last item visually.
- Section box height: **42px** (tight wrap of 25px buttons + 24px chips)
- Box fill: `Color(100, 0, 50, 220)` with border `Color(50, 50, 50, 150)`, arc **15**
- Box width: **contentBoxW** (aligned with content section box, not full window)
- Box x: **contentBoxX** (same left edge as content box)
- Nav buttons: `button_up_small.png` series (image is **141×25px**), font **11**, centered in box at `itemY = barY + (42 - 25) / 2`
- **Gap** between nav buttons and chips: **7px** on each side (tight for 540px box)
- Items centered **horizontally** via `(sectionW - totalWidth) / 2`
- **Clickable**: completed steps can be clicked to jump back to that step's last substep

## 5. Step Chips

- Size: **52×24px**, rounded corners arc **8**
- Chip vertical position: **9px** from top of bar (`(42 - 24) / 2`)
- **Gap between chips**: **12px**
- Total chip area width: `4 × 52 + 3 × 12 = 244px`
- Arrow between chips: **">"** using `Arial 12` (conthrax-sb.otf lacks the → glyph)
- Labels: `Type → Install → Patch → Done`
- Font: `conthrax-sb.otf`, size **9**
- Text vertically centered within chip via `FontMetrics`: `((chipH - fm.getHeight()) / 2) + fm.getAscent()`
- Colors:
  - Done: bg `Color(60, 60, 60)`, fg `Color.LIGHT_GRAY`
  - Current (idle): bg `Color(0, 180, 0)`, fg `Color.WHITE`
  - Current (working): bg pulses green via `Math.sin()` timer animation — `(0,140,0)` ↔ `(0,220,0)`
  - Upcoming: bg `Color(40, 40, 40)`, fg `Color.WHITE`
- **Pulsing**: when the current step is actively working (downloading, extracting, patching), the chip's green channel oscillates via a `javax.swing.Timer` at 50ms. The same timer also drives the status bar animation.

## 6. Sidebar

- **Position**: left-anchored at x=**10** (10px from window edge), width **130px** (`SIDEBAR_W + 10` for 5px internal padding each side)
- **Height**: matches the content section box height (`bH`), aligned at same `bY`
- **Fill**: `Color(100, 0, 50, 220)` — same as progress bar box
- **Inner panel**: y+**10**, h-**20** (10px top/bottom padding), x+5, w=`SIDEBAR_W`=**120**
- **Step number label**: `conthrax-sb.otf` bold **13**, white, at top of panel
- **Substep labels**: `Arial` **10**, stacked every **18px** starting at y=**38** from panel top
  - Completed: `✓ ` prefix, `Color.GRAY` — **clickable** navigates to that substep
  - Current: `● ` prefix, `Color(0, 180, 0)`
  - Upcoming: `○ ` prefix, `Color.WHITE`
- Shows only the **current step**'s substeps (updated via `updateSidebar()`)
- Step 3 (new player): single substep `"Authorize & Patch"` (consolidated from old 3-substep join/react/paste flow)
- Step 3 (owner): single substep `"Optional Patches"`

## 7. Status Bar

- **Position**: top of window at y=**10**, spans content area width (x=contentBoxX, w=contentBoxW)
- **Height**: **32px**
- **Arc**: **8**
- **Label**: `Arial` bold **14**, white `Color.WHITE`, centered, `setOpaque(false)` — on top of the box
- **Z-order**: always topmost (`setComponentZOrder(label, 0)`)
- **Text**: context-aware per step/substep, updated via `updateStatusText()` in `showStep()`:
  - Step 0: "Choose your player type"
  - Step 1 sub 0: "Choose your Echo VR install path"
  - Step 1 sub 1: "Ready to download" → downloader updates → "Complete"
   - Step 2 (owner): "Apply optional patches"
   - Step 2 (new player): "Authorize with Discord to generate your patch"
   - Step 3: "Echo VR installation complete!"
   - During OAuth2 flow: "Discord authorization opened in your browser. Complete it there." → "Downloading patch file..." → "Patch applied successfully!"
   - During error: "Patch failed. Try again."
  - During extract: "Extracting..." → "Extraction complete"
- **Three-state coloring** (background fill):
  | State | Color | Trigger |
  |-------|-------|---------|
  | **Idle** (ready) | `Color(50, 90, 150, 255)` — blue | Default; `showStep()` resets here |
  | **Working** (pulse) | `Color(50,90,150)` ↔ `Color(90,140,210)` | `stepInProgress=true` |
  | **Done** | `Color(40, 130, 40, 255)` — green | `stepCompleted=true` after work finishes |
- **Animation**: a single `javax.swing.Timer` at **50ms** increments `animPhase` and calls `statusBarBox.repaint()` + `progPanel.repaint()`. The `Math.sin()` function produces a smooth sine-wave pulse. Started on work begin, stopped on completion.
- **State resets**: navigating via `showStep()` (back, forward, chip click) resets both `stepInProgress=false` and `stepCompleted=false`, returning to idle blue.

## 8. Headers (Step Questions)

- Background: `tipbox_top.png`, **450px** wide
- Font: `conthrax-sb.otf`, size **14**, white text
- **Centered horizontally** in content panel
- HTML table wrapping for multi-line text

## 9. Buttons

| Role | Image series | Font size | Example |
|------|-------------|-----------|---------|
| Primary | `button_up.png` | **18** | "I own Echo", "Start Download" |
| Secondary | `button_up_small.png` | **11–12** | "Choose path", "Auto choose" |
| Nav | `button_up_small.png` | **11** | "Back", "Next" |

- All buttons **centered horizontally** in their section
- Every button has a **TipBox hover tip**

## 10. Navigation

- Back: **disabled on step 0**, enabled on all others
- Next: **enabled after step requirements met**
- Last step: button text **"Finish"**, disposes dialog

## 11. Content

- `JPanel` with null layout, `opaque=false`
- Content switches per step: `removeAll()` → build → `revalidate()` + `repaint()`
- Path labels: `SpecialLabel` with white bg `(255,255,255,200)`, black text, rounded arc **8**
- URL fields: `SpecialTextfield`, 380–480px wide
- Discord links: `SpecialHyperlink`
- Default path: Windows → `C:/EchoVR`, Linux/Mac → `<appdir>/echovr`
- Download URL: `https://files.echovr.de/ready-at-dawn-echo-arena.zip`
- Extract/unzip runs inline (synchronous, no separate popup window) — status bar shows progress
- **Path validation**: Each step with a path chooser shows a ✓ (green, bold 18) or ✗ (red, bold 18) indicator next to the path. Valid paths also tint the path label background green `Color(200, 255, 200, 200)`. Invalid paths keep default white background.

## 12. OAuth2 Discord Flow (Step 3 — New Player)

- Triggered by "Authorize with Discord" button in step 3
- Opens **system default browser** (not embedded WebView) to Discord OAuth2 authorize URL
- Starts a **temporary HTTP server on localhost:53124** to capture the OAuth2 callback
- On redirect, extracts `?code=XXX` from the URL, sends to `POST https://files.echovr.de/api/exchange`
- Server exchanges code → verifies guild membership → generates patch file via existing `create_dll()`/`create_apk()` → returns CDN URL
- **No cookie persistence needed** — flow is fully stateless
- Status bar shows progress: "Discord authorization opened..." → "Downloading patch file..." → "Patch applied successfully!"
- On error, button re-enables, status bar shows "Patch failed. Try again."
- Server enforces guild membership check — non-members see error dialog with invite link

## 13. General

- All interactive elements MUST have hover tips
- Every logical section gets a rounded section box
- FrameMain stays unchanged behind modal guidance dialog
- Font: `conthrax-sb.otf` for headers/chips, `Arial` for body text
