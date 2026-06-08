package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Background extends JPanel {
    //Attribute:
    private Image image = null;

    //Konstruktor - full size (native resolution):
    public Background(String imageName) {
        this(imageName, -1, -1);
    }

    //Konstruktor with proportional scaling.
    // width > 0 and height <= 0: width set, height auto-scales
    // height > 0 and width <= 0: height set, width auto-scales
    // both > 0: exact dimensions
    // both <= 0: native size
    public Background(String imageName, int width, int height) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return;

        ImageIcon icon = new ImageIcon(imageURL, imageName);
        Image rawImage = icon.getImage();

        int origW = icon.getIconWidth();
        int origH = icon.getIconHeight();

        int targetW, targetH;
        if (width > 0 && height > 0) {
            targetW = width;
            targetH = height;
        } else if (width > 0) {
            targetW = width;
            targetH = (int) ((double) origH * width / origW);
        } else if (height > 0) {
            targetW = (int) ((double) origW * height / origH);
            targetH = height;
        } else {
            targetW = origW;
            targetH = origH;
        }

        this.image = rawImage.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        //PanelgrÃ¶ÃŸe automatisch an die skalierte BildgrÃ¶ÃŸe anpassen (kein weiÃŸer Rand)
        setSize(targetW, targetH);
    }

    //Neuzeichnen des JPanels:
    public void paintComponent(Graphics g) {
        //Elternklasse berÃ¼cksichtigen...
        super.paintComponent(g);

        //Bild zeichnen...
        if (image != null) g.drawImage(image, 0, 0, this);
    }

    //LÃ¤dt eine GUI-Grafik und gibt sie zurÃ¼ck:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }
}
