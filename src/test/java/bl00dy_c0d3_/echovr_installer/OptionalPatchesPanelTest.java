package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static org.junit.jupiter.api.Assertions.*;

public class OptionalPatchesPanelTest {

    private OptionalPatchesPanel panel;

    @BeforeEach
    void setUp() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        WizardState wizardState = new WizardState();
        panel = new OptionalPatchesPanel(null, wizardState);
        // Make the dialog displayable so dispose() changes isDisplayable state
        panel.pack();
    }

    @Test
    void testPanelHasNoLicenceButton() {
        SpecialButton button = findButtonByText("No Licence Patch");
        assertNotNull(button, "No Licence Patch button not found");
    }

    @Test
    void testPanelHasSteamPatchButton() {
        SpecialButton button = findButtonByText("Steam Patch (Revive)");
        assertNotNull(button, "Steam Patch (Revive) button not found");
    }

    @Test
    void testPanelHasSkipButton() {
        SpecialButton button = findButtonByText("Skip / I'm done");
        assertNotNull(button, "Skip / I'm done button not found");
    }

    @Test
    void testClickSkipDisposes() {
        SpecialButton skipButton = findButtonByText("Skip / I'm done");
        assertNotNull(skipButton);
        assertTrue(panel.isDisplayable(), "Panel should be displayable before click");
        dispatchClick(skipButton);
        assertFalse(panel.isDisplayable(), "Panel should be disposed after clicking Skip");
    }

    @Test
    void testPanelHasThreeButtons() {
        int count = countSpecialButtons(panel.getContentPane());
        assertTrue(count >= 3, "Expected at least 3 SpecialButtons, found " + count);
    }

    @Test
    void testGetWizardStateReturnsState() {
        WizardState state = panel.getWizardState();
        assertNotNull(state);
    }

    private SpecialButton findButtonByText(String text) {
        return findButtonByText(panel.getContentPane(), text);
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
                if (labelText != null && labelText.startsWith("<html>")) {
                    labelText = labelText.replaceAll("<[^>]+>", "").trim();
                }
                return expectedText.equals(labelText);
            }
        }
        return false;
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

    private int countSpecialButtons(Container container) {
        int count = 0;
        for (Component comp : container.getComponents()) {
            if (comp instanceof SpecialButton) {
                count++;
            }
            if (comp instanceof Container) {
                count += countSpecialButtons((Container) comp);
            }
        }
        return count;
    }
}
