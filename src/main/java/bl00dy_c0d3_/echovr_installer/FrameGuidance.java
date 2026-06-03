package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameGuidance extends JDialog {

    private int FW = 700;
    private int FH = 594;
    private static final Color BOX_BORDER = new Color(50, 50, 50, 150);
    private static final int SIDEBAR_W = 120;

    private WizardState wizardState = new WizardState();
    private int currentStep = 0;
    private int currentSubstep = 0;
    private JPanel contentPanel;
    private JPanel progPanel;
    private JPanel statusBarBox;
    private SpecialButton backBtn;
    private SpecialButton nextBtn;
    private TipBox tipBox;
    private FrameMain frameMain;

    private Downloader downloader;
    private Downloader downloadPatch;
    private JLabel dlProgressLabel;
    private SpecialLabel pathLbl;
    private SpecialButton dlButton;

    private JPanel sidebarPanel;
    private JLabel sidebarStepLabel;
    private JLabel[] sidebarSubLabels = new JLabel[3];

    private boolean stepInProgress = false;
    private int patchDetailMode = 0; // 0=master, 1=licence, 2=steam
    private boolean stepCompleted = false;
    private javax.swing.Timer progressAnimator;
    private float animPhase = 0f;

    public FrameGuidance(FrameMain frameMain) {
        super(frameMain, true);
        this.frameMain = frameMain;
        initWindow();
        progressAnimator = new javax.swing.Timer(50, e -> {
            animPhase += 0.15f;
            progPanel.repaint();
            statusBarBox.repaint();
        });
        showStep(0, 0);
        setVisible(true);
    }

    private void initWindow() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (confirmAbortDownload()) dispose();
            }
        });
        setResizable(false);
        setIconImage(loadGUI("icon.png"));
        setTitle("Echo VR Installer");
        Background back = new Background("EchoArena.jpg", -1, FH);
        int w = back.getWidth(); if (w > 0) FW = w;
        back.setLayout(null); setContentPane(back);

        tipBox = new TipBox();
        int contentBoxX = SIDEBAR_W + 30; // 10(edge) + 130(sidebar) + 10(gap) = 150
        int contentBoxW = FW - contentBoxX - 10; // right margin 10px

        int statusBarY = 10;
        int statusBarH = 32;
        statusBarBox = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill;
                if (stepInProgress) {
                    float pulse = (float)(Math.sin(animPhase) * 0.5 + 0.5);
                    fill = new Color((int)(50 + pulse * 40), (int)(90 + pulse * 50), (int)(150 + pulse * 60), 255);
                } else if (stepCompleted) {
                    fill = new Color(40, 130, 40, 255);
                } else {
                    fill = new Color(50, 90, 150, 255);
                }
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BOX_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        statusBarBox.setBounds(contentBoxX, statusBarY, contentBoxW, statusBarH);
        statusBarBox.setOpaque(false);
        back.add(statusBarBox);

        dlProgressLabel = new JLabel("", SwingConstants.CENTER);
        dlProgressLabel.setForeground(Color.WHITE);
        dlProgressLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dlProgressLabel.setOpaque(false);
        dlProgressLabel.setBounds(contentBoxX, statusBarY, contentBoxW, statusBarH);
        back.add(dlProgressLabel);
        back.setComponentZOrder(dlProgressLabel, 0);

        int cy = 72;
        contentPanel = new JPanel(null);
        contentPanel.setBounds(contentBoxX + 10, cy, contentBoxW - 20, 245);
        contentPanel.setOpaque(false);
        back.add(contentPanel);
        int ce = Math.max(cy + 245, cy + contentPanel.getHeight());

        int tsy = ce + 10;
        int tw = tipBox.getWidth(), th = tipBox.getHeight();
        tipBox.setLocation(contentBoxX + (contentBoxW - tw) / 2, tsy + 8);
        back.add(tipBox);
        int bY = contentPanel.getY() - 20;
        int bH = (tipBox.getY() + th) - bY + 20;
        back.add(sectionBoxAt(contentBoxX, bY, bH, 15, contentBoxW, new Color(200, 0, 150, 90)));

        buildSidebar(bY, bH, back);
        buildBar(back, FH - 74);

        pack(); setSize(FW, FH); setLocationRelativeTo(frameMain);
    }

    private void buildSidebar(int y, int h, Background back) {
        int sidebarBoxX = 10; // 10px from window edge
        int sidebarBoxW = SIDEBAR_W + 10; // 120 + 10 = 130 (5px padding each side)
        JPanel box = sectionBoxAt(sidebarBoxX, y, h, 15, sidebarBoxW, new Color(100, 0, 50, 220));
        back.add(box);

        sidebarPanel = new JPanel(null);
        sidebarPanel.setBounds(sidebarBoxX + 5, y + 10, SIDEBAR_W, h - 20);
        sidebarPanel.setOpaque(false);

        Font sf = SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(Font.BOLD, 13f) : new Font("Arial", Font.BOLD, 13);

        sidebarStepLabel = new JLabel();
        sidebarStepLabel.setFont(sf); sidebarStepLabel.setForeground(Color.WHITE);
        sidebarStepLabel.setBounds(8, 12, SIDEBAR_W - 16, 20);
        sidebarPanel.add(sidebarStepLabel);

        for (int i = 0; i < 3; i++) {
            sidebarSubLabels[i] = new JLabel();
            sidebarSubLabels[i].setFont(new Font("Arial", Font.PLAIN, 10));
            sidebarSubLabels[i].setBounds(8, 38 + i * 18, SIDEBAR_W - 16, 16);
            sidebarPanel.add(sidebarSubLabels[i]);
        }
        sidebarPanel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                int my = e.getY();
                int sc = getSubstepCount(currentStep);
                for (int i = 0; i < sc; i++) {
                    int sy = 38 + i * 18;
                    if (my >= sy && my < sy + 16 && i < currentSubstep) {
                        if (!confirmAbortDownload()) break;
                        showStep(currentStep, i); break;
                    }
                }
            }
        });
        back.add(sidebarPanel);
        back.setComponentZOrder(sidebarPanel, 0);
    }

    private void updateSidebar() {
        int sc = getSubstepCount(currentStep);
        sidebarStepLabel.setText("Step " + (currentStep + 1));
        for (int i = 0; i < 3; i++) {
            if (i < sc) {
                sidebarSubLabels[i].setVisible(true);
                String p; Color c;
                if (i < currentSubstep) { p = "\u2713 "; c = Color.GRAY; }
                else if (i == currentSubstep) { p = "\u25CF "; c = new Color(0, 180, 0); }
                else { p = "\u25CB "; c = Color.WHITE; }
                sidebarSubLabels[i].setText(p + getSubstepName(currentStep, i));
                sidebarSubLabels[i].setForeground(c);
            } else sidebarSubLabels[i].setVisible(false);
        }
    }

    private int getSubstepCount(int s) {
        if (s == 0 || s == 1 || s == 2 || s == 3 || s == 5) return 1;
        if (s == 4) return wizardState.getPlayStyle() == WizardState.PlayStyle.STEAMVR ? 2 : 1;
        return 1;
    }

    private String getSubstepName(int s, int sub) {
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

    private void showStep(int s, int sub) {
        currentStep = s; currentSubstep = sub;
        patchDetailMode = 0;
        stepCompleted = false;
        stepInProgress = false;
        progressAnimator.stop();
        updateBar(); updateSidebar();
        contentPanel.removeAll();
        buildContent(s, sub);
        contentPanel.revalidate(); contentPanel.repaint();
        backBtn.setEnabled(!(s == 0 && sub == 0));
        int sc = getSubstepCount(s);
        boolean lastSub = (sub >= sc - 1);
        nextBtn.setEnabled(s < 5);
        nextBtn.changeText(s == 5 ? "Finish" : "Next >");
        updateStatusText(s, sub);
    }

    private void updateStatusText(int s, int sub) {
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

    private boolean confirmAbortDownload() {
        if (!stepInProgress) return true;
        int choice = JOptionPane.showConfirmDialog(FrameGuidance.this,
            "Installation is still in progress.\n\nAbort and continue?",
            "Install not done", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            if (downloader != null) downloader.cancelDownload();
            stepInProgress = false;
            progressAnimator.stop();
            if (dlButton != null) dlButton.changeText("Start Download");
            return true;
        }
        return false;
    }

    private void goBack() {
        if (!confirmAbortDownload()) return;
        if (currentSubstep > 0) showStep(currentStep, currentSubstep - 1);
        else if (currentStep > 0) showStep(currentStep - 1, getSubstepCount(currentStep - 1) - 1);
    }

    private void advance() {
        if (currentStep == 6) { dispose(); return; }
        if (currentStep == 3 && !stepInProgress) {
            String checkPath = wizardState.getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10/echovr.exe";
            System.out.println("ECHO CHECK: path=" + checkPath + " exists=" + new File(checkPath).exists());
            if (!new File(checkPath).exists()) {
                int choice = JOptionPane.showConfirmDialog(FrameGuidance.this,
                    "Echo VR hasn't been installed yet.\n\nStart download now?",
                    "Echo VR not found", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    triggerDownload();
                } else {
                    showStep(4, 0);
                }
                return;
            }
        }
        int sc = getSubstepCount(currentStep);
        if (currentSubstep < sc - 1) showStep(currentStep, currentSubstep + 1);
        else showStep(currentStep + 1, 0);
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
            downloader.startDownload("ready-at-dawn-echo-arena.zip", pathLbl.getText(), "ready-at-dawn-echo-arena.zip", dlProgressLabel, FrameGuidance.this, frameMain, 0, false, 0, false);
        }).start();
    }

    private void buildContent(int s, int sub) {
        int cx = contentPanel.getWidth();
        switch (s) {
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
            public void mouseReleased(MouseEvent e) { wizardState.setPlayStyle(WizardState.PlayStyle.STEAMVR); nextBtn.setEnabled(true); advance(); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Use this if you launch Echo VR through SteamVR with Revive. A Steam patch will be available in the next step."); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(steamvr);

        SpecialButton meta = new SpecialButton("Meta Link", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        meta.setLocation((cx - meta.getWidth()) / 2, 133);
        meta.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { wizardState.setPlayStyle(WizardState.PlayStyle.META_LINK); nextBtn.setEnabled(true); advance(); }
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
                pathFolderChooser(pathLbl, FrameGuidance.this);
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

        dlButton = new SpecialButton("Start Download", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        dlButton.setLocation((cx - dlButton.getWidth()) / 2, 102);
        dlButton.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (stepInProgress) {
                    if (downloader != null) downloader.cancelDownload();
                    stepInProgress = false;
                    progressAnimator.stop();
                    dlButton.changeText("Start Download");
                    dlProgressLabel.setText("Ready to download");
                    nextBtn.setEnabled(true);
                } else {
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
        if (patchDetailMode > 0) { showPatchDetail(0); return; }
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
            showLicencePatchInline(cx, true);
            if (currentStep == 4) {
                for (int i = 0; i < 3; i++) {
                    if (sidebarSubLabels[i] != null) {
                        sidebarSubLabels[i].setText(getSubstepName(4, i));
                    }
                }
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
            indicator.setForeground(new Color(0, 220, 0));
            indicator.setFont(new Font("Arial", Font.BOLD, 18));
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
                        new ErrorDialog().errorDialog(FrameGuidance.this, "Wrong path", "Check your path", 0);
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
                    downloadPatch.startDownload(patchUrl, ep, "pnsovr.dll", new SpecialLabel(" 0%", 13), FrameGuidance.this, null, 3, true, -1, false);
                });
            } catch (java.util.concurrent.ExecutionException ex) {
                Throwable cause = ex.getCause();
                System.out.println("OAuth2 ERROR (ExecutionException): " + (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : "null cause"));
                if (cause != null) cause.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (cause instanceof DiscordOAuth2Flow.OAuth2Exception oae) {
                        if ("not_in_guild".equals(oae.getErrorCode())) {
                            new ErrorDialog().errorDialog(FrameGuidance.this, "Join Server First",
                                    oae.getMessage(), 0);
                        } else if ("busy".equals(oae.getErrorCode())) {
                            new ErrorDialog().errorDialog(FrameGuidance.this, "Bot Busy",
                                    oae.getMessage(), 0);
                        } else {
                            new ErrorDialog().errorDialog(FrameGuidance.this, "Authorization Failed",
                                    oae.getMessage(), 0);
                        }
                    } else {
                        new ErrorDialog().errorDialog(FrameGuidance.this, "Error",
                                "Failed: " + (cause != null ? cause.getMessage() : "Unknown error"), 0);
                    }
                    resetAfterError(triggerBtn);
                });
            } catch (Exception ex) {
                System.out.println("OAuth2 ERROR: " + ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (ex instanceof DiscordOAuth2Flow.OAuth2Exception oae) {
                        if ("not_in_guild".equals(oae.getErrorCode())) {
                            new ErrorDialog().errorDialog(FrameGuidance.this, "Join Server First",
                                    oae.getMessage(), 0);
                        } else if ("busy".equals(oae.getErrorCode())) {
                            new ErrorDialog().errorDialog(FrameGuidance.this, "Bot Busy",
                                    oae.getMessage(), 0);
                        } else {
                            new ErrorDialog().errorDialog(FrameGuidance.this, "Authorization Failed",
                                    oae.getMessage(), 0);
                        }
                    } else {
                        new ErrorDialog().errorDialog(FrameGuidance.this, "Error",
                                "Timed out or cancelled. Try again.", 0);
                    }
                    resetAfterError(triggerBtn);
                });
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
            public void mouseReleased(MouseEvent e) { showPatchDetail(0); }
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
        pathStatusLabel.setBounds((cx - 440) / 2 + 445, 100, 60, 22);
        pathStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(pathStatusLabel);
        updatePathStatus(pathStatusLabel, savedPath, patchPathLbl);

        SpecialButton choosePathBtn = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        choosePathBtn.setLocation((cx - choosePathBtn.getWidth()) / 2, 127);
        choosePathBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                pathFolderChooser(patchPathLbl, FrameGuidance.this);
                String newPath = patchPathLbl.getText();
                wizardState.setInstallPath(newPath);
                updatePathStatus(pathStatusLabel, newPath, patchPathLbl);
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Choose your Echo VR install folder"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(choosePathBtn);

        SpecialButton oauthBtn = new SpecialButton("Authorize with Discord", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
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
                                new ErrorDialog().errorDialog(FrameGuidance.this, "Wrong path", "Check your path", 0);
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
                            downloadPatch.startDownload(patchUrl, ep, "pnsovr.dll", new SpecialLabel(" 0%", 13), FrameGuidance.this, null, 3, true, -1, false);
                        });
                    } catch (java.util.concurrent.ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        System.out.println("OAuth2 ERROR (ExecutionException): " + (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : "null cause"));
                        if (cause != null) cause.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            if (cause instanceof DiscordOAuth2Flow.OAuth2Exception oae) {
                                if ("not_in_guild".equals(oae.getErrorCode())) {
                                    new ErrorDialog().errorDialog(FrameGuidance.this, "Join Server First",
                                            oae.getMessage(), 0);
                                } else if ("busy".equals(oae.getErrorCode())) {
                                    new ErrorDialog().errorDialog(FrameGuidance.this, "Bot Busy",
                                            oae.getMessage(), 0);
                                } else {
                                    new ErrorDialog().errorDialog(FrameGuidance.this, "Authorization Failed",
                                            oae.getMessage(), 0);
                                }
                            } else {
                                new ErrorDialog().errorDialog(FrameGuidance.this, "Error",
                                        "Failed: " + (cause != null ? cause.getMessage() : "Unknown error"), 0);
                            }
                            resetAfterError(oauthBtn);
                        });
                    } catch (Exception ex) {
                        System.out.println("OAuth2 ERROR: " + ex.getClass().getName() + ": " + ex.getMessage());
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            if (ex instanceof DiscordOAuth2Flow.OAuth2Exception oae) {
                                if ("not_in_guild".equals(oae.getErrorCode())) {
                                    new ErrorDialog().errorDialog(FrameGuidance.this, "Join Server First",
                                            oae.getMessage(), 0);
                                } else if ("busy".equals(oae.getErrorCode())) {
                                    new ErrorDialog().errorDialog(FrameGuidance.this, "Bot Busy",
                                            oae.getMessage(), 0);
                                } else {
                                    new ErrorDialog().errorDialog(FrameGuidance.this, "Authorization Failed",
                                            oae.getMessage(), 0);
                                }
                            } else {
                                new ErrorDialog().errorDialog(FrameGuidance.this, "Error",
                                        "Timed out or cancelled. Try again.", 0);
                            }
                            resetAfterError(oauthBtn);
                        });
                    }
                }).start();
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Authorize with Discord to generate your personalized patch"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(oauthBtn);
    }

    private void buildSteamPatchDetail(int cx) {
        JLabel h = makeHeader("Steam Patch (Revive)");
        h.setBounds((cx - 450) / 2, 35, 450, 55); contentPanel.add(h);

        JLabel info = new JLabel("<html><center>This patch is for players using SteamVR with Revive<br>on non-Oculus headsets (Valve Index, HTC Vive, etc).<br><br>Click below to open the Steam Patch wizard.</center></html>", SwingConstants.CENTER);
        info.setBounds(30, 95, cx - 60, 80);
        info.setForeground(Color.WHITE);
        info.setFont(new Font("Arial", Font.PLAIN, 13));
        contentPanel.add(info);

        SpecialButton startBtn = new SpecialButton("Open Steam Patch Wizard", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        startBtn.setLocation((cx - startBtn.getWidth()) / 2, 185);
        startBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { new FrameSteamPatcher(frameMain); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Open the SteamVR patch configuration wizard"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(startBtn);
    }

    private void resetAfterError(SpecialButton btn) {
        stepInProgress = false;
        progressAnimator.stop();
        btn.setEnabled(true);
        dlProgressLabel.setText("Patch failed. Try again.");
        progPanel.repaint();
        statusBarBox.repaint();
    }

    private void buildBar(Background back, int barY) {
        int barH = 42;

        backBtn = new SpecialButton("< Back", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        backBtn.setEnabled(false);
        backBtn.addMouseListener(new MouseAdapter() { public void mouseReleased(MouseEvent e) { goBack(); } });
        back.add(backBtn);

        nextBtn = new SpecialButton("Next >", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        nextBtn.setEnabled(false);
        nextBtn.addMouseListener(new MouseAdapter() { public void mouseReleased(MouseEvent e) { advance(); } });
        back.add(nextBtn);

        String[] chipNames = {"Type", "Play", "Path", "Download", "Patch", "Done"};
        int[] chipWidths = new int[6];
        Font chipFont = SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(9f) : new Font("Arial", Font.PLAIN, 9);
        FontMetrics chipFm = getFontMetrics(chipFont);
        for (int i = 0; i < 6; i++) {
            chipWidths[i] = Math.max(40, Math.min(74, chipFm.stringWidth(chipNames[i]) + 16));
        }
        int gap = 3;
        int totalChipsW = 0; for (int w : chipWidths) totalChipsW += w;
        int totalNavW = totalChipsW + 5 * gap;

        progPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String[] names = {"Type", "Play", "Path", "Download", "Patch", "Done"};
                int chipH = 24;
                int totalW = 0; for (int w : chipWidths) totalW += w; totalW += 5 * gap;
                int chipY = (barH - chipH) / 2;
                int sx = (getWidth() - totalW) / 2;
                int cx = sx;
                for (int i = 0; i < 6; i++) {
                    int cw = chipWidths[i];
                    Color bg, fg;
                    if (i < currentStep) { bg = new Color(60, 60, 60); fg = Color.LIGHT_GRAY; }
                    else if (i == currentStep) {
                        if (stepInProgress) {
                            float pulse = (float)(Math.sin(animPhase) * 0.5 + 0.5);
                            int green = (int)(140 + pulse * 80);
                            bg = new Color(0, Math.min(green, 255), 0); fg = Color.WHITE;
                        } else { bg = new Color(0, 180, 0); fg = Color.WHITE; }
                    }
                    else { bg = new Color(40, 40, 40); fg = Color.WHITE; }
                    g2.setColor(bg); g2.fillRoundRect(cx, chipY, cw, chipH, 8, 8);
                    g2.setFont(SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(9f) : new Font("Arial", Font.PLAIN, 9));
                    g2.setColor(fg);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(names[i], cx + (cw - fm.stringWidth(names[i])) / 2, chipY + ((chipH - fm.getHeight()) / 2) + fm.getAscent());
                    if (i < 5) { g2.setFont(new Font("Arial", Font.PLAIN, 12)); g2.setColor(Color.GRAY); g2.drawString(">", cx + cw + 5, chipY + chipH / 2 + 5); }
                    cx += cw + gap;
                }
                g2.dispose();
            }
        };
        progPanel.setOpaque(false);
        back.add(progPanel);
        progPanel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                int totalChipW = 0; for (int w : chipWidths) totalChipW += w; totalChipW += 5 * gap;
                int sx = (totalNavW - totalChipW) / 2;
                int mx = e.getX();
                int cx = sx;
                for (int i = 0; i < 6; i++) {
                    int cw = chipWidths[i];
                    if (mx >= cx && mx < cx + cw) {
                        if (!confirmAbortDownload()) break;
                        if (i == 4) {
                            String checkPath = wizardState.getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10/echovr.exe";
                            if (!new File(checkPath).exists()) {
                                int choice = JOptionPane.showConfirmDialog(FrameGuidance.this,
                                    "Echo VR needs to be installed first.\n\nGo to download step?",
                                    "Echo not installed", JOptionPane.YES_NO_OPTION);
                                if (choice == JOptionPane.YES_OPTION) showStep(3, 0);
                                else showStep(i, 0);
                                break;
                            }
                        }
                        if (i < currentStep) showStep(i, getSubstepCount(i) - 1);
                        else if (i == currentStep) showStep(currentStep, 0);
                        else showStep(i, 0);
                        break;
                    }
                    cx += cw + gap;
                }
            }
        });

        int btnH = 25;
        int itemY = barY + (barH - btnH) / 2;
        int contentBoxX = SIDEBAR_W + 30;  // 150
        int contentBoxW = FW - contentBoxX - 10; // 540
        int gapN = 3;
        int totalNavWidth = backBtn.getWidth() + gapN + totalNavW + gapN + nextBtn.getWidth();
        int sectionW = contentBoxW, sectionX = contentBoxX;
        int navX = sectionX + (sectionW - totalNavWidth) / 2;
        backBtn.setLocation(navX, itemY);
        progPanel.setBounds(navX + backBtn.getWidth() + gapN, barY, totalNavW, barH);
        nextBtn.setLocation(progPanel.getX() + progPanel.getWidth() + gapN, itemY);
        back.add(sectionBoxAt(contentBoxX, barY, barH, 15, contentBoxW, new Color(100, 0, 50, 220)));
    }

    private void updateBar() { progPanel.repaint(); }

    private JLabel makeHeader(String text) {
        ImageIcon icon = new ImageIcon(loadGUI("tipbox_top.png"));
        int w = 450, ih = (int) ((double) icon.getIconHeight() * w / icon.getIconWidth());
        Image s = icon.getImage().getScaledInstance(w, ih, Image.SCALE_SMOOTH);
        String html = "<html><table width='400' align='center'><tr><td align='center'>" + text + "</td></tr></table></html>";
        JLabel l = new JLabel(html, new ImageIcon(s), SwingConstants.CENTER);
        l.setHorizontalTextPosition(JLabel.CENTER); l.setVerticalTextPosition(JLabel.CENTER);
        l.setSize(w, Math.max(ih, 55)); l.setForeground(Color.WHITE);
        l.setFont(SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(Font.PLAIN, 14f) : new Font("Arial", Font.BOLD, 14));
        return l;
    }

    private SpecialLabel makeRoundedLabel(String text, int fontSize) {
        SpecialLabel lbl = new SpecialLabel(text, fontSize) {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        return lbl;
    }

    private void addRoundedImage(String img, int x, int y, int w, int h) {
        Background bg = new Background(img, w, h) {
            @Override public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10); g2.dispose();
            }
        }; bg.setLocation(x, y); bg.setSize(w, h); contentPanel.add(bg);
    }

    private JPanel sectionBox(int y, int h, int arc, int w, Color fill) {
        JPanel p = new JPanel(null) {
            @Override public boolean contains(int x, int y) { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(BOX_BORDER); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); g2.dispose();
            }
        };
        p.setBounds((FW - w) / 2, y, w, h); p.setOpaque(false); return p;
    }

    private JPanel sectionBoxAt(int x, int y, int h, int arc, int w, Color fill) {
        JPanel p = new JPanel(null) {
            @Override public boolean contains(int x, int y) { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(BOX_BORDER); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); g2.dispose();
            }
        };
        p.setBounds(x, y, w, h); p.setOpaque(false); return p;
    }
}
