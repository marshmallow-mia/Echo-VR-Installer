package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UserTypeDialog extends JDialog {

    private WizardState wizardState = new WizardState();

    public UserTypeDialog(FrameMain parent) {
        super(parent, true);
        initComponents();
        if (parent != null) {
            int x = parent.getX() + (parent.getWidth() - getWidth()) / 2;
            int y = parent.getY() + (parent.getHeight() - getHeight()) / 2;
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setModal(true);

        Background back = new Background("Echox720.png");
        back.setLayout(null);
        setContentPane(back);

        SpecialButton ownerButton = new SpecialButton(
                "I own Echo on Meta",
                "button_up.png", "button_down.png", "button_highlighted.png", 20
        );
        ownerButton.setLocation(150, 80);
        ownerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                wizardState.setUserType(WizardState.UserType.OWNER);
                dispose();
            }
        });
        back.add(ownerButton);

        SpecialButton newPlayerButton = new SpecialButton(
                "I'm a new player",
                "button_up.png", "button_down.png", "button_highlighted.png", 20
        );
        newPlayerButton.setLocation(150, 150);
        newPlayerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                wizardState.setUserType(WizardState.UserType.NEW_PLAYER);
                dispose();
            }
        });
        back.add(newPlayerButton);

        setSize(500, 300);
    }

    public WizardState getWizardState() {
        return wizardState;
    }
}
