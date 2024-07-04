package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class SpecialCheckBox extends JCheckBox{

    public SpecialCheckBox (String text, int textsize){

        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            font  = font.deriveFont(Font.PLAIN, textsize);
        }
        catch (Exception e) {}

        this.setFont(font); // Set text size to 20
        this.setText(text);
        this.setOpaque(false);
        this.setForeground(Color.WHITE);
        this.setBorderPaintedFlat(false);

    }
}
