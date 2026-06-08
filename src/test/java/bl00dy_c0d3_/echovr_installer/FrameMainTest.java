package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FrameMainTest {

    private FrameMain frame;

    @BeforeEach
    void setUp() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        frame = new FrameMain();
    }

    @Test
    void testWizardButtonHasCorrectLabel() {
        // The "Install Echo VR" button is the wizard entry point
        SpecialButton btn = findButtonByText("Install Echo VR");
        assertNotNull(btn, "Wizard button 'Install Echo VR' should exist");
    }

    @Test
    void testUpdateEchoButtonVisible() {
        // "Update Echo (PC)" button should remain visible
        SpecialButton btn = findButtonByText("Update Echo (PC)");
        assertNotNull(btn, "Update Echo button should exist");
        assertTrue(btn.isVisible(), "Update Echo (PC) button should be visible");
    }

    @Test
    void testNoLicenceFrameHidden() {
        // rahmen1 is the JPanel containing "No licence patch" AND "Steam Patch (Revive)"
        // It should be hidden
        JPanel rahmen1 = findPanelContainingBoth("No licence patch", "Steam Patch (Revive)");
        assertNotNull(rahmen1, "Rahmen1 panel should exist");
        assertFalse(rahmen1.isVisible(), "Rahmen1 (PC no licence + Steam) should be hidden");
    }

    @Test
    void testQuestButtonsVisible() {
        // "Quest Install Echo" button should now be visible (opens FrameGuidanceQuest)
        SpecialButton questBtn = findButtonByText("Quest Install Echo");
        assertNotNull(questBtn, "Quest Install Echo button should exist");
        assertTrue(questBtn.isVisible(), "Quest Install Echo button should be visible");
    }

    @Test
    void testWizardEntryExists() {
        // The wizard entry button ("Install Echo VR") should exist and be visible
        SpecialButton btn = findButtonByText("Install Echo VR");
        assertNotNull(btn, "Wizard entry button should exist");
        assertTrue(btn.isVisible(), "Wizard entry button should be visible");
    }

    @Test
    void testGetQuestLogsButtonHidden() {
        // "Get Quest Logs" button should be hidden
        SpecialButton btn = findButtonByText("Get Quest Logs");
        assertNotNull(btn, "Get Quest Logs button should exist");
        assertFalse(btn.isVisible(), "Get Quest Logs button should be hidden");
    }

    @Test
    void testDeleteCacheButtonHidden() {
        // "Delete cache" button should be hidden
        SpecialButton btn = findButtonByText("Delete cache");
        assertNotNull(btn, "Delete cache button should exist");
        assertFalse(btn.isVisible(), "Delete cache button should be hidden");
    }

    // --- Helper methods ---

    private SpecialButton findButtonByText(String text) {
        return findButtonByText(frame.getContentPane(), text);
    }

    private SpecialButton findButtonByText(Container container, String text) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof SpecialButton) {
                if (buttonTextEquals((SpecialButton) comp, text)) {
                    return (SpecialButton) comp;
                }
            }
            if (comp instanceof Container) {
                SpecialButton found = findButtonByText((Container) comp, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean buttonTextEquals(SpecialButton button, String expectedText) {
        for (Component comp : button.getComponents()) {
            if (comp instanceof JLabel) {
                String labelText = ((JLabel) comp).getText();
                if (labelText != null) {
                    if (labelText.startsWith("<html>")) {
                        labelText = labelText.replaceAll("<[^>]+>", "").trim();
                    }
                    return expectedText.equals(labelText);
                }
            }
        }
        return false;
    }

    private JPanel findPanelContainingBoth(String text1, String text2) {
        return findPanelContainingBoth(frame.getContentPane(), text1, text2);
    }

    private JPanel findPanelContainingBoth(Container container, String text1, String text2) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                boolean hasText1 = findButtonByText(panel, text1) != null;
                boolean hasText2 = findButtonByText(panel, text2) != null;
                if (hasText1 && hasText2) {
                    return panel;
                }
            }
            if (comp instanceof Container) {
                JPanel found = findPanelContainingBoth((Container) comp, text1, text2);
                if (found != null) return found;
            }
        }
        return null;
    }
}
