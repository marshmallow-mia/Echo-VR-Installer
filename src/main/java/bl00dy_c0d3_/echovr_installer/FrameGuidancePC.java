package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameGuidancePC extends BaseWizard {

    private PCWizardState wizardState = new PCWizardState();
    private SpecialLabel pathLbl;
    private SpecialButton steamPatchStartBtn;
    private SpecialLabel steamPatchProgressLbl;
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
    protected String getWindowTitle() { return "Echo VR Installer"; }

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
                if (wizardState.getUserType() == WizardState.UserType.OWNER) {
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
        wizardState.setInstallPath(pathLbl.getText());
        stepInProgress = true;
        stepCompleted = false;
        progressAnimator.start();
        if (dlButton != null) dlButton.changeText("Cancel Download");
        if (downloader != null) { downloader.cancelDownload(); pause(1); }
        new Thread(() -> {
            downloader = new Downloader();
            downloader.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> { dlProgressLabel.setText("Complete"); nextBtn.setEnabled(true); stepInProgress = false; stepCompleted = true; progressAnimator.stop(); progPanel.repaint(); statusBarBox.repaint(); if (dlButton != null) dlButton.changeText("Start Download"); }));
            downloader.startDownload("ready-at-dawn-echo-arena.zip", pathLbl.getText(), "ready-at-dawn-echo-arena.zip", dlProgressLabel, FrameGuidancePC.this, frameMain, 0, false, 0, false);
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

    private void buildStep2(int cx) {
        JLabel h = makeHeader("Choose your Echo VR install path");
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        String savedPath = wizardState.getInstallPath();
        if (savedPath == null || savedPath.isEmpty()) savedPath = System.getProperty("os.name").toLowerCase().contains("win") ? "C:/EchoVR" : System.getProperty("user.dir") + File.separator + "echovr";
        wizardState.setInstallPath(savedPath);
        pathLbl = makeRoundedLabel(savedPath, 12);
        pathLbl.setLocation((cx - 440) / 2, 70); pathLbl.setSize(440, 22);
        pathLbl.setBackground(new Color(255, 255, 255, 200)); pathLbl.setForeground(Color.BLACK);
        contentPanel.add(pathLbl);

        SpecialButton pick = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        pick.setLocation((cx - pick.getWidth()) / 2, 102);
        pick.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                pathFolderChooser(pathLbl, FrameGuidancePC.this);
                wizardState.setInstallPath(pathLbl.getText());
                showStep(3, 0);
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Manually choose install folder"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(pick);
    }

    private void buildStep3(int cx) {
        JLabel h = makeHeader("Download Echo VR client files");
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        String savedPath = wizardState.getInstallPath();
        if (savedPath == null || savedPath.isEmpty()) savedPath = System.getProperty("os.name").toLowerCase().contains("win") ? "C:/EchoVR" : System.getProperty("user.dir") + File.separator + "echovr";
        wizardState.setInstallPath(savedPath);
        pathLbl = makeRoundedLabel(savedPath, 12);
        pathLbl.setLocation((cx - 440) / 2, 70); pathLbl.setSize(440, 22);
        pathLbl.setBackground(new Color(255, 255, 255, 200)); pathLbl.setForeground(Color.BLACK);
        contentPanel.add(pathLbl);

        this.pathIndicator = new JLabel();
        this.pathIndicator.setBounds((cx - 440) / 2 + 445, 64, 90, 34);
        this.pathIndicator.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(this.pathIndicator);
        updatePathStatus(this.pathIndicator, savedPath, pathLbl);

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
                    String installPath = wizardState.getInstallPath();
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
                    wizardState.setInstallPath(pathLbl.getText());
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
        if (wizardState.getUserType() == WizardState.UserType.OWNER) {
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
                if (currentStep == 4) {
                    for (int i = 0; i < 3; i++) {
                        if (sidebarSubLabels[i] != null) {
                            sidebarSubLabels[i].setText(getSubstepName(4, i));
                        }
                    }
                }
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
                if (wizardState.getPlayStyle() == PCWizardState.PlayStyle.STEAMVR) {
                    SpecialButton st = new SpecialButton("Steam Patch (Revive)", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
                    st.setLocation((cx - st.getWidth()) / 2, 121);
                    st.addMouseListener(new MouseAdapter() {
                        public void mouseReleased(MouseEvent e) { showPatchDetail(2); }
                        public void mouseEntered(MouseEvent e) { tipBox.showTip("Patch Echo VR for Steam/Revive compatibility"); }
                        public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
                    });
                    contentPanel.add(st);
                }
                nextBtn.setEnabled(true);
            }
        }
    }

    private void buildStep5(int cx) {
        JLabel d = new JLabel("You're all set!", SwingConstants.CENTER);
        d.setBounds(0, 55, cx, 40); d.setForeground(new Color(0, 255, 0)); d.setFont(new Font("Arial", Font.BOLD, 24)); contentPanel.add(d);
        JLabel s = new JLabel("Echo VR is ready to play.", SwingConstants.CENTER);
        s.setBounds(0, 105, cx, 24); s.setForeground(Color.WHITE); s.setFont(new Font("Arial", Font.PLAIN, 16)); contentPanel.add(s);
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
        if (valid) {
            indicator.setText("\u2713");
            indicator.setForeground(new Color(80, 255, 0));
            indicator.setFont(new Font("Arial", Font.BOLD, 28));
            indicator.setToolTipText("Echo VR found at this path");
            if (pathLabel != null) {
                pathLabel.setBackground(new Color(200, 255, 200, 200));
                pathLabel.repaint();
            }
        } else {
            indicator.setText("\u2717");
            indicator.setForeground(new Color(255, 80, 80));
            indicator.setFont(new Font("Arial", Font.BOLD, 18));
            indicator.setToolTipText("Echo VR not found at this path");
        }
    }


    private void startLicenceOAuth2(SpecialButton triggerBtn) {
        triggerBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        stepInProgress = true;
        progressAnimator.start();

        new Thread(() -> {
            try {
                DiscordOAuth2Flow flow = new DiscordOAuth2Flow("dll");
                String patchUrl = flow.start(status -> dlProgressLabel.setText(status)).get(300, TimeUnit.SECONDS);

                String finalPatchUrl = patchUrl;
                System.out.println("OAuth2 SUCCESS: URL=" + finalPatchUrl);
                SwingUtilities.invokeLater(() -> {
                    dlProgressLabel.setText("Downloading patch file...");

                    String ep = wizardState.getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10";
                    if (!new File(ep).exists()) {
                        new ErrorDialog().errorDialog(FrameGuidancePC.this, "Wrong path", "Check your path", 0);
                        resetAfterError(triggerBtn);
                        return;
                    }
                    if (downloadPatch != null) { downloadPatch.cancelDownload(); pause(1); }
                    downloadPatch = new Downloader();
                    downloadPatch.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                        stepInProgress = false;
                        stepCompleted = true;
                        progressAnimator.stop();
                        nextBtn.setEnabled(true);
                        triggerBtn.setEnabled(true);
                        dlProgressLabel.setText("License patch applied!");
                        System.out.println("OAuth2: License patch download complete from " + finalPatchUrl);
                        progPanel.repaint();
                        statusBarBox.repaint();
                    }));
                    downloadPatch.startDownload(patchUrl, ep, "pnsovr.dll", new SpecialLabel(" 0%", 13), FrameGuidancePC.this, null, 3, true, -1, false);
                });
            } catch (java.util.concurrent.ExecutionException ex) {
                OAuth2ErrorHandler.handleError(ex.getCause(), FrameGuidancePC.this, triggerBtn);
                resetAfterError(triggerBtn);
            } catch (Exception ex) {
                OAuth2ErrorHandler.handleError(ex, FrameGuidancePC.this, triggerBtn);
                resetAfterError(triggerBtn);
            }
        }).start();
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
        JLabel h = makeHeader("No Licence Patch");
        h.setBounds((cx - 450) / 2, 35, 450, 55); contentPanel.add(h);
        SpecialButton backBtn = new SpecialButton("← Back", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        backBtn.setLocation(10, 5);
        backBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { showStep(4, 0); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Return to patch selection"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(backBtn);

        String savedPath = wizardState.getInstallPath();
        if (savedPath == null || savedPath.isEmpty()) savedPath = System.getProperty("os.name").toLowerCase().contains("win") ? "C:/EchoVR" : System.getProperty("user.dir") + File.separator + "echovr";
        wizardState.setInstallPath(savedPath);
        SpecialLabel patchPathLbl = makeRoundedLabel(savedPath, 12);
        patchPathLbl.setLocation((cx - 440) / 2, 100); patchPathLbl.setSize(440, 22);
        patchPathLbl.setBackground(new Color(255, 255, 255, 200)); patchPathLbl.setForeground(Color.BLACK);
        contentPanel.add(patchPathLbl);

        JLabel pathStatusLabel = new JLabel();
        pathStatusLabel.setBounds((cx - 440) / 2 + 445, 94, 90, 34);
        pathStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(pathStatusLabel);
        updatePathStatus(pathStatusLabel, savedPath, patchPathLbl);

        SpecialButton choosePathBtn = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        choosePathBtn.setLocation((cx - choosePathBtn.getWidth()) / 2, 127);
        choosePathBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                pathFolderChooser(patchPathLbl, FrameGuidancePC.this);
                String newPath = patchPathLbl.getText();
                wizardState.setInstallPath(newPath);
                updatePathStatus(pathStatusLabel, newPath, patchPathLbl);
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Choose your Echo VR install folder"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(choosePathBtn);

        SpecialButton oauthBtn = new SpecialButton("Authorize with Discord", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        oauthBtn.setLocation((cx - oauthBtn.getWidth()) / 2, 160);
        oauthBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                oauthBtn.setEnabled(false);
                nextBtn.setEnabled(false);
                stepInProgress = true;
                progressAnimator.start();

                new Thread(() -> {
                    try {
                        DiscordOAuth2Flow flow = new DiscordOAuth2Flow("dll");
                        String patchUrl = flow.start(status -> dlProgressLabel.setText(status)).get(300, TimeUnit.SECONDS);

                        String finalPatchUrl = patchUrl;
                        System.out.println("OAuth2 SUCCESS: URL=" + finalPatchUrl);
                        SwingUtilities.invokeLater(() -> {
                            dlProgressLabel.setText("Downloading patch file...");

                            String ep = wizardState.getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10";
                            if (!new File(ep).exists()) {
                                new ErrorDialog().errorDialog(FrameGuidancePC.this, "Wrong path", "Check your path", 0);
                                resetAfterError(oauthBtn);
                                return;
                            }
                            if (downloadPatch != null) { downloadPatch.cancelDownload(); pause(1); }
                            downloadPatch = new Downloader();
                            downloadPatch.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                                stepInProgress = false;
                                stepCompleted = true;
                                progressAnimator.stop();
                                nextBtn.setEnabled(true);
                                dlProgressLabel.setText("Patch applied successfully!");
                                System.out.println("OAuth2: Patch download complete from " + finalPatchUrl);
                                progPanel.repaint();
                                statusBarBox.repaint();
                                if (redirectAfterDownload) {
                                    contentPanel.removeAll();
                                    buildStep4AfterOAuth(cx);
                                    contentPanel.revalidate();
                                    contentPanel.repaint();
                                    getContentPane().revalidate();
                                    getContentPane().repaint();
                                }
                            }));
                            downloadPatch.startDownload(patchUrl, ep, "pnsovr.dll", new SpecialLabel(" 0%", 13), FrameGuidancePC.this, null, 3, true, -1, false);
                        });
                    } catch (java.util.concurrent.ExecutionException ex) {
                        OAuth2ErrorHandler.handleError(ex.getCause(), FrameGuidancePC.this, oauthBtn);
                        resetAfterError(oauthBtn);
                    } catch (Exception ex) {
                        OAuth2ErrorHandler.handleError(ex, FrameGuidancePC.this, oauthBtn);
                        resetAfterError(oauthBtn);
                    }
                }).start();
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Generate your personalized licence patch"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(oauthBtn);
    }

    private void buildSteamPatchDetail(int cx) {
        JLabel h = makeHeader("Steam Patch (Revive)");
        h.setBounds((cx - 450) / 2, 35, 450, 55); contentPanel.add(h);

        SpecialButton backBtn = new SpecialButton("← Back", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        backBtn.setLocation(10, 5);
        backBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { showStep(4, 0); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Return to patch selection"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(backBtn);

        JLabel info = new JLabel("<html><center>This will install Revive for SteamVR headsets<br>on non-Oculus headsets (Valve Index, HTC Vive, etc).<br><br>Click 'Start Install' to begin the download.</center></html>", SwingConstants.CENTER);
        info.setBounds(30, 95, cx - 60, 80);
        info.setForeground(Color.WHITE);
        info.setFont(new Font("Arial", Font.PLAIN, 13));
        contentPanel.add(info);

        SpecialButton startBtn = new SpecialButton("Start Install", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        startBtn.setLocation((cx - startBtn.getWidth()) / 2, 185);
        startBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { startSteamPatchDownload(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Begin installing Revive for SteamVR"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(startBtn);
        this.steamPatchStartBtn = startBtn;

        SpecialLabel progressLbl = new SpecialLabel(" 0%", 13);
        progressLbl.setLocation((cx - 100) / 2, 220);
        contentPanel.add(progressLbl);
        this.steamPatchProgressLbl = progressLbl;
    }

    private void startSteamPatchDownload() {
        if (!Helpers.checkForAdmin()) {
            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Please restart as Admin",
                "<html>To install Revive, you need to restart this app as admin. To do that,<br>close the Installer completely. Then right click on EchoVR_Installer.exe<br>and click on Start as Admin.</html>", -1);
            return;
        }

        steamPatchStartBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        stepInProgress = true;
        progressAnimator.start();

        new Thread(() -> {
            try {
                Downloader steamDownloader = new Downloader();
                steamDownloader.setOnCompleteListener(() -> {
                    SwingUtilities.invokeLater(() -> {
                        String installerPath = System.getProperty("java.io.tmpdir") + "/revive/ReviveInstaller.exe";
                        if (new File(installerPath).exists()) {
                            installReviveInline();
                        } else {
                            new ErrorDialog().errorDialog(FrameGuidancePC.this, "Download Failed",
                                "Could not download Revive installer. Please check your internet connection and try again.", 0);
                            resetAfterError(steamPatchStartBtn);
                        }
                    });
                });
                steamDownloader.startDownload(
                    "https://github.com/LibreVR/Revive/releases/latest/download/ReviveInstaller.exe",
                    System.getProperty("java.io.tmpdir") + "/revive",
                    "/ReviveInstaller.exe",
                    steamPatchProgressLbl,
                    FrameGuidancePC.this,
                    null,
                    1,
                    true,
                    -1,
                    false
                );
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    new ErrorDialog().errorDialog(FrameGuidancePC.this, "Error",
                        "Download failed: " + ex.getMessage(), 0);
                    resetAfterError(steamPatchStartBtn);
                });
            }
        }).start();
    }

    private void installReviveInline() {
        String installerPath = System.getProperty("java.io.tmpdir") + "/revive/ReviveInstaller.exe";

        SwingUtilities.invokeLater(() -> {
            steamPatchProgressLbl.setText("Installing Revive...");
        });

        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(installerPath);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("REVIVE: " + line);
                }

                int exitValue = process.waitFor();
                System.out.println("Revive installer exited with code: " + exitValue);

                SwingUtilities.invokeLater(() -> {
                    stepInProgress = false;
                    stepCompleted = true;
                    progressAnimator.stop();
                    nextBtn.setEnabled(true);
                    steamPatchStartBtn.setEnabled(true);
                    steamPatchProgressLbl.setText("Revive installation complete!");
                    dlProgressLabel.setText("Revive installed successfully!");
                    progPanel.repaint();
                    statusBarBox.repaint();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    new ErrorDialog().errorDialog(FrameGuidancePC.this, "Installation Failed",
                        "Failed to install Revive: " + e.getMessage(), 0);
                    resetAfterError(steamPatchStartBtn);
                });
            }
        }).start();
    }

}
