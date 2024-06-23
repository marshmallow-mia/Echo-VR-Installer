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


        SpecialLabel errorText = new SpecialLabel(errorText_st, 14);
        int XPos2 = ( ( errorDialog.getWidth() - errorText.getWidth() )   /2) - 4 ;
        // TODO BAD WAY TO CORRECT THE POSITION
        errorText.setLocation(XPos2, 35);
        back.add(errorText);

        SpecialButton btn_errorClose = new SpecialButton("Close", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 14);
        int XPos3 = ( ( errorDialog.getWidth() - btn_errorClose.getWidth() )   /2) ;
        int YPos3 = ( errorText.getHeight() + errorText.getY() + 30 );
        btn_errorClose.setLocation(XPos3, YPos3);
        btn_errorClose.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                errorDialog.dispose();
            }
        });
        back.add(btn_errorClose);

        if (hyperlink == 1) {
            debugMode();
        }
        else if (hyperlink == 2) {
            javaRuntimeLink();
        }
        errorDialog.setVisible(true);



    }

    public void debugMode(){
        SpecialHyperlink hyperlinkPC = new SpecialHyperlink(115, 70, "Click on me for infos on how to enable the debugmode", "https://learn.adafruit.com/sideloading-on-oculus-quest/enable-developer-mode", 16);
        hyperlinkPC.setOpaque(true); // Make the label opaque
        hyperlinkPC.setForeground(Color.BLUE); // Set text color to white
        hyperlinkPC.setBackground(Color.WHITE);

        back.add(hyperlinkPC);
    }

    public void javaRuntimeLink(){
        SpecialHyperlink hyperlinkPC = new SpecialHyperlink(112, 70, "Click on me to go to the Java Runtime Download Page", "https://www.java.com/de/download/manual.jsp", 16);
        hyperlinkPC.setOpaque(true); // Make the label opaque
        hyperlinkPC.setForeground(Color.BLUE); // Set text color to white
        hyperlinkPC.setBackground(Color.WHITE);

        back.add(hyperlinkPC);
    }
}
