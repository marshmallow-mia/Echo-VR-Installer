package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OptionalPatchesPanel extends JDialog {

    private final WizardState wizardState;

    public OptionalPatchesPanel(JDialog parent, WizardState wizardState) {
        super(parent, true);
        this.wizardState = wizardState;
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

        SpecialButton noLicenceButton = new SpecialButton(
                "No Licence Patch",
                "button_up.png", "button_down.png", "button_highlighted.png", 18
        );
        noLicenceButton.setLocation(150, 60);
        noLicenceButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                new FramePCPatcher();
                dispose();
            }
        });
        back.add(noLicenceButton);

        SpecialButton steamPatchButton = new SpecialButton(
                "Steam Patch (Revive)",
                "button_up.png", "button_down.png", "button_highlighted.png", 18
        );
        steamPatchButton.setLocation(150, 130);
        steamPatchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                new FrameSteamPatcher(null);
                dispose();
            }
        });
        back.add(steamPatchButton);

        SpecialButton skipButton = new SpecialButton(
                "Skip / I'm done",
                "button_up.png", "button_down.png", "button_highlighted.png", 18
        );
        skipButton.setLocation(150, 200);
        skipButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                dispose();
            }
        });
        back.add(skipButton);

        setSize(500, 350);
    }

    public WizardState getWizardState() {
        return wizardState;
    }
}
