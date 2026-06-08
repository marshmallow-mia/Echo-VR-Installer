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

    private static final int HOLD_MS = 2000;
    private static final int TICK_MS = 80;

    private List<BufferedImage> frames;
    private int currentFrame;
    private Timer timer;

    private int panelX, panelY;
    private boolean visible;
    private long startTime;
    private Runnable onComplete;

    public ClippyAnimation() {
        setOpaque(false);
        setLayout(null);

        // Extract all GIF frames
        frames = new ArrayList<>();
        try {
            URL gifUrl = getClass().getResource("/clippy/anim2.gif");
            System.err.println("[Clippy] GIF URL: " + gifUrl);
            if (gifUrl != null) {
                ImageInputStream iis = ImageIO.createImageInputStream(gifUrl.openStream());
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(iis);
                    int n = reader.getNumImages(true);
                    System.err.println("[Clippy] GIF frames: " + n);
                    for (int i = 0; i < n; i++) frames.add(reader.read(i));
                    reader.dispose();
                }
                iis.close();
            }
        } catch (Exception e) {
            System.err.println("[Clippy] GIF error: " + e);
        }
        System.err.println("[Clippy] Extracted frames: " + frames.size());

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
        this.startTime = System.currentTimeMillis();
        this.currentFrame = 0;
        this.panelX = position.x;
        this.panelY = Math.max(0, position.y - getHeight());

        setBounds(panelX, panelY, getWidth(), getHeight());
        layeredPane.add(this, JLayeredPane.POPUP_LAYER);
        layeredPane.repaint();
    }

    private void onTick() {
        if (!visible) return;

        // Advance frame
        if (!frames.isEmpty()) {
            currentFrame = (currentFrame + 1) % frames.size();
        }

        // After HOLD_MS, cleanup
        if (System.currentTimeMillis() - startTime >= HOLD_MS) {
            cleanup();
            return;
        }

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
