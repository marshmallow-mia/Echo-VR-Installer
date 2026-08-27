package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

/**
 * Standalone Quest update wizard -- the Quest counterpart to {@link FramePCUpdate}.
 *
 * <p>Mirrors the PC wizard's three steps, with "Connect" standing in for "Path": the
 * on-device location is fixed, so there is nothing for the user to choose. The version
 * gate runs as a pre-flight when the Update step opens rather than as its own step, so
 * the shape stays identical to PC.
 */
public class FrameQuestUpdate extends BaseWizard {

    private SpecialLabel updateProgressLabel;
    private SpecialButton updateBtn;

    /** Last adb status from the Connect step: 0 = ready, 1 = unauthorized, -1 = none. */
    private int lastConnectionStatus = -1;

    /** Manifest for the pending update; non-null once the version check has passed. */
    private UpdateManifest manifest;

    public FrameQuestUpdate(FrameMain frameMain) {
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
    protected String getBackgroundImage() { return "Echo2.jpg"; }

    @Override
    protected int getWindowWidth() { return FW; }

    @Override
    protected int getWindowHeight() { return FH; }

    @Override
    protected String getWindowTitle() { return VERSION_TITLE; }

    @Override
    protected int getStepCount() { return 3; }

    @Override
    protected String getChipLabel(int step) {
        String[] chips = {"Connect", "Update", "Done"};
        return step >= 0 && step < chips.length ? chips[step] : "";
    }

    @Override
    protected int getSubstepCount(int s) {
        return 1;
    }

    @Override
    protected String getSubstepName(int s, int sub) {
        switch (s) {
            case 0: return "Connect to Quest";
            case 1: return "Update";
            case 2: return "All Done";
        }
        return "";
    }

    @Override
    protected boolean canAdvanceFrom(int step, int sub) {
        if (step == 0) {
            return lastConnectionStatus == 0;
        }
        if (step == 1) {
            return stepCompleted;
        }
        return true;
    }

    @Override
    protected void updateStatusText(int step, int sub) {
        switch (step) {
            case 0: dlProgressLabel.setText("Connect your Quest"); break;
            case 1: dlProgressLabel.setText(stepInProgress ? "Updating..." : "Ready to update"); break;
            case 2: dlProgressLabel.setText("Echo VR Quest update complete!"); break;
        }
    }

    @Override
    protected void buildContent(int step, int sub, int cx) {
        switch (step) {
            case 0: buildStep0(cx); break;
            case 1: buildStep1(cx); break;
            case 2: buildStep2(cx); break;
        }
    }

    // === Step 0: Connect ===

    private void buildStep0(int cx) {
        JLabel h = makeHeader("Connect your Quest");
        h.setBounds((cx - 450) / 2, 5, 450, 55); contentPanel.add(h);

        JLabel questStatus = new JLabel("Checking Quest connection...", SwingConstants.CENTER);
        questStatus.setBounds(0, 68, cx, 24);
        questStatus.setFont(new Font("Arial", Font.BOLD, 14));
        questStatus.setForeground(Color.LIGHT_GRAY);
        questStatus.setIconTextGap(6);
        contentPanel.add(questStatus);

        SpecialButton connectBtn = new SpecialButton("Connect to Quest",
            "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        connectBtn.setLocation((cx - connectBtn.getWidth()) / 2, 106);
        connectBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { checkConnection(connectBtn, questStatus, true); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Check that your Quest is connected and has allowed this PC"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        contentPanel.add(connectBtn);

        // Auto-check on entry, silently — the user hasn't asked for a popup yet.
        checkConnection(connectBtn, questStatus, false);
    }

    private void checkConnection(SpecialButton connectBtn, JLabel statusLbl, boolean interactive) {
        nextBtn.setEnabled(false);
        refreshQuestConnection(connectBtn, statusLbl, interactive, status -> {
            lastConnectionStatus = status;
            nextBtn.setEnabled(status == 0);
        });
    }

    // === Step 1: Update ===

    private void buildStep1(int cx) {
        JLabel h = makeHeader("Update Echo VR on your Quest");
        h.setBounds((cx - 450) / 2, 4, 450, 55); contentPanel.add(h);

        updateProgressLabel = new SpecialLabel("Checking your Echo VR version...", 14);
        updateProgressLabel.setBackground(new Color(255, 255, 255, 200));
        updateProgressLabel.setForeground(Color.BLACK);
        updateProgressLabel.setSize(440, updateProgressLabel.getHeight());
        updateProgressLabel.setLocation((cx - 440) / 2, 70);
        contentPanel.add(updateProgressLabel);

        updateBtn = new SpecialButton("Start Update",
            "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        updateBtn.setLocation((cx - updateBtn.getWidth()) / 2, 110);
        updateBtn.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (!updateBtn.isEnabled()) return;
                if (stepInProgress) {
                    if (!confirmAbortDownload()) return;
                    QuestUpdateService.cancelUpdate();
                    stepInProgress = false;
                    progressAnimator.stop();
                    updateBtn.changeText("Start Update");
                    updateProgressLabel.setText("Cancelling after the current file...");
                    updateStatusText(1, 0);
                    nextBtn.setEnabled(false);
                    progPanel.repaint();
                    statusBarBox.repaint();
                } else {
                    triggerUpdate();
                }
            }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("Copy the latest Echo VR update to your Quest"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        updateBtn.setEnabled(false);
        contentPanel.add(updateBtn);

        // The version gate runs on entry, so the user never presses a button that can't work.
        nextBtn.setEnabled(false);
        QuestUpdateService.checkVersion(QUEST_MANIFEST_URL, updateProgressLabel,
                FrameQuestUpdate.this, this::onVersionChecked);
    }

    private void onVersionChecked(QuestUpdateService.Status status) {
        if (status.isOk()) {
            manifest = status.manifest;
            updateProgressLabel.setText("Ready to update");
            updateBtn.setEnabled(true);
            return;
        }

        updateBtn.setEnabled(false);
        switch (status.result) {
            case NO_DEVICE -> {
                updateProgressLabel.setText("Quest disconnected");
                new ErrorDialog().errorDialog(this, "No Quest detected",
                    "<html><center>Couldn't find your Quest. Connect it by USB and make sure<br>"
                    + "Developer Mode is enabled.</center></html>", 1);
            }
            case MANIFEST_ERROR -> {
                updateProgressLabel.setText("Could not check for updates");
                new ErrorDialog().errorDialog(this, "Update check failed", status.detail, 0);
            }
            // NOT_INSTALLED and MISMATCH both mean the same thing to the user: reinstall.
            default -> {
                updateProgressLabel.setText("Version mismatch — reinstall required");
                showMismatchDialog(status.detail);
            }
        }
    }

    /**
     * A Quest update can only be applied on top of the exact APK the manifest was built
     * for, so a mismatch is a dead end here — offer the reinstall wizard instead.
     */
    private void showMismatchDialog(String detail) {
        Object[] options = {"Reinstall Echo VR", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "<html><center>" + detail + "<br><br>"
            + "Reinstall Echo VR on your Quest to continue.</center></html>",
            "Echo VR version mismatch",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            // Dispose first so the install wizard isn't stacked behind this modal, and defer
            // its construction until this dialog's modal pump has unwound.
            dispose();
            SwingUtilities.invokeLater(() -> new FrameGuidanceQuest(frameMain));
        }
    }

    private void triggerUpdate() {
        stepInProgress = true;
        stepCompleted = false;
        progressAnimator.start();
        updateBtn.changeText("Cancel");
        nextBtn.setEnabled(false);
        updateStatusText(1, 0);

        QuestUpdateService.applyUpdates(
            manifest,
            updateProgressLabel,
            FrameQuestUpdate.this,
            frameMain,
            () -> SwingUtilities.invokeLater(() -> {
                updateProgressLabel.setText("Update applied!");
                stepInProgress = false;
                stepCompleted = true;
                progressAnimator.stop();
                nextBtn.setEnabled(true);
                updateBtn.changeText("Start Update");
                dlProgressLabel.setText("Update applied!");
                progPanel.repaint();
                statusBarBox.repaint();
                advance();
            }),
            () -> SwingUtilities.invokeLater(() -> {
                // Abort or cancel: return the step to a state the user can retry from.
                stepInProgress = false;
                stepCompleted = false;
                progressAnimator.stop();
                updateBtn.changeText("Start Update");
                updateBtn.setEnabled(true);
                updateProgressLabel.setText("Update did not finish");
                nextBtn.setEnabled(false);
                updateStatusText(1, 0);
                progPanel.repaint();
                statusBarBox.repaint();
            }));
    }

    // === Step 2: Done ===

    private void buildStep2(int cx) {
        JLabel doneLabel = new JLabel("Update applied!", SwingConstants.CENTER);
        doneLabel.setBounds(0, 55, cx, 40);
        doneLabel.setForeground(new Color(0, 255, 0));
        doneLabel.setFont(new Font("Arial", Font.BOLD, 24));
        contentPanel.add(doneLabel);

        JLabel subtitle = new JLabel("Echo VR on your Quest has been updated.", SwingConstants.CENTER);
        subtitle.setBounds(0, 105, cx, 24);
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
        contentPanel.add(subtitle);

        stepCompleted = true;
        nextBtn.setEnabled(true);
    }
}
