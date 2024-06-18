package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class SpecialTextfield extends JTextField{

    public void specialTextfield(int width, int height, int x, int y, int textSize){
        this.setSize(width, height);
        this.setLocation(x, y);
        this.setBackground(new Color(30, 30, 30, 200));
        this.setForeground(Color.WHITE);
        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            font  = font.deriveFont(Font.PLAIN, textSize);
        }
        catch (Exception e) {}

        this.setFont(font);
    }

}
