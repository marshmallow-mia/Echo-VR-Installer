package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Asks which attached device is the Quest, when adb reports several and nothing in the
 * device table distinguishes them.
 *
 * <p>Structured exactly like {@link UserTypeDialog}: modal, a {@link Background} content
 * pane, one {@link SpecialButton} per choice, and the answer read back with
 * {@link #getChosenSerial()} after the dialog closes. The only difference is that the
 * buttons are laid out in a loop, since the number of devices is not known up front.
 *
 * <p>Buttons are labelled by model rather than serial -- "Quest 3 -- 1WMHH8150XX07M" -- because
 * a bare list of serials is unanswerable for someone who cannot tell which is their headset.
 */
public class DevicePickerDialog extends JDialog {

    private static final int BUTTON_X = 150;
    private static final int FIRST_BUTTON_Y = 96;
    private static final int BUTTON_SPACING = 70;
    private static final int DIALOG_WIDTH = 500;

    /** adb tops out well below this, but a runaway table must not produce an unusable dialog. */
    private static final int MAX_BUTTONS = 5;

    private String chosenSerial;

    public DevicePickerDialog(Dialog parent, List<AdbDevices.Device> devices) {
        super(parent, true);
        initComponents(devices);
        if (parent != null) {
            int x = parent.getX() + (parent.getWidth() - getWidth()) / 2;
            int y = parent.getY() + (parent.getHeight() - getHeight()) / 2;
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }
    }

    private void initComponents(List<AdbDevices.Device> devices) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setModal(true);
        setTitle("Which one is your Quest?");

        int shown = Math.min(devices.size(), MAX_BUTTONS);
        int height = FIRST_BUTTON_Y + shown * BUTTON_SPACING + 40;

        Background back = new Background("Echox720.png");
        back.setLayout(null);
        setContentPane(back);

        JLabel prompt = new JLabel(
                "<html><center>More than one device is plugged in.<br>"
                + "Which one is your Quest?</center></html>", SwingConstants.CENTER);
        prompt.setForeground(Color.WHITE);
        prompt.setBounds(40, 24, DIALOG_WIDTH - 80, 56);
        back.add(prompt);

        for (int i = 0; i < shown; i++) {
            AdbDevices.Device device = devices.get(i);
            SpecialButton button = new SpecialButton(
                    device.label(),
                    "button_up.png", "button_down.png", "button_highlighted.png", 16
            );
            button.setLocation(BUTTON_X, FIRST_BUTTON_Y + i * BUTTON_SPACING);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    chosenSerial = device.serial();
                    dispose();
                }
            });
            back.add(button);
        }

        setSize(DIALOG_WIDTH, height);
    }

    /** The serial the user picked, or null if they closed the dialog without choosing. */
    public String getChosenSerial() {
        return chosenSerial;
    }
}
