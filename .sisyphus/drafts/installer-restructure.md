# Draft: Echo VR Installer Restructure

## Requirements (from plan file /home/mia/Documents/echo_installer_plan.md)
1. Start on the PCVR side. The "Install Echo" button should NOT open the install window directly.
2. Guide the user through steps:
   - **Step 1**: Ask user if they own EchoVR on their Meta account, or if they want to patch as a new user
   - **Step 2**: Open install window, let user install Echo
   - **Step 3**: Add "Next" button on install window. If new user → open patch window
     - Use same path from install window
     - Patch window "redone completely" - guide user through every step
   - **Step 3a**: Discord integration - open Discord inside a window, let user login
   - **Step 3b**: Redirect to Echo VR Patcher Discord (https://discord.gg/KqjqdNUaHR)
   - **Step 3c**: Tell user to click "Quest Patch" reaction
   - **Step 3d**: Move into private thread, copy patch URL

## Current Codebase Understanding

### Entry Point: EchoVRInstaller.java → FrameMain.java
- FrameMain has two sections (PC + Quest) with buttons

### PC Side Frames:
- **FramePCDownload**: Install window (choose path → download echo zip → update files)
- **FramePCEchoUpdate**: Update existing Echo installation
- **FramePCPatcher**: 6-step guided patch (external Discord link → react → copy link → paste → choose path → patch)
- **FrameSteamPatcher**: Download & install Revive for SteamVR

### Quest Side Frames:
- **FrameQuestDownload**: Download Echo for Quest
- **FrameQuestPatcher**: 6-step guided patch (similar to PC patcher)

### Key Observations:
- FramePCPatcher ALREADY has a 6-step guided flow with numbered sections
- Current patcher uses external browser (SpecialHyperlink) for Discord
- FramePCDownload doesn't know about user type (owner vs new)
- No path sharing between FramePCDownload and FramePCPatcher currently

## Technical Decisions

### Platform Scope
- PCVR only (Quest restructure later)

### Wizard Flow (Multi-Window Sequence)
- Window 1: User-type question dialog ("Own Echo on Meta?" vs "New player?")
- Window 2: Modified FramePCDownload with "Next" button added
- Window 3 (if new user): Redesigned FramePCPatcher with embedded Discord WebView

### Main Window Changes
- Hide: No licence patch, Steam Patch (Revive), Quest Install Echo, Quest No licence patch, Delete cache, Get Quest Logs
- Keep: Update Echo (PC) button only
- Add: New wizard entry point replacing "Step 1: Install Echo"

### Discord Integration
- JavaFX WebView as SEPARATE popup window
- Hybrid approach: Auto-navigate to correct channel/message + highlight → User clicks reaction → Auto-detect private thread → Auto-extract patch URL
- JavaFX dependencies already in build.gradle (commented out) - just need to uncomment

### Patcher Redesign
- Replace external browser hyperlink steps with embedded Discord WebView
- Implement clipboard detection / JavaScript injection for auto URL extraction
- Pass install path from FramePCDownload to patcher automatically

### Post-Install (Owner Flow)
- If user OWNS Echo: "Next" shows optional patches (No licence, Steam/Revive) as next steps

### Testing
- TDD for ALL new code
- JUnit 5 (already configured in build.gradle)
- No tests exist currently - build test infrastructure from scratch
- Agent-executed QA scenarios for UI verification

### Build System
- Gradle with Java 17 modules
- JavaFX 17.0.2 (uncomment from build.gradle)
- jlink/jpackage for distribution

## Scope Boundaries
- INCLUDE: PCVR wizard flow, JavaFX WebView Discord integration, semi-auto Discord interaction, TDD for new code, main window simplification
- EXCLUDE: Quest side restructure, changes to FramePCEchoUpdate, changes to FrameSteamPatcher (used as optional post-install), Discord bot backend, Quest APK installation
