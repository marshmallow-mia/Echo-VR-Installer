package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class SpecialLabel extends JLabel {

    // Constructor
    public SpecialLabel(String labelText, int textSize) {
        //Schriftart laden...
        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            font = font.deriveFont(Font.PLAIN, textSize);
        }
        catch (Exception e) {e.printStackTrace();}

        this.setText(labelText);
        this.setFont(font);
        this.setForeground(Color.WHITE);
        this.setSize(this.getPreferredSize().width + 10, this.getPreferredSize().height + 10);
        this.setOpaque(true);
        this.setBackground(new Color(60, 70, 100, 200));
        //this.setBorder(BorderFactory.createLineBorder(new Color(80, 100, 130), 2));
        this.setHorizontalAlignment(SwingConstants.CENTER);
        this.setVerticalAlignment(SwingConstants.CENTER);
    }

}
