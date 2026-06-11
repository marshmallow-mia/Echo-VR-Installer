package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FramePCUpdate extends BaseWizard {

    private PCWizardState wizardState = new PCWizardState();
    private SpecialTextfield pathField;
    private JLabel pathIndicator;
    private String binPath;
    private SpecialLabel updateProgressLabel;

    public FramePCUpdate(FrameMain frameMain) {
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
    protected int getStepCount() { return 3; }

    @Override
    protected String getChipLabel(int step) {
        String[] chips = {"Path", "Update", "Done"};
        return step >= 0 && step < chips.length ? chips[step] : "";
    }

    @Override
    protected int getSubstepCount(int s) {
        return s == 1 ? 2 : 1;
    }

    @Override
    protected String getSubstepName(int s, int sub) {
        switch (s) {
            case 0: return "Choose Path";
            case 1: return sub == 0 ? "Download" : "Extract";
            case 2: return "All Done";
        }
        return "";
    }

    @Override
    protected boolean canAdvanceFrom(int step, int sub) {
        if (step == 0) {
            return Helpers.hasEchoInstall(wizardState.getInstallPath());
        }
        return true;
    }

    @Override
    protected void updateStatusText(int step, int sub) {
        switch (step) {
            case 0: dlProgressLabel.setText("Choose your Echo VR install path"); break;
            case 1:
                if (sub == 0) dlProgressLabel.setText(stepInProgress ? "Downloading update..." : "Ready to update");
                else dlProgressLabel.setText("Extracting update...");
                break;
            case 2: dlProgressLabel.setText("Echo VR update complete!"); break;
        }
    }

    @Override
    protected void buildContent(int step, int sub, int cx) {
        switch (step) {
            case 0: buildStep0(cx); break;
            case 1: buildStep1(cx, sub); break;
            case 2: buildStep2(cx); break;
        }
    }

    private void buildStep0(int cx) {
        JLabel h = makeHeader("Choose your Echo VR install path");
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        pathIndicator = new JLabel();
        pathIndicator.setBounds((cx - 440) / 2 + 445, 64, 90, 34);
        pathIndicator.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(pathIndicator);

        pathField = makeEditablePathField(cx, 70, pathIndicator);
        contentPanel.add(pathField);

        SpecialButton choosePathBtn = new SpecialButton("Choose path",
            "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        choosePathBtn.setLocation((cx - choosePathBtn.getWidth()) / 2, 102);
        choosePathBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                String chosen = Helpers.chooseFolder(FramePCUpdate.this);
                if (chosen == null) return;
                pathField.setText(chosen);
                commitPathField();
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Select the folder where Echo VR is installed"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(choosePathBtn);

        SpecialButton detectBtn = new SpecialButton("Detect Meta path",
            "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        detectBtn.setLocation((cx - detectBtn.getWidth()) / 2, 134);
        detectBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (!Helpers.isWindows) {
                    new ErrorDialog().errorDialog(FramePCUpdate.this, "Windows only",
                        "Meta/Oculus path detection is only available on Windows.", 0);
                    return;
                }
                String base = Helpers.getOculusBasePath();
                if (base == null || base.trim().isEmpty()) {
                    new ErrorDialog().errorDialog(FramePCUpdate.this, "Meta install not found",
                        "Could not find a Meta/Oculus installation in the registry.\nIs the Meta Quest (Oculus) app installed?", 0);
                    return;
                }
                String installPath = base.endsWith("/") || base.endsWith("\\") ? base : base + "\\";
                installPath += "Software\\Software";
                wizardState.setInstallPath(installPath);
                pathField.setText(wizardState.getInstallPath());
                Helpers.saveInstallPath(wizardState.getInstallPath());
                updatePathStatus(pathIndicator);
                nextBtn.setEnabled(canAdvanceFrom(0, 0));
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Auto-detect Echo VR from Oculus installation"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(detectBtn);
    }

    private SpecialTextfield makeEditablePathField(int cx, int y, JLabel indicator) {
        String saved = Helpers.loadInstallPath();
        String p = (saved != null && !saved.isEmpty()) ? saved : "C:/EchoVR";
        String resolved = Helpers.resolveEchoInstallRoot(p);
        wizardState.setInstallPath(resolved);

        SpecialTextfield tf = new SpecialTextfield();
        tf.specialTextfield(440, 24, (cx - 440) / 2, y, 12);
        tf.setText(resolved);

        updatePathStatus(indicator);
        wireClearOnClick(indicator, tf, () -> commitPathField());

        tf.addActionListener(e -> commitPathField());
        tf.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { commitPathField(); }
        });

        return tf;
    }

    private void commitPathField() {
        if (pathField == null) return;
        String resolved = Helpers.resolveEchoInstallRoot(pathField.getText().trim());
        wizardState.setInstallPath(resolved);
        String root = wizardState.getInstallPath();
        Helpers.saveInstallPath(root);
        pathField.setText(root);
        if (pathIndicator != null) updatePathStatus(pathIndicator);
        nextBtn.setEnabled(canAdvanceFrom(0, 0));
    }

    private void updatePathStatus(JLabel indicator) {
        String path = wizardState.getInstallPath();
        boolean valid = Helpers.hasEchoInstall(path);
        indicator.setText("");
        if (valid) {
            indicator.setIcon(markIcon(true, new Color(80, 255, 0), 26));
            indicator.setToolTipText("Echo VR found at this path");
        } else {
            indicator.setIcon(markIcon(false, new Color(255, 80, 80), 22));
            indicator.setToolTipText("Echo VR not found at this path");
        }
    }

    private void buildStep1(int cx, int sub) {
        if (sub == 0) {
            buildStep1Download(cx);
        } else {
            buildStep1Extract(cx);
            startExtraction();
        }
    }

    private void buildStep1Download(int cx) {
        JLabel h = makeHeader("Downloading Echo VR update");
        h.setBounds((cx - 450) / 2, 4, 450, 55); contentPanel.add(h);

        updateProgressLabel = new SpecialLabel("0.00%", 14);
        updateProgressLabel.setBackground(new Color(255, 255, 255, 200));
        updateProgressLabel.setForeground(Color.BLACK);
        updateProgressLabel.setSize(440, updateProgressLabel.getHeight());
        updateProgressLabel.setLocation((cx - 440) / 2, 70);
        contentPanel.add(updateProgressLabel);

        dlButton = new SpecialButton("Start Update",
            "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        dlButton.setLocation((cx - dlButton.getWidth()) / 2, 110);
        dlButton.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (stepInProgress) {
                    if (!confirmAbortDownload()) return;
                    if (downloader != null) downloader.cancelDownload();
                    stepInProgress = false;
                    progressAnimator.stop();
                    dlButton.changeText("Start Update");
                    updateProgressLabel.setText("0.00%");
                    updateStatusText(1, 0);
                    nextBtn.setEnabled(false);
                    progPanel.repaint();
                    statusBarBox.repaint();
                } else {
                    triggerUpdateDownload();
                }
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Download the latest Echo VR game update"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(dlButton);
    }

    private void triggerUpdateDownload() {
        stepInProgress = true;
        stepCompleted = false;
        progressAnimator.start();
        dlButton.changeText("Cancel");
        nextBtn.setEnabled(false);
        updateStatusText(1, 0);

        if (downloader != null) { downloader.cancelDownload(); pause(1); }

        final String tempDir = System.getProperty("java.io.tmpdir") + "/echo_update/";
        new Thread(() -> {
            downloader = new Downloader();
            downloader.setOnCompleteListener(() -> SwingUtilities.invokeLater(() -> {
                File f = new File(tempDir + "bullet_patch.zip");
                if (f.exists() && f.length() > 0) {
                    updateProgressLabel.setText("100.00%");
                    stepInProgress = false;
                    progressAnimator.stop();
                    progPanel.repaint();
                    statusBarBox.repaint();
                    advance();
                } else {
                    stepInProgress = false;
                    progressAnimator.stop();
                    progPanel.repaint();
                    statusBarBox.repaint();
                    dlButton.changeText("Retry");
                    dlButton.setEnabled(true);
                    updateProgressLabel.setText("Download failed");
                    updateStatusText(1, 0);
                }
            }));
            downloader.startDownload(
                "https://files.echovr.de/updates/bullet_patch.zip",
                tempDir, "bullet_patch.zip",
                updateProgressLabel, FramePCUpdate.this, frameMain,
                1, false, -1, true);
        }).start();
    }

    private void buildStep1Extract(int cx) {
        JLabel h = makeHeader("Extracting Echo VR update");
        h.setBounds((cx - 450) / 2, 4, 450, 55); contentPanel.add(h);

        updateProgressLabel = new SpecialLabel("Extracting...", 14);
        updateProgressLabel.setBackground(new Color(255, 255, 255, 200));
        updateProgressLabel.setForeground(Color.BLACK);
        updateProgressLabel.setSize(440, updateProgressLabel.getHeight());
        updateProgressLabel.setLocation((cx - 440) / 2, 70);
        contentPanel.add(updateProgressLabel);
    }

    private void startExtraction() {
        stepInProgress = true;
        progressAnimator.start();
        updateStatusText(1, 1);
        nextBtn.setEnabled(false);

        final String tempDir = System.getProperty("java.io.tmpdir") + "/echo_update/";
        final File zipFile = new File(tempDir + "bullet_patch.zip");

        new Thread(() -> {
            try {
                if (!zipFile.exists()) {
                    SwingUtilities.invokeLater(() -> {
                        new ErrorDialog().errorDialog(FramePCUpdate.this,
                            "Download missing",
                            "The update file was not found.\nPlease try downloading again.", 0);
                        showStep(1, 0);
                        resetAfterError(dlButton);
                        dlButton.changeText("Retry");
                    });
                    return;
                }
                String binPath = wizardState.getBinPath();
                UnzipFile.unzip(zipFile.getAbsolutePath(), binPath);
                SwingUtilities.invokeLater(() -> {
                    updateProgressLabel.setText("Update applied!");
                    stepInProgress = false;
                    stepCompleted = true;
                    progressAnimator.stop();
                    nextBtn.setEnabled(true);
                    dlProgressLabel.setText("Update applied!");
                    progPanel.repaint();
                    statusBarBox.repaint();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    new ErrorDialog().errorDialog(FramePCUpdate.this,
                        "Please close Echo VR first",
                        "Please close Echo VR before updating.\n\n" + e.getMessage(), 0);
                    showStep(1, 0);
                    resetAfterError(dlButton);
                    dlButton.changeText("Retry");
                });
            }
        }).start();
    }

    private void buildStep2(int cx) {
        JLabel doneLabel = new JLabel("Update applied!", SwingConstants.CENTER);
        doneLabel.setBounds(0, 20, cx, 40);
        doneLabel.setForeground(new Color(0, 255, 0));
        doneLabel.setFont(new Font("Arial", Font.BOLD, 24));
        contentPanel.add(doneLabel);

        JLabel subtitle = new JLabel("Echo VR has been updated.", SwingConstants.CENTER);
        subtitle.setBounds(0, 70, cx, 24);
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
        contentPanel.add(subtitle);

        SpecialButton openBtn = new SpecialButton("Open Install Folder",
            "button_up.png", "button_down.png", "button_highlighted.png", 18);
        openBtn.setLocation((cx - openBtn.getWidth()) / 2, 115);
        openBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                String installPath = wizardState.getInstallPath();
                if (installPath == null || installPath.isEmpty()) {
                    new ErrorDialog().errorDialog(FramePCUpdate.this, "No Install Path",
                        "Echo VR is not installed. Please download and install Echo VR first.", 0);
                    return;
                }
                Helpers.openFolder(wizardState.getBinPath());
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Open the Echo VR bin/win10 folder"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(openBtn);

        stepCompleted = true;
        nextBtn.setEnabled(true);
    }
}
