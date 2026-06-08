package bl00dy_c0d3_.echovr_installer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClippyAnimation extends JPanel {

    private static final int TICK_MS = 80;
    private final int riseMs, fallMs;
    private Timer timer;
    private int startY, targetY;
    public int debugCounter; // public so TipBox can read if needed

    public ClippyAnimation() {
        this(400, 400);
    }

    public ClippyAnimation(int riseMs, int fallMs) {
        this.riseMs = Math.max(riseMs, 1);
        this.fallMs = Math.max(fallMs, 1);
        setOpaque(false);

        setSize(124, 93);
        setPreferredSize(new Dimension(124, 93));

        // Timer starts in constructor, runs forever, just calls repaint()
        timer = new Timer(TICK_MS, e -> {
            debugCounter++;
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Draw pulsing/color-changing circle
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark background
        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Pulsing circle: size changes with debugCounter
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int r = 5 + (debugCounter % 15);
        float hue = (debugCounter * 0.03f) % 1.0f;
        g2.setColor(Color.getHSBColor(hue, 1f, 1f));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Frame counter
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.drawString("f:" + debugCounter, 4, 12);

        g2.dispose();

        // Red border
        g.setColor(Color.RED);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }
}
