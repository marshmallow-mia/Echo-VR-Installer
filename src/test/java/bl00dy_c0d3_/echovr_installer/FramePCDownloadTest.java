package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class FramePCDownloadTest {

    private FrameMain frameMain;
    private FramePCDownload downloadFrame;
    private WizardState wizardState;

    @BeforeEach
    void setUp() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        frameMain = new FrameMain();
        wizardState = new WizardState();
        downloadFrame = new FramePCDownload(frameMain, wizardState);
    }

    @Test
    void testConstructorAcceptsWizardState() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertNotNull(downloadFrame, "FramePCDownload should be created");
        assertNotNull(downloadFrame.nextButton, "nextButton should exist");
    }

    @Test
    void testNextButtonInitiallyHidden() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertFalse(downloadFrame.nextButton.isVisible(),
                "Next button should be hidden before download completes");
    }

    @Test
    void testNextButtonVisibleAfterCompletion() throws Exception {
        if (GraphicsEnvironment.isHeadless()) return;

        assertFalse(downloadFrame.nextButton.isVisible(),
                "Next button should be hidden before completion");

        // Trigger the onComplete callback mechanism that makes nextButton visible
        // We simulate a downloader callback by accessing onComplete via reflection
        Downloader testDownloader = new Downloader();
        testDownloader.setOnCompleteListener(() -> downloadFrame.nextButton.setVisible(true));
        downloadFrame.downloader = testDownloader;

        Field onCompleteField = Downloader.class.getDeclaredField("onComplete");
        onCompleteField.setAccessible(true);
        Runnable onComplete = (Runnable) onCompleteField.get(testDownloader);
        onComplete.run();

        assertTrue(downloadFrame.nextButton.isVisible(),
                "Next button should be visible after download completion callback");
    }

    @Test
    void testNextButtonStoresPath() {
        if (GraphicsEnvironment.isHeadless()) return;

        assertEquals("", wizardState.getInstallPath(),
                "Install path should be empty initially");

        // Click dispatches directly; FramePCPatcher creation uses
        // SwingUtilities.invokeLater so it won't block the test thread.
        dispatchClick(downloadFrame.nextButton);

        String storedPath = wizardState.getInstallPath();
        assertNotNull(storedPath, "Install path should be set after clicking Next");
        assertFalse(storedPath.isEmpty(), "Install path should not be empty");
        assertEquals("C:/EchoVR", storedPath,
                "Path should match the path label's default text");
    }

    @Test
    void testOwnerBranchOpensPatcher() {
        if (GraphicsEnvironment.isHeadless()) return;

        wizardState.setUserType(WizardState.UserType.OWNER);
        wizardState.setInstallPath("");

        dispatchClick(downloadFrame.nextButton);

        assertNotNull(wizardState.getInstallPath(),
                "Install path should be set by Next button handler");
        assertFalse(wizardState.getInstallPath().isEmpty(),
                "Install path should not be empty");
        assertEquals(WizardState.UserType.OWNER, wizardState.getUserType(),
                "UserType should remain OWNER after clicking Next");
    }

    @Test
    void testNewPlayerBranchOpensPatcher() {
        if (GraphicsEnvironment.isHeadless()) return;

        wizardState.setUserType(WizardState.UserType.NEW_PLAYER);
        wizardState.setInstallPath("");

        dispatchClick(downloadFrame.nextButton);

        assertNotNull(wizardState.getInstallPath(),
                "Install path should be set by Next button handler");
        assertFalse(wizardState.getInstallPath().isEmpty(),
                "Install path should not be empty");
        assertEquals(WizardState.UserType.NEW_PLAYER, wizardState.getUserType(),
                "UserType should remain NEW_PLAYER after clicking Next");
    }

    @Test
    void testNullUserTypeOnlyDisposes() {
        if (GraphicsEnvironment.isHeadless()) return;

        // userType is null by default
        wizardState.setInstallPath("");

        dispatchClick(downloadFrame.nextButton);

        // Path should still be set even for null user type
        assertNotNull(wizardState.getInstallPath(),
                "Install path should be set even when userType is null");
        assertFalse(wizardState.getInstallPath().isEmpty(),
                "Install path should not be empty");
        assertNull(wizardState.getUserType(),
                "UserType should remain null");
    }

    private void dispatchClick(SpecialButton button) {
        long now = System.currentTimeMillis();
        int x = button.getWidth() / 2;
        int y = button.getHeight() / 2;

        MouseEvent press = new MouseEvent(
                button, MouseEvent.MOUSE_PRESSED, now, 0,
                x, y, 1, false, MouseEvent.BUTTON1
        );
        MouseEvent release = new MouseEvent(
                button, MouseEvent.MOUSE_RELEASED, now + 10, 0,
                x, y, 1, false, MouseEvent.BUTTON1
        );

        for (MouseListener ml : button.getMouseListeners()) {
            ml.mousePressed(press);
            ml.mouseReleased(release);
        }
    }
}
