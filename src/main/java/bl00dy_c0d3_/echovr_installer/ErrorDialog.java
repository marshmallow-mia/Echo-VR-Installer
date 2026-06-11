package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ErrorDialog {
    static Background back; // Declare back as an instance variable
    JDialog errorDialog = new JDialog();

    //the hyperlink int makes sure to add a hyperlink if needed and also selects which
    public void errorDialog(JDialog frame, String errorTitle_st, String errorText_st, int hyperlink){
        errorDialog.setTitle(errorTitle_st);
        errorDialog.setSize(800,200);
        int XPos1 = ( frame.getX() + frame.getWidth() / 2 - errorDialog.getWidth()/2) ;
        int YPos1 = ( frame.getY() + frame.getHeight() / 2 - errorDialog.getHeight()/2) ;
        errorDialog.setLocation(XPos1, YPos1);
        back = new Background("Marcelus.png");
        back.setLayout(null);
        errorDialog.setContentPane(back);
        errorDialog.setModal(true);


        // Centered error text near the top.
        SpecialLabel errorText = new SpecialLabel(errorText_st, 14);
        errorText.setLocation((errorDialog.getWidth() - errorText.getWidth()) / 2, 30);
        back.add(errorText);

        int nextY = errorText.getY() + errorText.getHeight() + 12;

        // Optional, on-theme help link (white, underlined, transparent — sits cleanly on the
        // dark background rather than as a blue-on-white box).
        if (hyperlink == 1) {
            nextY = addHelpLink("How to enable Developer Mode on your Quest",
                "https://learn.adafruit.com/sideloading-on-oculus-quest/enable-developer-mode", nextY);
        } else if (hyperlink == 2) {
            nextY = addHelpLink("Open the Java Runtime download page",
                "https://www.java.com/de/download/manual.jsp", nextY);
        } else if (hyperlink == 3) {
            nextY = addHelpLink("How to allow USB debugging on your Quest",
                "https://learn.adafruit.com/sideloading-on-oculus-quest/enable-developer-mode", nextY);
        }

        SpecialButton btn_errorClose = new SpecialButton("Close", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 14);
        btn_errorClose.setLocation((errorDialog.getWidth() - btn_errorClose.getWidth()) / 2, nextY + 6);
        btn_errorClose.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                errorDialog.dispose();
            }
        });
        back.add(btn_errorClose);

        errorDialog.setVisible(true);
    }

    /** Adds a centered, underlined white help link at y; returns the y below it. */
    private int addHelpLink(String text, String url, int y) {
        SpecialHyperlink link = new SpecialHyperlink(0, y, "<html><u>" + text + "</u></html>", url, 14);
        link.setForeground(Color.WHITE);
        link.setSize(link.getPreferredSize());
        link.setLocation((errorDialog.getWidth() - link.getWidth()) / 2, y);
        back.add(link);
        return y + link.getHeight();
    }
}

