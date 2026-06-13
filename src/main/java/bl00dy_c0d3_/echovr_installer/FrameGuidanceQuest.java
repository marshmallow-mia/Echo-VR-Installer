package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameGuidanceQuest extends BaseWizard {

    private QuestWizardState questState;
    private Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    private SpecialLabel apkProgressLabel;
    private SpecialLabel dataProgressLabel;
    private SpecialLabel installProgressLabel;
    private int downloadCompleteCount = 0;
    // Cancelled before a Retry so the previous attempt's callback socket is freed.
    private DiscordOAuth2Flow activeOAuthFlow;
    // True once a patched APK has actually been downloaded in THIS session — only then may a Retry
    // reuse the staged temp file (otherwise a stale APK would short-circuit OAuth on the first click).
    private boolean patchedApkReady = false;

    public FrameGuidanceQuest(FrameMain frameMain) {
        super(frameMain);
        questState = new QuestWizardState();
        Background back = (Background) getContentPane();
        int bY = contentPanel.getY() - 20;
        int bH = (tipBox.getY() + tipBox.getHeight()) - bY + 20;
        buildSidebar(bY, bH, back);
        buildBar(back, FH - 74);
        pack(); setSize(FW, FH); setLocationRelativeTo(frameMain);
        showStep(0, 0);
        setVisible(true);
    }

    // === Abstract Method Implementations ===

    @Override
    protected String getBackgroundImage() { return "Echo2.jpg"; }

    @Override
    protected int getWindowWidth() { return 700; }

    @Override
    protected int getWindowHeight() {
        java.awt.Image img = loadGUI("Echo2.jpg");
        if (img != null) {
            int w = img.getWidth(null);
            int h = img.getHeight(null);
            if (w > 0 && h > 0) { return (int) ((double) h * 700 / w); }
        }
        return 380;
    }

    @Override
    protected String getWindowTitle() { return "Echo VR Installer v0.9.4b.003"; }

    @Override
    protected int getStepCount() { return 4; }

    @Override
    protected String getChipLabel(int step) {
        String[] chips = {"Type", "Download", "Install", "Done"};
        return step >= 0 && step < chips.length ? chips[step] : "";
    }

    @Override
    protected int getSubstepCount(int step) { return 1; }

    @Override
    protected String getSubstepName(int step, int sub) {
        String[] names = {"Choose Type", "Download", "Install to Quest", "All Done"};
        return step >= 0 && step < names.length ? names[step] : "";
    }

    @Override
    protected void updateStatusText(int step, int sub) {
        String[] texts = {
            "Choose your player type",
            "Ready to download",
            "Install to Quest",
            "Echo VR installation complete!"
        };
        if (step >= 0 && step < texts.length) {
            dlProgressLabel.setText(texts[step]);
        }
    }

    @Override
    protected boolean canAdvanceFrom(int step, int sub) {
        if (step == 0 && questState.getUserType() == null) {
            JOptionPane.showMessageDialog(FrameGuidanceQuest.this,
                "Please select whether you own Echo VR or are a new player before continuing.",
                "No Player Type Selected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (step == 2) {
            return stepCompleted;
        }
        return true;
    }

    @Override
    protected void buildContent(int step, int sub, int cx) {
        switch (step) {
            case 0: buildStep0(cx); break;
            case 1: buildStep1(cx); break;
            case 2: buildStep2(cx); break;
            case 3: buildStep3(cx); break;
        }
    }

    // === Step 0: Type Selection ===

    private void buildStep0(int cx) {
        // Reset selection so the user is forced to choose again
        questState.setUserType(null);

        JLabel h = makeHeader("Do you own Echo VR on your Meta account?");
        h.setBounds((cx - 450) / 2, 8, 450, 55); contentPanel.add(h);

        SpecialButton own = new SpecialButton("I own Echo on Meta", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        own.setLocation((cx - own.getWidth()) / 2, 73);
        own.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                questState.setUserType(WizardState.UserType.OWNER);
                advance();
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("You already own Echo VR"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(own);

        SpecialButton np = new SpecialButton("I'm a new player", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        np.setLocation((cx - np.getWidth()) / 2, 133);
        np.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                questState.setUserType(WizardState.UserType.NEW_PLAYER);
                advance();
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("You need to patch Echo VR"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(np);
    }

    // === Step 1: Download ===

    private void buildStep1(int cx) {
        downloadCompleteCount = 0;

        String headerText = questState.getUserType() == WizardState.UserType.OWNER
                ? "Download Echo VR client files"
                : "Authorize and download patched Echo VR";
        JLabel h = makeHeader(headerText);
        h.setBounds((cx - 450) / 2, 5, 450, 46); contentPanel.add(h);

        apkProgressLabel = new SpecialLabel("Not started", 13);
        apkProgressLabel.setLocation((cx - 440) / 2, 59);
        apkProgressLabel.setSize(440, 22);
        apkProgressLabel.setBackground(new Color(255, 255, 255, 200));
        apkProgressLabel.setForeground(Color.BLACK);
        contentPanel.add(apkProgressLabel);

        dataProgressLabel = new SpecialLabel("Not started", 13);
        dataProgressLabel.setLocation((cx - 440) / 2, 89);
        dataProgressLabel.setSize(440, 22);
        dataProgressLabel.setBackground(new Color(255, 255, 255, 200));
        dataProgressLabel.setForeground(Color.BLACK);
        contentPanel.add(dataProgressLabel);

        if (questState.getUserType() == WizardState.UserType.OWNER) {
            dlButton = new SpecialButton("Start Download", "button_up.png", "button_down.png", "button_highlighted.png", 18);
            dlButton.setLocation((cx - dlButton.getWidth()) / 2, 119);
            dlButton.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (stepInProgress) {
                        if (!confirmAbortDownload()) return;
                        cancelDownloads();
                        stepInProgress = false;
                        progressAnimator.stop();
                        dlButton.changeText("Start Download");
                        dlProgressLabel.setText("Ready to download");
                        nextBtn.setEnabled(true);
                    } else {
                        startOwnerDownload();
                    }
                }
                public void mouseEntered(MouseEvent e) { tipBox.showTip("Download the Echo VR APK and data files"); }
                public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
            }); contentPanel.add(dlButton);
        } else {
            // Patch-options panel: "Authorize with Discord" + "— or —" + custom-URL row, in one box.
            dlButton = new SpecialButton("Authorize with Discord", "button_up.png", "button_down.png", "button_highlighted.png", 18);
            SpecialTextfield urlField = buildPatchOptionsPanel(cx, 119, dlButton, "Start Patching",
                "Paste patch URL here…",
                "Paste a direct URL to use a custom patch instead of generating one.",
                t -> validateApkUrl(t) != null,
                () -> patchedApkReady = false);   // mode switch → don't reuse the staged download

            dlButton.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (stepInProgress) {
                        if (!confirmAbortDownload()) return;
                        if (activeOAuthFlow != null) { activeOAuthFlow.cancel(); activeOAuthFlow = null; }
                        cancelDownloads();
                        stepInProgress = false;
                        progressAnimator.stop();
                        markQuestRetry();
                        dlProgressLabel.setText("Ready to download");
                        nextBtn.setEnabled(true);
                    } else {
                        startPatchedDownload(urlField);
                    }
                }
                public void mouseEntered(MouseEvent e) { tipBox.showTip(urlField.isEnabled()
                    ? "Download and install the patched APK from your pasted URL"
                    : "Authorize with Discord to get your patched APK"); }
                public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
            });
        }
    }

    private void markQuestRetry() {
        if (dlButton != null) dlButton.changeText("Retry");
    }

    private void markQuestDone() {
        if (dlButton != null) dlButton.changeText("Done");
    }

    /** Accepts a files.echovr.de patched-APK URL; null otherwise. */
    private String validateApkUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        return url.matches("https://files\\.echovr\\.de/.*") ? url : null;
    }

    private void cancelDownloads() {
        if (downloader != null) downloader.cancelDownload();
        if (downloadPatch != null) downloadPatch.cancelDownload();
    }

    private synchronized void onDownloadFileComplete() {
        downloadCompleteCount++;
        if (downloadCompleteCount >= 2) {
            onAllDownloadsComplete();
        }
    }

    private void onAllDownloadsComplete() {
        stepInProgress = false;
        stepCompleted = true;
        progressAnimator.stop();
        nextBtn.setEnabled(true);
        dlButton.changeText("Start Download");
        dlProgressLabel.setText("Complete");
        progPanel.repaint();
        statusBarBox.repaint();
    }

    // --- Owner download flow ---

    private void startOwnerDownload() {
        dlProgressLabel.setText("Downloading...");
        nextBtn.setEnabled(false);
        stepInProgress = true;
        stepCompleted = false;
        downloadCompleteCount = 0;
        progressAnimator.start();
        dlButton.changeText("Cancel Download");
        cancelDownloads();

        new Thread(() -> {
            downloader = new Downloader();
            downloader.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                apkProgressLabel.setText("100.00%");
                onDownloadFileComplete();
            }));
            downloader.startDownload("r15_26-06-25.apk", tempPath.toString(), "r15_26-06-25.apk",
                    apkProgressLabel, FrameGuidanceQuest.this, null, 2, false, 0, false);
        }).start();

        pause(2);

        new Thread(() -> {
            downloadPatch = new Downloader();
            downloadPatch.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                dataProgressLabel.setText("100.00%");
                onDownloadFileComplete();
            }));
            downloadPatch.startDownload("_data.zip", tempPath.toString(), "_data.zip",
                    dataProgressLabel, FrameGuidanceQuest.this, null, 2, false, 0, false);
        }).start();
    }

    // --- New player OAuth2 + download flow ---

    /**
     * Drives the patched-APK download: a valid custom URL (pasted into the patch-options panel)
     * bypasses OAuth; an already-downloaded temp APK is reused on Retry without re-authorizing;
     * otherwise the Discord OAuth flow runs. Any previous in-flight attempt is cancelled first.
     */
    private void startPatchedDownload(SpecialTextfield urlField) {
        if (activeOAuthFlow != null) { activeOAuthFlow.cancel(); activeOAuthFlow = null; }
        cancelDownloads();

        dlButton.setEnabled(false);
        nextBtn.setEnabled(false);
        stepInProgress = true;
        downloadCompleteCount = 0;
        progressAnimator.start();

        // The custom-URL path is taken only when its field is enabled (checkbox ticked).
        boolean advancedOn = urlField.isEnabled();
        String advUrl = advancedOn ? validateApkUrl(urlField.getText().trim()) : null;
        if (advancedOn && advUrl == null) {
            new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Wrong URL provided",
                "Your provided Download Link is wrong. Please check!", 0);
            markQuestRetry();
            resetAfterError(dlButton);
            return;
        }
        dlButton.changeText("Please Wait...");   // work is starting (OAuth / download)

        if (advUrl != null) {
            dlProgressLabel.setText("Downloading patched APK from link...");
            downloadPatchedApk(advUrl);
            return;
        }

        // Reuse a patched APK downloaded earlier in THIS session on Retry (skips OAuth + re-download).
        // Gated on patchedApkReady so a stale APK from a previous run can't short-circuit OAuth.
        Path tempApk = tempPath.resolve(questState.getApkFilename());
        if (patchedApkReady && java.nio.file.Files.exists(tempApk)) {
            dlProgressLabel.setText("Using downloaded patch...");
            apkProgressLabel.setText("100.00%");
            questState.setPatchedApk(true);
            onDownloadFileComplete();
            new Thread(() -> {
                Downloader dataDl = new Downloader();
                dataDl.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                    dataProgressLabel.setText("100.00%");
                    onDownloadFileComplete();
                    markQuestDone();
                }));
                dataDl.startDownload("_data.zip", tempPath.toString(), "_data.zip",
                        dataProgressLabel, FrameGuidanceQuest.this, null, 2, false, 0, false);
            }).start();
            return;
        }

        new Thread(() -> {
            try {
                DiscordOAuth2Flow flow = new DiscordOAuth2Flow("apk");
                activeOAuthFlow = flow;
                String patchUrl = flow.start(status ->
                        SwingUtilities.invokeLater(() -> dlProgressLabel.setText(status))
                ).get(120, TimeUnit.SECONDS);
                activeOAuthFlow = null;

                String finalPatchUrl = patchUrl;
                System.out.println("OAuth2 SUCCESS: URL=" + finalPatchUrl);
                SwingUtilities.invokeLater(() -> downloadPatchedApk(finalPatchUrl));
            } catch (java.util.concurrent.ExecutionException ex) {
                activeOAuthFlow = null;
                Throwable cause = ex.getCause();
                System.out.println("OAuth2 ERROR (ExecutionException): " +
                        (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : "null cause"));
                if (cause != null) cause.printStackTrace();
                SwingUtilities.invokeLater(() -> handleOAuth2Error(cause));
            } catch (Exception ex) {
                activeOAuthFlow = null;
                System.out.println("OAuth2 ERROR: " + ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> handleOAuth2Error(ex));
            }
        }).start();
    }

    private void downloadPatchedApk(String patchUrl) {
        dlProgressLabel.setText("Downloading patched APK...");

        downloadPatch = new Downloader();
        downloadPatch.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
            apkProgressLabel.setText("100.00%");
            questState.setPatchedApk(true);
            patchedApkReady = true;     // a real download finished this session → safe to reuse on Retry
            onDownloadFileComplete();

            new Thread(() -> {
                Downloader dataDl = new Downloader();
                dataDl.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                    dataProgressLabel.setText("100.00%");
                    onDownloadFileComplete();
                    markQuestDone();
                }));
                dataDl.startDownload("_data.zip", tempPath.toString(), "_data.zip",
                        dataProgressLabel, FrameGuidanceQuest.this, null, 2, false, 0, false);
            }).start();
        }));
        downloadPatch.startDownload(patchUrl, tempPath.toString(), questState.getApkFilename(),
                apkProgressLabel, FrameGuidanceQuest.this, null, 2, true, -1, false);
    }

    private void handleOAuth2Error(Throwable error) {
        if (error instanceof DiscordOAuth2Flow.OAuth2Exception oae) {
            if ("not_in_guild".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Join Server First",
                        oae.getMessage(), 0);
            } else if ("busy".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Bot Busy",
                        oae.getMessage(), 0);
            } else if ("cancelled".equals(oae.getErrorCode())) {
                // User/retry-initiated cancel — no dialog.
            } else if ("timeout".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Try again in a minute",
                        oae.getMessage(), 0);
            } else if ("port_in_use".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Authorization busy",
                        oae.getMessage(), 0);
            } else {
                new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Authorization Failed",
                        oae.getMessage(), 0);
            }
        } else {
            new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Error",
                    "Failed: " + (error != null ? error.getMessage() : "Unknown error"), 0);
        }
        markQuestRetry();
        resetAfterError(dlButton);
    }

    // === Step 2: Install to Quest ===

    private void buildStep2(int cx) {
        JLabel h = makeHeader("Install Echo VR to your Quest");
        h.setBounds((cx - 450) / 2, 5, 450, 50); contentPanel.add(h);

        // Connection status row + "Connect to Quest" button.
        JLabel questStatus = new JLabel("Checking Quest connection...", SwingConstants.CENTER);
        questStatus.setBounds(0, 60, cx, 24);
        questStatus.setFont(new Font("Arial", Font.BOLD, 14));
        questStatus.setForeground(Color.LIGHT_GRAY);
        questStatus.setIconTextGap(6);
        contentPanel.add(questStatus);

        SpecialButton connectBtn = new SpecialButton("Connect to Quest", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        connectBtn.setLocation((cx - connectBtn.getWidth()) / 2, 88);
        connectBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { refreshQuestConnection(connectBtn, questStatus, true); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Check that your Quest is connected and has allowed this PC"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(connectBtn);

        // Auto-check on entry (no popups — just updates the status hook).
        refreshQuestConnection(connectBtn, questStatus, false);

        installProgressLabel = new SpecialLabel("Not started yet", 15);
        installProgressLabel.setLocation((cx - 440) / 2, 128);
        installProgressLabel.setSize(440, 28);
        installProgressLabel.setBackground(new Color(255, 255, 255, 200));
        installProgressLabel.setForeground(Color.BLACK);
        contentPanel.add(installProgressLabel);

        SpecialButton installBtn = new SpecialButton("Install to Quest", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        installBtn.setLocation((cx - installBtn.getWidth()) / 2, 166);
        installBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (stepInProgress && !confirmAbortDownload()) return;
                installBtn.setEnabled(false);
                nextBtn.setEnabled(false);
                stepInProgress = true;
                stepCompleted = false;
                progressAnimator.start();
                installProgressLabel.setText("Installation started! Wait!");

                new Thread(() -> {
                    try {
                        InstallerQuest installer = new InstallerQuest();
                        boolean success = installer.installAPK(tempPath.toString(),
                                questState.getApkFilename(), "_data.zip",
                                installProgressLabel, FrameGuidanceQuest.this);
                        SwingUtilities.invokeLater(() -> {
                            stepInProgress = false;
                            if (success) {
                                stepCompleted = true;
                                installProgressLabel.setText("Installation is complete!");
                                dlProgressLabel.setText("Installation complete!");
                            } else {
                                installProgressLabel.setText("Installation did not finish!");
                                dlProgressLabel.setText("Installation failed");
                            }
                            progressAnimator.stop();
                            nextBtn.setEnabled(true);
                            installBtn.setEnabled(true);
                            progPanel.repaint();
                            statusBarBox.repaint();
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            stepInProgress = false;
                            installProgressLabel.setText("Installation error: " + ex.getMessage());
                            progressAnimator.stop();
                            nextBtn.setEnabled(true);
                            installBtn.setEnabled(true);
                            progPanel.repaint();
                            statusBarBox.repaint();
                        });
                    }
                }).start();
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Install the downloaded APK and data to your Quest"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        }); contentPanel.add(installBtn);
    }

    /**
     * Checks (on a background thread) whether the Quest is connected and authorized, then updates
     * the status label with a ✓/✗ hook. When {@code interactive} (user pressed Connect), also shows
     * a guidance dialog if the device is unauthorized or not detected.
     */
    private void refreshQuestConnection(SpecialButton connectBtn, JLabel statusLbl, boolean interactive) {
        connectBtn.setEnabled(false);
        statusLbl.setIcon(null);
        statusLbl.setForeground(Color.LIGHT_GRAY);
        statusLbl.setText("Checking Quest connection...");
        new Thread(() -> {
            int status;
            try { status = InstallerQuest.checkConnection(); }
            catch (Exception ex) { status = -1; }
            final int s = status;
            SwingUtilities.invokeLater(() -> {
                connectBtn.setEnabled(true);
                switch (s) {
                    case 0 -> {
                        statusLbl.setIcon(markIcon(true, new Color(0, 200, 0), 18));
                        statusLbl.setForeground(Color.WHITE);
                        statusLbl.setText("Quest connected");
                    }
                    case 1 -> {
                        statusLbl.setIcon(markIcon(false, new Color(255, 80, 80), 18));
                        statusLbl.setForeground(Color.WHITE);
                        statusLbl.setText("Quest found — not authorized");
                        if (interactive) {
                            new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Allow your PC on the Quest",
                                "<html><center>Your Quest is connected, but it hasn't allowed this PC yet.<br>"
                                + "Put on your headset and tap&nbsp;<b>Allow</b>&nbsp;when the USB debugging prompt appears "
                                + "(replug the cable if you don't see it).</center></html>", 3);
                        }
                    }
                    default -> {
                        statusLbl.setIcon(markIcon(false, new Color(255, 80, 80), 18));
                        statusLbl.setForeground(Color.WHITE);
                        statusLbl.setText("No Quest detected");
                        if (interactive) {
                            new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "No Quest detected",
                                "<html><center>Couldn't find your Quest. Connect it by USB and make sure<br>"
                                + "Developer Mode is enabled.</center></html>", 1);
                        }
                    }
                }
            });
        }).start();
    }

    // === Step 3: Done ===

    private void buildStep3(int cx) {
        JLabel d = new JLabel("You're all set!", SwingConstants.CENTER);
        d.setBounds(0, 55, cx, 40);
        d.setForeground(new Color(0, 255, 0));
        d.setFont(new Font("Arial", Font.BOLD, 24));
        contentPanel.add(d);

        JLabel s = new JLabel("Echo VR is ready to play on your Quest.", SwingConstants.CENTER);
        s.setBounds(0, 105, cx, 24);
        s.setForeground(Color.WHITE);
        s.setFont(new Font("Arial", Font.PLAIN, 16));
        contentPanel.add(s);

        nextBtn.setEnabled(true);
    }
}
