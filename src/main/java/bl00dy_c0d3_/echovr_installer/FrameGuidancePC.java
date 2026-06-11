package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameGuidancePC extends BaseWizard {

    private PCWizardState wizardState = new PCWizardState();
    private SpecialTextfield pathField;
    // Cancelled before a Retry so the previous attempt's callback socket is freed (avoids
    // "Already in use: Bind").
    private DiscordOAuth2Flow activeLicenceFlow;
    // True once a patch dll has actually been downloaded in THIS dialog session — only then may a
    // Retry reuse the staged temp file. Without this, a stale pnsovr.dll from a previous run would
    // be reused on the very first click and OAuth would never run.
    private boolean licenceTempReady = false;
    private SpecialButton steamPatchStartBtn;
    private SpecialLabel steamPatchProgressLbl;
    private SpecialCheckBox[] steamPatchBoxes;
    private JLabel[] steamPatchStatus;
    private int patchDetailMode = 0; // 0=master, 1=licence, 2=steam
    private boolean justArrivedAtStep4 = false;
    private JLabel pathIndicator;
    public FrameGuidancePC(FrameMain frameMain) {
        super(frameMain);
        Background back = (Background) getContentPane();
        int bY = contentPanel.getY() - 20;
        int bH = (tipBox.getY() + tipBox.getHeight()) - bY + 20;
        buildSidebar(bY, bH, back);
        buildBar(back, FH - 74);
        pack(); setSize(FW, FH); setLocationRelativeTo(frameMain);
        showStep(0, 0);
        setVisible(true);
    }

    @Override
    protected String getBackgroundImage() { return "EchoArena.jpg"; }

    @Override
    protected int getWindowWidth() { return FW; }

    @Override
    protected int getWindowHeight() { return FH; }

    @Override
    protected String getWindowTitle() { return "Echo VR Installer v0.9.4b"; }

    @Override
    protected int getStepCount() { return 6; }

    @Override
    protected String getChipLabel(int step) {
        String[] chips = {"Type", "Play", "Path", "Download", "Patch", "Done"};
        return step >= 0 && step < chips.length ? chips[step] : "";
    }

    @Override
    protected int getSubstepCount(int s) {
        if (s == 0 || s == 1 || s == 2 || s == 3 || s == 5) return 1;
        if (s == 4) return wizardState.getPlayStyle() == PCWizardState.PlayStyle.STEAMVR ? 2 : 1;
        return 1;
    }

    @Override
    protected String getSubstepName(int s, int sub) {
        switch (s) {
            case 0: return "Choose Type";
            case 1: return "How do you play?";
            case 2: return "Choose Path";
            case 3: return "Download";
            case 4:
                if (sub == 1) return "Steam Patch";
                if (wizardState.getUserType() == WizardState.UserType.NEW_PLAYER) {
                    return "Authorize & Patch";
                }
                return "Optional Patches";
            case 5: return "All Done";
        }
        return "";
    }

    @Override
    protected void showStep(int s, int sub) {
        patchDetailMode = 0;
        justArrivedAtStep4 = (s == 4 && currentStep != 4);
        // Auto-advance to steam patch ONLY when arriving from step 3 (forward navigation).
        // currentStep hasn't been updated yet by super.showStep, so currentStep==3 means
        // we're coming from download step. currentStep==4 means back-button within step 4.
        if (s == 4 && sub == 0 && currentStep == 3
            && wizardState.getUserType() == WizardState.UserType.OWNER
            && wizardState.getPlayStyle() == PCWizardState.PlayStyle.STEAMVR) {
            super.showStep(s, 1);
            return;
        }
        super.showStep(s, sub);
    }

    @Override
    protected void onChipClick(int step) {
        if (step == 4) {
            String checkPath = wizardState.getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10/echovr.exe";
            if (!new File(checkPath).exists()) {
                int choice = JOptionPane.showConfirmDialog(FrameGuidancePC.this,
                    "Echo VR needs to be installed first.\n\nGo to download step?",
                    "Echo not installed", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) showStep(3, 0);
                else showStep(step, 0);
                return;
            }
        }
        super.onChipClick(step);
    }

    @Override
    protected boolean canAdvanceFrom(int step, int sub) {
        if (step == 0 && wizardState.getUserType() == null) {
            JOptionPane.showMessageDialog(FrameGuidancePC.this,
                "Please select whether you own Echo VR or are a new player before continuing.",
                "No Player Type Selected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (step == 1 && wizardState.getPlayStyle() == null) {
            JOptionPane.showMessageDialog(FrameGuidancePC.this,
                "Please select how you launch Echo VR (SteamVR or Meta Link) before continuing.",
                "No Playstyle Selected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (step == 3 && !stepInProgress) {
            String checkPath = wizardState.getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10/echovr.exe";
            System.out.println("ECHO CHECK: path=" + checkPath + " exists=" + new File(checkPath).exists());
            if (!new File(checkPath).exists()) {
                int choice = JOptionPane.showConfirmDialog(FrameGuidancePC.this,
                    "Echo VR hasn't been installed yet.\n\nStart download now?",
                    "Echo VR not found", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    triggerDownload();
                } else {
                    showStep(4, 0);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected void updateStatusText(int s, int sub) {
        switch (s) {
            case 0: dlProgressLabel.setText("Choose your player type"); break;
            case 1: dlProgressLabel.setText("How do you launch Echo VR?"); break;
            case 2: dlProgressLabel.setText("Choose your Echo VR install path"); break;
            case 3: dlProgressLabel.setText("Ready to download"); break;
            case 4:
                if (sub == 1) { dlProgressLabel.setText("Apply Steam patch for Revive compatibility"); break; }
                if (wizardState.getUserType() == WizardState.UserType.OWNER
                    || wizardState.getUserType() == null) {
                    dlProgressLabel.setText("Apply optional patches");
                } else {
                    dlProgressLabel.setText("Authorize with Discord to generate your patch");
                }
                break;
            case 5: dlProgressLabel.setText("Echo VR installation complete!"); break;
        }
    }

    private void triggerDownload() {
        dlProgressLabel.setText("Downloading...");
        nextBtn.setEnabled(false);
        String root = wizardState.getInstallPath();
        stepInProgress = true;
        stepCompleted = false;
        progressAnimator.start();
        if (dlButton != null) dlButton.changeText("Cancel Download");
        if (downloader != null) { downloader.cancelDownload(); pause(1); }
        new Thread(() -> {
            downloader = new Downloader();
            downloader.setOnCompleteListener(() -> {
                final String binPath = root + "/ready-at-dawn-echo-arena/bin/win10";
                final String tempDir = System.getProperty("java.io.tmpdir") + "/echo_update_fresh/";

                SwingUtilities.invokeLater(() -> dlProgressLabel.setText("Applying update..."));

                Downloader updateDl = new Downloader();
                updateDl.setOnCompleteListener(() -> {
                    try {
                        File zipFile = new File(tempDir + "bullet_patch.zip");
                        if (zipFile.exists() && zipFile.length() > 0) {
                            SwingUtilities.invokeLater(() -> dlProgressLabel.setText("Extracting update..."));
                            UnzipFile.unzip(zipFile.getAbsolutePath(), binPath);
                            System.out.println("Update extracted to: " + binPath);
                        }
                    } catch (Exception e) {
                        System.err.println("Update extraction failed: " + e.getMessage());
                    }
                    SwingUtilities.invokeLater(() -> {
                        dlProgressLabel.setText("Installation complete!");
                        nextBtn.setEnabled(true);
                        stepInProgress = false;
                        stepCompleted = true;
                        progressAnimator.stop();
                        progPanel.repaint();
                        statusBarBox.repaint();
                        if (dlButton != null) dlButton.changeText("Start Download");
                    });
                });
                updateDl.startDownload(
                    "https://files.echovr.de/updates/bullet_patch.zip",
                    tempDir, "bullet_patch.zip",
                    dlProgressLabel, FrameGuidancePC.this, frameMain,
                    1, false, -1, true);
            });
            downloader.startDownload("ready-at-dawn-echo-arena.zip", root, "ready-at-dawn-echo-arena.zip", dlProgressLabel, FrameGuidancePC.this, frameMain, 0, false, 0, false);
        }).start();
    }

    @Override
    protected void buildContent(int step, int sub, int cx) {
        switch (step) {
            case 0: buildStep0(cx); break;
            case 1: buildStep1(cx); break;
            case 2: buildStep2(cx); break;
            case 3: buildStep3(cx); break;
            case 4: buildStep4(cx, sub); break;
            case 5: buildStep5(cx); break;
        }
    }

    private void buildStep0(int cx) {
        JLabel h = makeHeader("Do you own Echo VR on your Meta account?");
        h.setBounds((cx - 450) / 2, 8, 450, 55); contentPanel.add(h);

        SpecialButton own = new SpecialButton("I own Echo on Meta", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        own.setLocation((cx - own.getWidth()) / 2, 73);
        own.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { wizardState.setUserType(WizardState.UserType.OWNER); nextBtn.setEnabled(true); advance(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("You already own Echo VR"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(own);

        SpecialButton np = new SpecialButton("I'm a new player", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        np.setLocation((cx - np.getWidth()) / 2, 133);
        np.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { wizardState.setUserType(WizardState.UserType.NEW_PLAYER); nextBtn.setEnabled(true); advance(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("You need to patch Echo VR"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(np);
    }

    private void buildStep1(int cx) {
        JLabel h = makeHeader("How do you play Echo VR?");
        h.setBounds((cx - 450) / 2, 8, 450, 55); contentPanel.add(h);

        SpecialButton steamvr = new SpecialButton("SteamVR (Revive)", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        steamvr.setLocation((cx - steamvr.getWidth()) / 2, 73);
        steamvr.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { wizardState.setPlayStyle(PCWizardState.PlayStyle.STEAMVR); nextBtn.setEnabled(true); advance(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Use this if you launch Echo VR through SteamVR with Revive. A Steam patch will be available in the next step."); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(steamvr);

        SpecialButton meta = new SpecialButton("Meta Link", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        meta.setLocation((cx - meta.getWidth()) / 2, 133);
        meta.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { wizardState.setPlayStyle(PCWizardState.PlayStyle.META_LINK); nextBtn.setEnabled(true); advance(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Use this if you run Echo directly through the Meta Quest Link app on PC."); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(meta);
    }

    /** The current install root, falling back to the saved/default path. */
    private String currentInstallRoot() {
        String p = wizardState.getInstallPath();
        if (p == null || p.isEmpty()) {
            String cfg = Helpers.loadInstallPath();
            p = (cfg != null && !cfg.isEmpty()) ? cfg
                : (Helpers.isWindows ? "C:/EchoVR" : System.getProperty("user.dir") + File.separator + "echovr");
        }
        wizardState.setInstallPath(p);
        return wizardState.getInstallPath();
    }

    /**
     * Reads whatever the user typed/pasted into a path field, resolves it back to the install ROOT
     * (so picking the ready-at-dawn-echo-arena folder, a subfolder, or the root all work), stores +
     * saves it, re-validates the ✓/✗ indicator, and rewrites the field. When {@code arena} is true
     * the field shows the full path up to ready-at-dawn-echo-arena (patch stages); otherwise the root.
     * Returns the resolved root.
     */
    private String commitPathField(SpecialTextfield tf, boolean arena, JLabel indicator) {
        String resolved = Helpers.resolveEchoInstallRoot(tf.getText().trim());
        wizardState.setInstallPath(resolved);
        String root = wizardState.getInstallPath();
        Helpers.saveInstallPath(root);
        // Keep an emptied field genuinely empty (don't re-add the arena suffix to nothing).
        tf.setText(root.isEmpty() ? "" : (arena ? root + "/" + Helpers.ARENA_DIR : root));
        if (indicator != null) updatePathStatus(indicator, root, null);
        return root;
    }

    /** Editable, pastable path field (440px wide, centered) wired to live re-validation. */
    private SpecialTextfield makeEditablePathField(int cx, int y, boolean arena, JLabel indicator) {
        return makeEditablePathField((cx - 440) / 2, y, 440, arena, indicator);
    }

    /** Editable, pastable path field at an explicit x/width, re-validating on Enter and focus loss. */
    private SpecialTextfield makeEditablePathField(int x, int y, int w, boolean arena, JLabel indicator) {
        String root = currentInstallRoot();
        SpecialTextfield tf = new SpecialTextfield();
        tf.specialTextfield(w, 24, x, y, 12);
        tf.setText(arena ? root + "/" + Helpers.ARENA_DIR : root);
        if (indicator != null) {
            updatePathStatus(indicator, root, null);
            // The ✓/✗ also clears the field (then re-validates) — pairs with the patch Paste affordance.
            wireClearOnClick(indicator, tf, () -> commitPathField(tf, arena, indicator));
        }
        tf.addActionListener(e -> commitPathField(tf, arena, indicator));
        tf.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { commitPathField(tf, arena, indicator); }
        });
        return tf;
    }

    private void buildStep2(int cx) {
        JLabel h = makeHeader("Choose your Echo VR install path");
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        pathIndicator = new JLabel();
        pathIndicator.setBounds((cx - 440) / 2 + 445, 64, 90, 34);
        pathIndicator.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(pathIndicator);

        pathField = makeEditablePathField(cx, 70, false, pathIndicator);
        contentPanel.add(pathField);

        SpecialButton pick = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        pick.setLocation((cx - pick.getWidth()) / 2, 102);
        pick.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                String chosen = Helpers.chooseFolder(FrameGuidancePC.this);
                if (chosen == null) return;
                pathField.setText(chosen);
                commitPathField(pathField, false, pathIndicator);
                showStep(3, 0);
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Manually choose install folder — picking the game folder or a subfolder works too"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(pick);

        SpecialButton detect = new SpecialButton("Detect Meta path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        detect.setLocation((cx - detect.getWidth()) / 2, 134);
        detect.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { detectMetaInstallPath(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Auto-detect where Meta/Oculus installs games"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(detect);
    }

    /**
     * Reads the Meta/Oculus install base from the Windows registry, points the install path at
     * that location's game folder, and warns the user if Echo VR isn't actually installed there yet.
     */
    private void detectMetaInstallPath() {
        if (!Helpers.isWindows) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Windows only",
                "Meta/Oculus path detection is only available on Windows.", 0);
            return;
        }
        // Reading the registry base does NOT need admin — elevation is only required later when an
        // operation actually writes into the protected folder (handled by AdminBroker on failure).
        String base = Helpers.getOculusBasePath();
        base = base == null ? "" : base.trim();
        if (base.isEmpty()) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Meta install not found",
                "Could not find a Meta/Oculus installation in the registry. Is the Meta Quest (Oculus) app installed?", 0);
            System.out.println("[PathDetect] Meta/Oculus base not found (base empty)");
            return;
        }

        // Meta games live under <Base>\Software\Software\<app>. setInstallPath normalizes slashes.
        String installPath = base.endsWith("\\") || base.endsWith("/") ? base : base + "\\";
        installPath += "Software\\Software";
        wizardState.setInstallPath(installPath);
        String normalized = wizardState.getInstallPath();
        if (pathField != null) pathField.setText(normalized);
        if (pathIndicator != null) updatePathStatus(pathIndicator, normalized, null);
        Helpers.saveInstallPath(normalized);

        String exe = normalized + "/ready-at-dawn-echo-arena/bin/win10/echovr.exe";
        boolean installed = new File(exe).exists();
        System.out.println("[PathDetect] Meta base=" + base + " -> install path=" + normalized
            + " | echovr.exe exists=" + installed);

        if (!installed) {
            JOptionPane.showMessageDialog(FrameGuidancePC.this,
                "<html>Nice — we found your Meta install folder and set the path to:<br><b>" + normalized + "</b><br><br>"
                + "Echo VR isn't in that folder yet. You've got two easy options:<br><br>"
                + "&nbsp;&nbsp;<b>•</b> Install Echo VR from the Meta Store and launch it once, to use your own licence, or<br>"
                + "&nbsp;&nbsp;<b>•</b> Skip that and apply the <b>Licence Patch</b> in the Patch step instead.<br><br>"
                + "Either way the path is ready — you can continue whenever you like.</html>",
                "Echo VR not installed yet", JOptionPane.INFORMATION_MESSAGE);
        } else {
            tipBox.showTip("Found your Meta Echo VR install!");
        }
    }

    private void buildStep3(int cx) {
        JLabel h = makeHeader("Download Echo VR client files");
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        this.pathIndicator = new JLabel();
        this.pathIndicator.setBounds((cx - 440) / 2 + 445, 64, 90, 34);
        this.pathIndicator.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(this.pathIndicator);

        pathField = makeEditablePathField(cx, 70, false, this.pathIndicator);
        contentPanel.add(pathField);

        dlButton = new SpecialButton("Start Download", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        dlButton.setLocation((cx - dlButton.getWidth()) / 2, 102);
        dlButton.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (stepInProgress) {
                    if (!confirmAbortDownload()) return;
                    if (downloader != null) downloader.cancelDownload();
                    stepInProgress = false;
                    progressAnimator.stop();
                    dlButton.changeText("Start Download");
                    dlProgressLabel.setText("Ready to download");
                    nextBtn.setEnabled(true);
                } else {
                    String installPath = commitPathField(pathField, false, pathIndicator);
                    if (installPath != null && !installPath.isEmpty()) {
                        String exePath = installPath + "/ready-at-dawn-echo-arena/bin/win10/echovr.exe";
                        if (new File(exePath).exists()) {
                            int choice = JOptionPane.showConfirmDialog(FrameGuidancePC.this,
                                "Echo VR is already installed at this path.\n\nOverwrite the existing installation?",
                                "Existing Installation Found", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                            if (choice != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }
                    }
                    dlButton.changeText("Cancel Download");
                    triggerDownload();
                }
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Download Echo VR client files"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(dlButton);
    }

    private void buildStep4(int cx, int sub) {
        if (sub == 1) {
            showPatchDetail(2);
            return;
        }
        patchDetailMode = 0;
        // Treat "no type chosen yet" the same as OWNER so the patch menu always renders the
        // full, consistent layout (e.g. when reached via chip click before selecting a type).
        if (wizardState.getUserType() == WizardState.UserType.OWNER
            || wizardState.getUserType() == null) {
            JLabel h = makeHeader("Optional patches");
            h.setBounds((cx - 450) / 2, 8, 450, 55); contentPanel.add(h);
            SpecialButton nl = new SpecialButton("No Licence Patch", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
            nl.setLocation((cx - nl.getWidth()) / 2, 73);             nl.addMouseListener(new MouseAdapter() { public void mouseReleased(MouseEvent e) { contentPanel.removeAll(); showLicencePatchInline(cx, false); contentPanel.revalidate(); contentPanel.repaint(); } public void mouseEntered(MouseEvent e) { tipBox.showTip("Patch Echo VR to skip licence check"); } public void mouseExited(MouseEvent e) { tipBox.showDefault(); } });
            contentPanel.add(nl);
            SpecialButton st = new SpecialButton("Steam Patch (Revive)", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
            st.setLocation((cx - st.getWidth()) / 2, 121);             st.addMouseListener(new MouseAdapter() { public void mouseReleased(MouseEvent e) { showPatchDetail(2); } public void mouseEntered(MouseEvent e) { tipBox.showTip("Patch Echo VR for Steam/Revive compatibility"); } public void mouseExited(MouseEvent e) { tipBox.showDefault(); } });
            contentPanel.add(st);
            nextBtn.setEnabled(true);
        } else {
            if (justArrivedAtStep4) {
                showLicencePatchInline(cx, true);
                // Re-sync the sidebar (substep names depend on userType) — via updateSidebar so the
                // labels keep their HTML multi-line wrapping instead of being overwritten with plain text.
                if (currentStep == 4) updateSidebar();
            } else {
                JLabel h = makeHeader("Patch Menu");
                h.setBounds((cx - 450) / 2, 8, 450, 55); contentPanel.add(h);
                SpecialButton authBtn = new SpecialButton("Licence Patch", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
                authBtn.setLocation((cx - authBtn.getWidth()) / 2, 73);
                authBtn.addMouseListener(new MouseAdapter() {
                    public void mouseReleased(MouseEvent e) {
                        contentPanel.removeAll();
                        showLicencePatchInline(cx, true);
                        contentPanel.revalidate();
                        contentPanel.repaint();
                    }
                    public void mouseEntered(MouseEvent e) { tipBox.showTip("Get your licence patch"); }
                    public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
                });
                contentPanel.add(authBtn);
                // Always show the Steam Patch option in the overview, regardless of playstyle —
                // the back button from any patch substep must return to the full patch menu.
                SpecialButton st = new SpecialButton("Steam Patch (Revive)", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
                st.setLocation((cx - st.getWidth()) / 2, 121);
                st.addMouseListener(new MouseAdapter() {
                    public void mouseReleased(MouseEvent e) { showPatchDetail(2); }
                    public void mouseEntered(MouseEvent e) { tipBox.showTip("Patch Echo VR for Steam/Revive compatibility"); }
                    public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
                });
                contentPanel.add(st);
                nextBtn.setEnabled(true);
            }
        }
    }

    private void buildStep5(int cx) {
        JLabel d = new JLabel("You're all set!", SwingConstants.CENTER);
        d.setBounds(0, 20, cx, 40); d.setForeground(new Color(0, 255, 0)); d.setFont(new Font("Arial", Font.BOLD, 24)); contentPanel.add(d);
        JLabel s = new JLabel("Echo VR is ready to play.", SwingConstants.CENTER);
        s.setBounds(0, 70, cx, 24); s.setForeground(Color.WHITE); s.setFont(new Font("Arial", Font.PLAIN, 16)); contentPanel.add(s);

        SpecialButton shortcutBtn = new SpecialButton("Add Desktop Shortcut",
            "button_up.png", "button_down.png", "button_highlighted.png", 18);
        shortcutBtn.setLocation((cx - shortcutBtn.getWidth()) / 2, 115);
        shortcutBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                String installPath = wizardState.getInstallPath();
                if (installPath == null || installPath.isEmpty()) {
                    new ErrorDialog().errorDialog(FrameGuidancePC.this, "No Install Path",
                        "Echo VR is not installed. Please download and install Echo VR first.", 0);
                    return;
                }
                Helpers.createDesktopShortcut(wizardState.getExePath());
                JOptionPane.showMessageDialog(FrameGuidancePC.this,
                    "Desktop shortcut created!", "Done", JOptionPane.INFORMATION_MESSAGE);
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Create a shortcut to Echo VR on your desktop"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(shortcutBtn);

        SpecialButton openBtn = new SpecialButton("Open Install Folder",
            "button_up.png", "button_down.png", "button_highlighted.png", 18);
        openBtn.setLocation((cx - openBtn.getWidth()) / 2, 180);
        openBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                String installPath = wizardState.getInstallPath();
                if (installPath == null || installPath.isEmpty()) {
                    new ErrorDialog().errorDialog(FrameGuidancePC.this, "No Install Path",
                        "Echo VR is not installed. Please download and install Echo VR first.", 0);
                    return;
                }
                Helpers.openFolder(wizardState.getBinPath());
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Open the Echo VR install folder in file explorer"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(openBtn);

        nextBtn.setEnabled(true);
    }

    private void buildStep4AfterOAuth(int cx) {
        JLabel h = makeHeader("Optional patches");
        h.setBounds((cx - 450) / 2, 8, 450, 55); contentPanel.add(h);

        SpecialButton nl = new SpecialButton("No Licence Patch", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        nl.setLocation((cx - nl.getWidth()) / 2, 73);
        nl.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { contentPanel.removeAll(); showLicencePatchInline(cx, false); contentPanel.revalidate(); contentPanel.repaint(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Patch Echo VR to skip licence check"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(nl);

        SpecialButton st = new SpecialButton("Steam Patch (Revive)", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        st.setLocation((cx - st.getWidth()) / 2, 121);
        st.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { showPatchDetail(2); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Patch Echo VR for Steam/Revive compatibility"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(st);

        nextBtn.setEnabled(true);
    }

    private void updatePathStatus(JLabel indicator, String path, JLabel pathLabel) {
        String exePath = path + "/ready-at-dawn-echo-arena/bin/win10/echovr.exe";
        boolean valid = new File(exePath).exists();
        // Use a vector-drawn mark instead of a unicode glyph: Windows Arial has no U+2713/U+2717,
        // so a text glyph renders as an empty "tofu" box.
        indicator.setText("");
        if (valid) {
            indicator.setIcon(markIcon(true, new Color(80, 255, 0), 26));
            indicator.setToolTipText("Echo VR found at this path");
            if (pathLabel != null) {
                pathLabel.setBackground(new Color(200, 255, 200, 200));
                pathLabel.repaint();
            }
        } else {
            indicator.setIcon(markIcon(false, new Color(255, 80, 80), 22));
            indicator.setToolTipText("Echo VR not found at this path");
        }
    }


    // === Patch Detail Views (master-detail pattern) ===

    private void showPatchDetail(int mode) {
        patchDetailMode = mode;
        contentPanel.removeAll();
        int cx = contentPanel.getWidth();
        // Back button
        SpecialButton backBtn = new SpecialButton("← Back", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        backBtn.setLocation(10, 5);
        backBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { showStep(4, 0); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Return to patch selection"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(backBtn);

        if (mode == 2) {
            buildSteamPatchDetail(cx);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showLicencePatchInline(int cx, boolean redirectAfterDownload) {
        SpecialButton backBtn = new SpecialButton("← Back", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        backBtn.setLocation(10, 5);
        backBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { showStep(4, 0); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Return to patch selection"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(backBtn);

        // Header at y=4/h=42, consistent with the sibling Steam-patch detail (Back button shows
        // through the header image's transparent edge). Even 6–8px gaps down the column.
        JLabel h = makeHeader("No Licence Patch");
        h.setBounds((cx - 450) / 2, 4, 450, 42); contentPanel.add(h);

        // Path: 440-wide editable field (same as the Path stage), centred, with the ✓/✗ indicator to
        // its right and the "Choose path" button stacked below it — all on the window centre axis.
        JLabel pathStatusLabel = new JLabel();
        pathStatusLabel.setBounds((cx - 440) / 2 + 445, 52, 40, 28);
        pathStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(pathStatusLabel);

        // Patch path shows the full path up to ready-at-dawn-echo-arena (and is editable/pastable).
        SpecialTextfield patchPathField = makeEditablePathField(cx, 54, true, pathStatusLabel);
        contentPanel.add(patchPathField);

        SpecialButton choosePathBtn = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        choosePathBtn.setLocation((cx - choosePathBtn.getWidth()) / 2, 86);
        choosePathBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                String chosen = Helpers.chooseFolder(FrameGuidancePC.this);
                if (chosen == null) return;
                patchPathField.setText(chosen);
                commitPathField(patchPathField, true, pathStatusLabel);
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Choose your Echo VR install folder — the game folder or a subfolder works too"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(choosePathBtn);

        // Patch-options panel: "Authorize with Discord" + "— or —" + custom-URL row, in one box.
        SpecialButton oauthBtn = new SpecialButton("Authorize with Discord", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        SpecialTextfield urlField = buildPatchOptionsPanel(cx, 119, oauthBtn, "Start Patching",
            "Paste patch URL here…",
            "Paste a direct URL to use a custom patch instead of generating one.",
            t -> validateDllUrl(t) != null,
            () -> licenceTempReady = false);   // mode switch → don't reuse the staged download
        oauthBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                startLicencePatch(oauthBtn, cx, redirectAfterDownload, patchPathField, pathStatusLabel, urlField);
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip(urlField.isEnabled()
                ? "Download and apply the patch from your pasted URL"
                : "Generate your personalized licence patch"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
    }

    /**
     * Drives the licence patch: a valid custom URL (pasted into the patch-options panel) bypasses
     * OAuth; an already-downloaded temp file is reused on Retry without re-authorizing; otherwise the
     * Discord OAuth flow runs. Any previous in-flight OAuth attempt is cancelled first to free the port.
     */
    private void startLicencePatch(SpecialButton btn, int cx, boolean redirectAfter,
                                   SpecialTextfield patchField, JLabel indicator,
                                   SpecialTextfield urlField) {
        // Apply any path edit before validating/copying.
        commitPathField(patchField, true, indicator);

        // Free the previous attempt's resources so Retry can re-bind the callback port.
        if (activeLicenceFlow != null) { activeLicenceFlow.cancel(); activeLicenceFlow = null; }
        if (downloadPatch != null) { downloadPatch.cancelDownload(); }

        btn.setEnabled(false);
        nextBtn.setEnabled(false);
        stepInProgress = true;
        progressAnimator.start();

        // The custom-URL path is taken only when its field is enabled (checkbox ticked).
        boolean advancedOn = urlField.isEnabled();
        String advUrl = advancedOn ? validateDllUrl(urlField.getText().trim()) : null;
        if (advancedOn && advUrl == null) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Wrong URL provided",
                "Your provided Download Link is wrong. Please check!", 0);
            markLicenceRetry(btn);
            resetAfterError(btn);
            return;
        }

        btn.changeText("Please Wait...");   // work is starting (OAuth / download / install)

        Path tempDll = Helpers.PATCH_TEMP_DIR.resolve("pnsovr.dll");

        // Advanced: download straight from the pasted URL.
        if (advUrl != null) {
            downloadDllToTempThenInstall(advUrl, btn, cx, redirectAfter);
            return;
        }

        // Reuse a patch downloaded earlier in THIS session on Retry (skips OAuth + re-download).
        // Gated on licenceTempReady so a stale temp file from a previous run can't short-circuit OAuth.
        if (licenceTempReady && java.nio.file.Files.exists(tempDll)) {
            dlProgressLabel.setText("Using downloaded patch...");
            installLicenceFromTemp(btn, cx, redirectAfter);
            return;
        }

        new Thread(() -> {
            try {
                DiscordOAuth2Flow flow = new DiscordOAuth2Flow("dll");
                activeLicenceFlow = flow;
                String patchUrl = flow.start(status -> SwingUtilities.invokeLater(() -> dlProgressLabel.setText(status)))
                        .get(120, TimeUnit.SECONDS);
                activeLicenceFlow = null;
                System.out.println("OAuth2 SUCCESS: URL=" + patchUrl);
                SwingUtilities.invokeLater(() -> downloadDllToTempThenInstall(patchUrl, btn, cx, redirectAfter));
            } catch (java.util.concurrent.ExecutionException ex) {
                activeLicenceFlow = null;
                SwingUtilities.invokeLater(() -> {
                    OAuth2ErrorHandler.handleError(ex.getCause(), FrameGuidancePC.this, btn);
                    markLicenceRetry(btn);
                    resetAfterError(btn);
                });
            } catch (Exception ex) {
                activeLicenceFlow = null;
                SwingUtilities.invokeLater(() -> {
                    OAuth2ErrorHandler.handleError(ex, FrameGuidancePC.this, btn);
                    markLicenceRetry(btn);
                    resetAfterError(btn);
                });
            }
        }).start();
    }

    /** Downloads the patch dll into the temp staging dir, then copies it into the install folder. */
    private void downloadDllToTempThenInstall(String url, SpecialButton btn, int cx, boolean redirectAfter) {
        dlProgressLabel.setText("Downloading patch file...");
        if (downloadPatch != null) { downloadPatch.cancelDownload(); pause(1); }
        downloadPatch = new Downloader();
        downloadPatch.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
            licenceTempReady = true;     // a real download finished this session → safe to reuse on Retry
            installLicenceFromTemp(btn, cx, redirectAfter);
        }));
        downloadPatch.startDownload(url, Helpers.PATCH_TEMP_DIR.toString(), "pnsovr.dll",
            new SpecialLabel(" 0%", 13), FrameGuidancePC.this, null, 3, true, -1, false);
    }

    /** Copies the staged pnsovr.dll into the install folder. On any failure the temp file is KEPT so a Retry reuses it. */
    private void installLicenceFromTemp(SpecialButton btn, int cx, boolean redirectAfter) {
        Path tempDll = Helpers.PATCH_TEMP_DIR.resolve("pnsovr.dll");
        if (!java.nio.file.Files.exists(tempDll)) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Download missing",
                "The patch file wasn't downloaded. Please try again.", 0);
            markLicenceRetry(btn); resetAfterError(btn); return;
        }
        String root = wizardState.getInstallPath();
        String ep = root + "/ready-at-dawn-echo-arena/bin/win10";
        if (!new File(ep).exists()) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Wrong path",
                "Couldn't find ready-at-dawn-echo-arena\\bin\\win10 at your path.\n"
                + "Fix the path above and hit Retry — your downloaded patch will be reused.", 0);
            markLicenceRetry(btn); resetAfterError(btn); return;
        }
        try {
            Helpers.copyInto(tempDll, ep, "pnsovr.dll");
        } catch (Exception ioe) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Couldn't write patch",
                "Couldn't copy the patch into your install folder.\n" + ioe.getMessage()
                + "\n\nTry running the installer as administrator, then hit Retry.", 0);
            markLicenceRetry(btn); resetAfterError(btn); return;
        }
        stepInProgress = false;
        stepCompleted = true;
        progressAnimator.stop();
        nextBtn.setEnabled(true);
        btn.setEnabled(true);
        btn.changeText("Done");          // success → "Done" (only here); failures use "Retry"
        dlProgressLabel.setText("License patch applied!");
        progPanel.repaint();
        statusBarBox.repaint();
        if (redirectAfter) {
            if (wizardState.getPlayStyle() == PCWizardState.PlayStyle.STEAMVR) {
                // New player + SteamVR: chain straight into the Steam patch (substep 1).
                showStep(4, 1);
            } else {
                contentPanel.removeAll();
                buildStep4AfterOAuth(cx);
                contentPanel.revalidate();
                contentPanel.repaint();
                getContentPane().revalidate();
                getContentPane().repaint();
            }
        }
    }

    private void markLicenceRetry(SpecialButton btn) {
        if (btn != null) btn.changeText("Retry");
    }

    /** Accepts the legacy Discord-CDN pnsovr.dll link or a files.echovr.de URL; null if neither. */
    private String validateDllUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.matches("https://cdn\\.discordapp\\.com/attachments/.*/pnsovr\\.dll.*")) return url;
        if (url.matches("https://files\\.echovr\\.de/.*")) return url;
        return null;
    }

    // === Steam Patch (Revive) — checkbox-driven, chained setup ===
    // Patch rows, in execution order.
    private static final int ROW_REVIVE = 0;
    private static final int ROW_SHORTCUT = 1;
    private static final int ROW_DASHBOARD = 2;
    private static final int ROW_ARTWORK = 3;
    private static final String[] PATCH_LABELS = {
        "Install Revive", "Revive injector shortcut",
        "Restore Dashboard entry", "Fix game artwork"
    };
    private static final String[] PATCH_TIPS = {
        "Download and run the latest Revive installer",
        "Add a desktop shortcut that launches Echo through Revive (fixes 'can't press buttons in-game')",
        "Only for players who had Echo on PC before shutdown — restores the Revive Dashboard entry",
        "Download the correct Echo artwork into your Meta Horizon store assets"
    };
    // Default check state (per design decision): Dashboard off, everything else on.
    private static final boolean[] PATCH_DEFAULTS = {true, true, false, true};

    // Row status glyphs.
    private static final int ST_PENDING = 0, ST_WORKING = 1, ST_DONE = 2, ST_FAIL = 3;

    private void buildSteamPatchDetail(int cx) {
        JLabel h = makeHeader("Steam Patch (Revive)");
        h.setBounds((cx - 450) / 2, 4, 450, 42); contentPanel.add(h);
        // Note: showPatchDetail() already added the "← Back" button.

        // Logical section: wrap the patch items in their own rounded section box
        // (semi-transparent fill + border), as everywhere else. The box is visual only
        // (contains()->false), so the interactive rows/button sit on top as siblings.
        // One uniform inner padding (PAD) on all four sides; rows fill the inner width,
        // the button is centered, and the box ends one PAD below the button.
        // Kept compact so the box + the (shown) progress label fit the 245px content height.
        final int PAD = 12;
        final int rowPitch = 20;
        final int rowH = 20;
        final int glyphW = 22;
        final int gap = 6;           // between the rows block and the button
        final int n = PATCH_LABELS.length;

        // Create the action button first so the box can be sized to enclose it.
        SpecialButton startBtn = new SpecialButton("Install & Configure", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        SpecialLabel progressLbl = new SpecialLabel(" ", 13);
        progressLbl.setHorizontalAlignment(SwingConstants.CENTER);

        int rowsHeight = (n - 1) * rowPitch + rowH;
        int btnH = startBtn.getHeight();
        int btnW = startBtn.getWidth();

        int boxW = Math.min(cx - 20, Math.max(btnW, 300) + 2 * PAD);
        int boxX = (cx - boxW) / 2;
        int boxY = 48;
        int boxH = PAD + rowsHeight + gap + btnH + PAD;
        JPanel itemsBox = sectionBoxAt(boxX, boxY, boxH, 15, boxW, new Color(200, 0, 150, 90));
        contentPanel.add(itemsBox);

        int firstRowY = boxY + PAD;
        int cbX = boxX + PAD;
        int glyphX = boxX + boxW - PAD - glyphW;
        int cbW = glyphX - cbX - 6;
        steamPatchBoxes = new SpecialCheckBox[n];
        steamPatchStatus = new JLabel[n];
        for (int i = 0; i < n; i++) {
            final int idx = i;
            int rowY = firstRowY + i * rowPitch;
            SpecialCheckBox cb = new SpecialCheckBox(PATCH_LABELS[i], 14);
            cb.setSelected(PATCH_DEFAULTS[i]);
            cb.setBounds(cbX, rowY, cbW, rowH);
            cb.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { tipBox.showTip(PATCH_TIPS[idx]); }
                public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
            });
            contentPanel.add(cb);
            steamPatchBoxes[i] = cb;

            JLabel st = new JLabel("○"); // ○ pending
            st.setFont(new Font("Arial", Font.BOLD, 14));
            st.setForeground(Color.LIGHT_GRAY);
            st.setHorizontalAlignment(SwingConstants.RIGHT); // hug the inner edge so right padding == PAD
            st.setBounds(glyphX, rowY, glyphW, rowH);
            contentPanel.add(st);
            steamPatchStatus[i] = st;
        }

        int btnY = firstRowY + rowsHeight + gap;
        startBtn.setLocation(boxX + (boxW - btnW) / 2, btnY);
        startBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { runSteamPatchChain(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Run the selected Revive patches"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(startBtn);
        this.steamPatchStartBtn = startBtn;

        // Progress/% readout — sits just *below* the box and stays hidden until the chain
        // runs, so the box keeps uniform padding and shows no stray empty bar at rest.
        progressLbl.setBounds(boxX, boxY + boxH + 4, boxW, 18);
        progressLbl.setVisible(false);
        contentPanel.add(progressLbl);
        this.steamPatchProgressLbl = progressLbl;

        // Keep the section box behind the interactive rows/button.
        contentPanel.setComponentZOrder(itemsBox, contentPanel.getComponentCount() - 1);
    }

    /** Validates prerequisites then runs the selected patches sequentially on a worker thread. */
    private void runSteamPatchChain() {
        if (!Helpers.isWindows) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Windows only",
                "Revive is a Windows-only SteamVR shim. These patches can only be applied on Windows.", 0);
            return;
        }
        boolean anySelected = false;
        for (SpecialCheckBox cb : steamPatchBoxes) anySelected |= cb.isSelected();
        if (!anySelected) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Nothing selected",
                "Select at least one patch to apply.", 0);
            return;
        }
        // No upfront admin gate: each step runs in-process and, only if it fails for lack of
        // rights, the AdminBroker asks for consent and elevates (reusing one helper for the rest).
        if (!new File(wizardState.getExePath()).exists()) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Echo VR not found",
                "echovr.exe was not found at your install path. Download Echo VR first, then apply patches.", 0);
            return;
        }

        // Snapshot selections, lock the UI.
        boolean doRevive = steamPatchBoxes[ROW_REVIVE].isSelected();
        boolean doShortcut = steamPatchBoxes[ROW_SHORTCUT].isSelected();
        boolean doDashboard = steamPatchBoxes[ROW_DASHBOARD].isSelected();
        boolean doArtwork = steamPatchBoxes[ROW_ARTWORK].isSelected();

        for (SpecialCheckBox cb : steamPatchBoxes) cb.setEnabled(false);
        steamPatchStartBtn.setEnabled(false);
        steamPatchProgressLbl.setVisible(true);
        nextBtn.setEnabled(false);
        stepInProgress = true;
        progressAnimator.start();

        new Thread(() -> {
            try {
                String exe = new File(wizardState.getExePath()).getAbsolutePath();
                System.out.println("[SteamPatch] === chain start === exe=" + exe
                    + " | selected: Revive=" + doRevive + " Shortcut=" + doShortcut
                    + " Dashboard=" + doDashboard + " Artwork=" + doArtwork);

                if (doRevive) {
                    System.out.println("[SteamPatch] step: Install Revive");
                    markRow(ROW_REVIVE, ST_WORKING);
                    setStatus("Installing Revive...");
                    boolean ok = installReviveBlocking();
                    // The installer's exit code is unreliable (e.g. user cancel); the authoritative
                    // check is that ReviveInjector.exe actually exists. Give the files a moment to settle.
                    String dir = ok ? waitForReviveDir(8000) : null;
                    if (dir == null) {
                        markRow(ROW_REVIVE, ST_FAIL);
                        failChain("Revive does not appear to be installed (was the installer cancelled?). "
                            + "Re-run, or install Revive manually, then try again.");
                        return;
                    }
                    markRow(ROW_REVIVE, ST_DONE);
                }

                String reviveDir = ReviveSetup.findReviveDir();
                System.out.println("[SteamPatch] resolved Revive dir = " + reviveDir);
                if (doShortcut && reviveDir == null) {
                    failChain("Revive is not installed. Tick 'Install Revive' and run again, or install Revive manually first.");
                    return;
                }

                if (doShortcut) {
                    System.out.println("[SteamPatch] step: Revive injector shortcut");
                    markRow(ROW_SHORTCUT, ST_WORKING);
                    setStatus("Creating Revive shortcut...");
                    AdminBroker.createInjectorShortcut(FrameGuidancePC.this, reviveDir, exe);
                    markRow(ROW_SHORTCUT, ST_DONE);
                }

                if (doArtwork) {
                    System.out.println("[SteamPatch] step: Fix game artwork");
                    markRow(ROW_ARTWORK, ST_WORKING);
                    setStatus("Installing game artwork...");
                    AdminBroker.installArtwork(FrameGuidancePC.this);
                    markRow(ROW_ARTWORK, ST_DONE);
                }

                if (doDashboard) {
                    System.out.println("[SteamPatch] step: Restore Dashboard entry");
                    markRow(ROW_DASHBOARD, ST_WORKING);
                    setStatus("Restoring dashboard entry...");
                    try {
                        ReviveSetup.restoreDashboardManifests();
                        markRow(ROW_DASHBOARD, ST_DONE);
                    } catch (UnsupportedOperationException ue) {
                        markRow(ROW_DASHBOARD, ST_FAIL);
                        showInfo("Dashboard restore unavailable", ue.getMessage());
                    }
                }

                System.out.println("[SteamPatch] === chain complete ===");
                finishChain();
            } catch (Exception ex) {
                System.err.println("[SteamPatch] chain failed: " + ex);
                ex.printStackTrace();
                failChain("Steam patch failed: " + ex.getMessage());
            }
        }).start();
    }

    /** Polls {@link ReviveSetup#findReviveDir()} until Revive is verified present or the timeout elapses. */
    private String waitForReviveDir(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String dir;
        while ((dir = ReviveSetup.findReviveDir()) == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(500);
        }
        return dir;
    }

    /** Downloads ReviveInstaller.exe and runs it (silently, elevated), blocking until done. */
    private boolean installReviveBlocking() {
        String dir = System.getProperty("java.io.tmpdir") + "/revive";
        String installerPath = dir + "/ReviveInstaller.exe";
        try {
            System.out.println("[SteamPatch] installRevive: downloading ReviveInstaller.exe -> " + installerPath
                + (new File(installerPath).exists() ? " (existing file will be overwritten)" : ""));
            CountDownLatch latch = new CountDownLatch(1);
            Downloader d = new Downloader();
            d.setOnCompleteListener(latch::countDown);
            d.startDownload(
                "https://github.com/LibreVR/Revive/releases/download/3.1.1/ReviveInstaller.exe",
                dir, "/ReviveInstaller.exe", steamPatchProgressLbl, FrameGuidancePC.this,
                null, 1, true, -1, false);
            latch.await(10, TimeUnit.MINUTES);
            if (!new File(installerPath).exists()) {
                System.err.println("[SteamPatch] installRevive: installer not found after download");
                return false;
            }
            System.out.println("[SteamPatch] installRevive: downloaded " + new File(installerPath).length()
                + " bytes; launching installer (needs elevation — UAC prompt may appear)");

            // ReviveInstaller.exe requires elevation; ProcessBuilder can't trigger UAC (fails with
            // error=740). AdminBroker runs it in-process and, on that failure, via the elevated helper.
            // "/S" = NSIS silent install (default location), so completion is deterministic.
            int code = AdminBroker.runInstaller(FrameGuidancePC.this, installerPath, "/S");
            System.out.println("[SteamPatch] installRevive: installer exited with code " + code);
            return true;
        } catch (Exception e) {
            System.err.println("[SteamPatch] installRevive: failed: " + e);
            e.printStackTrace();
            return false;
        }
    }

    private void markRow(int row, int state) {
        SwingUtilities.invokeLater(() -> {
            JLabel lbl = steamPatchStatus[row];
            // ○ (pending) and ● (working) are text glyphs Arial renders fine; ✓/✗ are drawn as
            // icons because Arial lacks those glyphs (would show as empty boxes).
            switch (state) {
                case ST_WORKING -> { lbl.setIcon(null); lbl.setText("●"); lbl.setForeground(new Color(0, 180, 0)); }
                case ST_DONE -> { lbl.setText(""); lbl.setIcon(markIcon(true, new Color(0, 255, 0), 16)); }
                case ST_FAIL -> { lbl.setText(""); lbl.setIcon(markIcon(false, new Color(255, 80, 80), 16)); }
                default -> { lbl.setIcon(null); lbl.setText("○"); lbl.setForeground(Color.LIGHT_GRAY); }
            }
        });
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            dlProgressLabel.setText(text);
            steamPatchProgressLbl.setText(text);
            progPanel.repaint();
            statusBarBox.repaint();
        });
    }

    private void showInfo(String title, String message) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(FrameGuidancePC.this, message, title, JOptionPane.INFORMATION_MESSAGE));
    }

    private void finishChain() {
        SwingUtilities.invokeLater(() -> {
            stepInProgress = false;
            stepCompleted = true;
            progressAnimator.stop();
            nextBtn.setEnabled(true);
            steamPatchStartBtn.setEnabled(true);
            steamPatchProgressLbl.setText("Revive setup complete!");
            dlProgressLabel.setText("Revive setup complete!");
            progPanel.repaint();
            statusBarBox.repaint();
        });
    }

    private void failChain(String message) {
        SwingUtilities.invokeLater(() -> {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Steam Patch Failed", message, 0);
            for (SpecialCheckBox cb : steamPatchBoxes) cb.setEnabled(true);
            resetAfterError(steamPatchStartBtn);
        });
    }

}
