package bl00dy_c0d3_.echovr_installer;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URL;

public class SpecialButton extends JPanel {
    //Attribute:
    private boolean isDown = false;
    private boolean isHighlighted = false;
    private Image imageUp = null;
    private Image imageDown = null;
    private Image imageHighlighted = null;
    private JLabel label = null;



    //Konstruktor:
    public SpecialButton(String text, String imageUp, String imageDown, String imageHighlighted, int textSize) {
        //Grundeinstellungen...
        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        SpecialButton thisButton = this;

        //Bilder laden...
        this.imageUp = loadGUI(imageUp);
        this.imageDown = loadGUI(imageDown);
        this.imageHighlighted = loadGUI(imageHighlighted);
        this.setSize(getGUIWidth(imageUp), getGUIHeight(imageUp));

        //Schriftart laden...
        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            font  = font.deriveFont(Font.PLAIN, textSize);
        }
        catch (Exception e) {}

        //Label hinzufügen...
        label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font);
        label.setForeground(new Color(230, 230, 230));
        this.add(label, BorderLayout.CENTER);

        //Mouselistener hinzufügen...
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                isDown = true;
                thisButton.repaint();
            }
            public void mouseReleased(MouseEvent event) {
                isDown = false;
                thisButton.repaint();
            }
            public void mouseEntered(MouseEvent event) {
                isHighlighted = true;
                label.setForeground(new Color(250, 250, 250));
                thisButton.repaint();
            }
            public void mouseExited(MouseEvent event) {
                isHighlighted = false;
                label.setForeground(new Color(230, 230, 230));
                thisButton.repaint();
            }
        });
    }



    //Neuzeichnen des JPanels:
    public void paintComponent(Graphics g) {
        //Elternklasse berücksichtigen...
        super.paintComponent(g);

        //Bild zeichnen...
        if (isDown) {
            g.drawImage(imageDown, 0, 0, this);
        }
        else {
            if (isHighlighted) {
                g.drawImage(imageHighlighted, 0, 0, this);
            }
            else {
                g.drawImage(imageUp, 0, 0, this);
            }
        }
    }



    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }



    //Gibt Breite einer GUI Grafik zurück:
    private int getGUIWidth(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return 0;
        else return (new ImageIcon(imageURL, imageName)).getIconWidth();
    }



    //Gibt Höhe einer GUI Grafik zurück:
    private int getGUIHeight(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return 0;
        else return (new ImageIcon(imageURL, imageName)).getIconHeight();
    }

    void changeText(String text){
        label.setText(text);
    }

}
