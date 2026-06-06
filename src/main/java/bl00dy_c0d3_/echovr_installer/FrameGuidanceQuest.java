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
    protected String getWindowTitle() { return "Echo VR Installer"; }

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
        JLabel h = makeHeader("Do you own Echo VR on your Meta account?");
        h.setBounds((cx - 450) / 2, 8, 450, 55); contentPanel.add(h);

        SpecialButton own = new SpecialButton("I own Echo on Meta", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        own.setLocation((cx - own.getWidth()) / 2, 73);
        own.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                questState.setUserType(WizardState.UserType.OWNER);
                nextBtn.setEnabled(true);
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
                nextBtn.setEnabled(true);
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
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        apkProgressLabel = new SpecialLabel("Not started", 13);
        apkProgressLabel.setLocation((cx - 440) / 2, 70);
        apkProgressLabel.setSize(440, 22);
        apkProgressLabel.setBackground(new Color(255, 255, 255, 200));
        apkProgressLabel.setForeground(Color.BLACK);
        contentPanel.add(apkProgressLabel);

        dataProgressLabel = new SpecialLabel("Not started", 13);
        dataProgressLabel.setLocation((cx - 440) / 2, 97);
        dataProgressLabel.setSize(440, 22);
        dataProgressLabel.setBackground(new Color(255, 255, 255, 200));
        dataProgressLabel.setForeground(Color.BLACK);
        contentPanel.add(dataProgressLabel);

        if (questState.getUserType() == WizardState.UserType.OWNER) {
            dlButton = new SpecialButton("Start Download", "button_up.png", "button_down.png", "button_highlighted.png", 18);
            dlButton.setLocation((cx - dlButton.getWidth()) / 2, 130);
            dlButton.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (stepInProgress) {
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
            dlButton = new SpecialButton("Authorize with Discord", "button_up.png", "button_down.png", "button_highlighted.png", 18);
            dlButton.setLocation((cx - dlButton.getWidth()) / 2, 130);
            dlButton.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (!stepInProgress) startOAuth2Download();
                }
                public void mouseEntered(MouseEvent e) { tipBox.showTip("Authorize with Discord to get your patched APK"); }
                public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
            }); contentPanel.add(dlButton);
        }
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

    private void startOAuth2Download() {
        dlButton.setEnabled(false);
        nextBtn.setEnabled(false);
        stepInProgress = true;
        downloadCompleteCount = 0;
        progressAnimator.start();

        new Thread(() -> {
            try {
                DiscordOAuth2Flow flow = new DiscordOAuth2Flow("apk");
                String patchUrl = flow.start(status ->
                        SwingUtilities.invokeLater(() -> dlProgressLabel.setText(status))
                ).get(300, TimeUnit.SECONDS);

                String finalPatchUrl = patchUrl;
                System.out.println("OAuth2 SUCCESS: URL=" + finalPatchUrl);
                SwingUtilities.invokeLater(() -> downloadPatchedApk(finalPatchUrl));
            } catch (java.util.concurrent.ExecutionException ex) {
                Throwable cause = ex.getCause();
                System.out.println("OAuth2 ERROR (ExecutionException): " +
                        (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : "null cause"));
                if (cause != null) cause.printStackTrace();
                SwingUtilities.invokeLater(() -> handleOAuth2Error(cause));
            } catch (Exception ex) {
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
            onDownloadFileComplete();

            new Thread(() -> {
                Downloader dataDl = new Downloader();
                dataDl.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                    dataProgressLabel.setText("100.00%");
                    onDownloadFileComplete();
                    dlButton.changeText("Authorize with Discord");
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
            } else {
                new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Authorization Failed",
                        oae.getMessage(), 0);
            }
        } else {
            new ErrorDialog().errorDialog(FrameGuidanceQuest.this, "Error",
                    "Failed: " + (error != null ? error.getMessage() : "Unknown error"), 0);
        }
        resetAfterError(dlButton);
    }

    // === Step 2: Install to Quest ===

    private void buildStep2(int cx) {
        JLabel h = makeHeader("Install Echo VR to your Quest");
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        installProgressLabel = new SpecialLabel("Not started yet", 15);
        installProgressLabel.setLocation((cx - 440) / 2, 70);
        installProgressLabel.setSize(440, 30);
        installProgressLabel.setBackground(new Color(255, 255, 255, 200));
        installProgressLabel.setForeground(Color.BLACK);
        contentPanel.add(installProgressLabel);

        SpecialButton installBtn = new SpecialButton("Install to Quest", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        installBtn.setLocation((cx - installBtn.getWidth()) / 2, 115);
        installBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (stepInProgress) return;
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
