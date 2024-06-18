package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URI;

public class SpecialHyperlink extends JLabel{

    public SpecialHyperlink(int x, int y, String text, String url, int textsize){

        // Create the label with a hyperlink
        this.setText(text);
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));

        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            font  = font.deriveFont(Font.PLAIN, textsize);
        }
        catch (Exception e) {}

        this.setLocation(x, y); // Set position
        this.setFont(font); // Set text size to 20
        // TODO FONT
        this.setForeground(Color.WHITE); // Set text color to white

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    System.out.println("Hyperlink failed.");
                    //TODO This exeption is bullshit
                }
            }
        });
        this.setSize(this.getPreferredSize());


    }

}
