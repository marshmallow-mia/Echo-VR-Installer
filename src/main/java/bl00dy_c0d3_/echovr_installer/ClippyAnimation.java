package bl00dy_c0d3_.echovr_installer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClippyAnimation extends JPanel {

    private enum Phase { RISE, HOLD, FALL }

    private static final int RISE_MS = 400;
    private static final int HOLD_MS = 2000;
    private static final int FALL_MS = 400;
    private static final int TICK_MS = 80;

    private List<BufferedImage> frames;
    private int currentFrame;
    private Timer timer;

    // Position / phase
    private int panelX, startY, targetY, currentY;
    private boolean visible;
    private Phase phase;
    private long phaseStart;
    private Runnable onComplete;

    public ClippyAnimation() {
        setOpaque(false);
        setLayout(null);

        // Extract GIF frames
        frames = new ArrayList<>();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("clippy/anim2.gif");
            System.err.println("[Clippy] GIF stream: " + (is != null));
            if (is != null) {
                ImageInputStream iis = ImageIO.createImageInputStream(is);
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                System.err.println("[Clippy] GIF readers: " + (readers != null && readers.hasNext()));
                if (readers != null && readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(iis);
                    int n = reader.getNumImages(true);
                    System.err.println("[Clippy] GIF frames: " + n);
                    for (int i = 0; i < n; i++) frames.add(reader.read(i));
                    reader.dispose();
                } else {
                    // Fallback: try loading as single image
                    URL gifUrl = getClass().getClassLoader().getResource("clippy/anim2.gif");
                    if (gifUrl != null) {
                        ImageIcon icon = new ImageIcon(gifUrl);
                        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D bg = bi.createGraphics();
                        bg.drawImage(icon.getImage(), 0, 0, null);
                        bg.dispose();
                        frames.add(bi);
                        System.err.println("[Clippy] Fallback single frame: " + icon.getIconWidth() + "x" + icon.getIconHeight());
                    }
                }
                iis.close(); is.close();
            }
        } catch (Exception e) {
            System.err.println("[Clippy] GIF load error: " + e.getMessage());
        }

        if (frames.isEmpty()) {
            // No frames: still works (shows empty box with border)
            setSize(124, 93);
        } else {
            setSize(frames.get(0).getWidth(), frames.get(0).getHeight());
        }
        setPreferredSize(getSize());

        // Timer runs forever — drives both frame cycling and position update
        timer = new Timer(TICK_MS, e -> onTick());
        timer.start();
    }

    public void start(Rectangle position, JLayeredPane layeredPane, Runnable onComplete) {
        this.onComplete = onComplete;
        this.visible = true;
        this.phase = Phase.RISE;
        this.phaseStart = System.currentTimeMillis();
        this.currentFrame = 0;
        this.panelX = position.x;
        this.startY = position.y + position.height;
        this.targetY = Math.max(0, position.y - getHeight());
        this.currentY = startY;

        setBounds(panelX, currentY, getWidth(), getHeight());
        layeredPane.add(this, JLayeredPane.POPUP_LAYER);
        layeredPane.repaint();
    }

    private void onTick() {
        if (!visible) return;

        long elapsed = System.currentTimeMillis() - phaseStart;

        // Advance frame (loop while visible)
        if (!frames.isEmpty()) {
            currentFrame = (currentFrame + 1) % frames.size();
        }

        // Update Y position based on phase
        switch (phase) {
            case RISE:
                float rp = Math.min(1f, (float) elapsed / RISE_MS);
                currentY = startY - (int) ((startY - targetY) * rp);
                if (rp >= 1f) { currentY = targetY; phase = Phase.HOLD; phaseStart = System.currentTimeMillis(); }
                break;
            case HOLD:
                currentY = targetY;
                if (elapsed >= HOLD_MS) { phase = Phase.FALL; phaseStart = System.currentTimeMillis(); }
                break;
            case FALL:
                float fp = Math.min(1f, (float) elapsed / FALL_MS);
                currentY = targetY + (int) ((startY - targetY) * fp);
                if (fp >= 1f) { cleanup(); return; }
                break;
        }

        setBounds(panelX, currentY, getWidth(), getHeight());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (frames.isEmpty()) {
            g.setColor(Color.RED);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            return;
        }
        g.drawImage(frames.get(currentFrame), 0, 0, this);
    }

    private void cleanup() {
        if (!visible) return;
        visible = false;
        Container p = getParent();
        if (p != null) { p.remove(this); p.repaint(); }
        Runnable cb = onComplete;
        onComplete = null;
        if (cb != null) cb.run();
    }
}
