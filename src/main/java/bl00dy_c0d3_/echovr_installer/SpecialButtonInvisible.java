package bl00dy_c0d3_.echovr_installer;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URL;

public class SpecialButtonInvisible extends JPanel {


    //Konstruktor:
    public SpecialButtonInvisible() {
        //Grundeinstellungen...
        this.setLayout(new BorderLayout());
        this.setOpaque(true);




        //Label hinzufügen...
        JLabel label = new JLabel("wedfewf", SwingConstants.CENTER);

        label.setForeground(new Color(255, 255, 255));
        label.setBackground(Color.BLUE);
        label.setSize(100,100);
        label.setLocation(100,100);
        this.add(label, BorderLayout.CENTER);


    }






}
