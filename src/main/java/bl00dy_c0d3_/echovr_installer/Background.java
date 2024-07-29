package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Background extends JPanel {
    //Attribute:
    private Image image = null;


    //Konstruktor:
    public Background(String imageName) {
        this.image = loadGUI(imageName);
    }



    //Neuzeichnen des JPanels:
    public void paintComponent(Graphics g) {
        //Elternklasse berücksichtigen...
        super.paintComponent(g);

        //Bild zeichnen...
        if (image != null) g.drawImage(image, 0, 0, this);
    }


    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }
}
