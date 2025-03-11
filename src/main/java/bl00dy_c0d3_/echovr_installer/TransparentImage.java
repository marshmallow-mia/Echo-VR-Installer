package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TransparentImage extends JPanel {
    private BufferedImage background;
    private BufferedImage overlay;
    private int overlayX;
    private int overlayY;

    public TransparentImage(String backgroundPath, String overlayPath, int overlayX, int overlayY) {
        try {
            // Load the background image
            background = ImageIO.read(new File(backgroundPath));

            // Load the overlay image (with transparency)
            overlay = ImageIO.read(new File(overlayPath));

            // Set the overlay position
            this.overlayX = overlayX;
            this.overlayY = overlayY;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the background image
        if (background != null) {
            g.drawImage(background, 0, 0, this.getWidth(), this.getHeight(), this);
        }

        // Draw the overlay image at the specified position
        if (overlay != null) {
            g.drawImage(overlay, overlayX, overlayY, this);
        }
    }

    public static void main(String[] args) {
        // Example usage
        JFrame frame = new JFrame("Transparent Image Example");
        TransparentImage panel = new TransparentImage("path/to/your/background.png", "path/to/your/overlay.png", 50, 50);

        frame.setContentPane(panel);
        frame.setSize(800, 600); // Set the desired frame size
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
