package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static org.junit.jupiter.api.Assertions.*;

public class UserTypeDialogTest {

    private UserTypeDialog dialog;

    @BeforeEach
    void setUp() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        dialog = new UserTypeDialog(null);
    }

    @Test
    void testDialogCreatesWizardState() {
        assertNotNull(dialog.getWizardState());
    }

    @Test
    void testOwnerButtonSetsOwnerType() {
        SpecialButton ownerButton = findButtonByText("I own Echo on Meta");
        assertNotNull(ownerButton, "Owner button not found");
        dispatchClick(ownerButton);
        assertEquals(WizardState.UserType.OWNER, dialog.getWizardState().getUserType());
    }

    @Test
    void testNewPlayerButtonSetsNewPlayerType() {
        SpecialButton newPlayerButton = findButtonByText("I'm a new player");
        assertNotNull(newPlayerButton, "New player button not found");
        dispatchClick(newPlayerButton);
        assertEquals(WizardState.UserType.NEW_PLAYER, dialog.getWizardState().getUserType());
    }

    @Test
    void testGetWizardStateReturnsState() {
        WizardState state = dialog.getWizardState();
        assertNotNull(state);
        assertSame(state, dialog.getWizardState());
    }

    @Test
    void testDialogHasTwoButtons() {
        int count = countSpecialButtons(dialog.getContentPane());
        assertTrue(count >= 2, "Expected at least 2 SpecialButtons, found " + count);
    }

    private SpecialButton findButtonByText(String text) {
        return findButtonByText(dialog.getContentPane(), text);
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
