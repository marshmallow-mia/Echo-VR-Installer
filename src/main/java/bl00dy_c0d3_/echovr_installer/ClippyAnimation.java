package bl00dy_c0d3_.echovr_installer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing JPanel that displays a Clippy sprite animation with concurrent
 * slide-in/slide-out motion.  Animates upward from behind a UI element,
 * cycles through PNG frames, then slides back down and removes itself.
 */
public class ClippyAnimation extends JPanel {

    private enum Phase { RISE, HOLD, FALL }

    private static final int FRAME_INTERVAL_MS = 80;
    private static final int TIMER_INTERVAL_MS = 16;
    private static final int DEFAULT_DURATION_MS = 400;

    private final int riseDurationMs;
    private final int fallDurationMs;

    private List<BufferedImage> frames;
    private Timer timer;
    private int currentFrame;
    private Phase phase;
    private long phaseStartTime;
    private long lastFrameTime;
    private int startY;
    private int targetY;
    private int panelX;
    private int currentY;
    private boolean active;
    private Runnable onComplete;
    private Container parent;

    public ClippyAnimation() {
        this(DEFAULT_DURATION_MS, DEFAULT_DURATION_MS);
    }

    public ClippyAnimation(int riseDurationMs, int fallDurationMs) {
        this.riseDurationMs = Math.max(riseDurationMs, 1);
        this.fallDurationMs = Math.max(fallDurationMs, 1);
        this.frames = loadFrames();

        setOpaque(false);

        if (frames.isEmpty()) {
            System.err.println(
                "ClippyAnimation: No frame_*.png files found in clippy/ -- start() is a no-op.");
            setSize(0, 0);
        } else {
            setSize(frames.get(0).getWidth(), frames.get(0).getHeight());
        }

        addHierarchyListener(this::onHierarchyChanged);
    }

    /**
     * Start the animation, sliding upward from below {@code tipBoxBounds},
     * cycling frames, holding above, then sliding back down.
     *
     * @param tipBoxBounds bounds of the UI element the clippy should pop up behind/above
     * @param parent       container to add this panel to
     * @param onComplete   callback invoked after the animation finishes (or window closes)
     */
    public void start(Rectangle tipBoxBounds, Container parent, Runnable onComplete) {
        if (active || frames.isEmpty()) {
            return;
        }

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth <= 0 || panelHeight <= 0) {
            return;
        }

        panelX = tipBoxBounds.x + (tipBoxBounds.width - panelWidth) / 2;
        startY = tipBoxBounds.y + tipBoxBounds.height;
        targetY = Math.max(0, tipBoxBounds.y - panelHeight);
        currentY = startY;

        setBounds(panelX, currentY, panelWidth, panelHeight);

        this.parent = parent;
        this.onComplete = onComplete;
        this.active = true;

        currentFrame = 0;
        phase = Phase.RISE;
        lastFrameTime = System.currentTimeMillis();
        phaseStartTime = lastFrameTime;

        parent.add(this);
        parent.setComponentZOrder(this, 0);

        if (timer == null) {
            timer = new Timer(TIMER_INTERVAL_MS, e -> onTick());
        }
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (frames == null || frames.isEmpty() || currentFrame >= frames.size()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(frames.get(currentFrame), 0, 0, this);
        g2.dispose();
        super.paintComponent(g);
    }

    private void onTick() {
        if (!active) return;

        long now = System.currentTimeMillis();
        long elapsed = now - phaseStartTime;

        switch (phase) {
            case RISE:
                float riseProgress = Math.min(1.0f, (float) elapsed / riseDurationMs);
                currentY = startY - (int) ((startY - targetY) * riseProgress);
                if (riseProgress >= 1.0f) {
                    currentY = targetY;
                    phase = Phase.HOLD;
                    phaseStartTime = now;
                }
                break;

            case HOLD:
                currentY = targetY;
                if (currentFrame >= frames.size() - 1) {
                    phase = Phase.FALL;
                    phaseStartTime = now;
                }
                break;

            case FALL:
                float fallProgress = Math.min(1.0f, (float) elapsed / fallDurationMs);
                currentY = targetY + (int) ((startY - targetY) * fallProgress);
                if (fallProgress >= 1.0f) {
                    currentY = startY;
                    cleanup();
                    return;
                }
                break;
        }

        if (now - lastFrameTime >= FRAME_INTERVAL_MS) {
            lastFrameTime = now;
            if (currentFrame < frames.size() - 1) {
                currentFrame++;
            }
        }

        setBounds(panelX, currentY, getWidth(), getHeight());
        repaint();
    }

    private void onHierarchyChanged(HierarchyEvent e) {
        if (active && getParent() == null) {
            cleanup();
        }
    }

    private void cleanup() {
        if (!active) return;
        active = false;

        if (timer != null) {
            timer.stop();
        }
        if (parent != null) {
            parent.remove(this);
            parent.revalidate();
            parent.repaint();
            parent = null;
        }

        Runnable cb = onComplete;
        onComplete = null;
        if (cb != null) {
            cb.run();
        }

        if (frames != null) {
            frames.clear();
            frames = null;
        }
    }

    private static List<BufferedImage> loadFrames() {
        List<BufferedImage> list = new ArrayList<>();
        ClassLoader cl = ClippyAnimation.class.getClassLoader();

        for (int i = 0; i < 1000; i++) {
            BufferedImage img = loadFrame(cl, i);
            if (img != null) {
                list.add(img);
            } else if (!list.isEmpty()) {
                break;
            }
        }

        return list;
    }

    private static BufferedImage loadFrame(ClassLoader cl, int index) {
        String[] names = {
            String.format("clippy/frame_%03d.png", index),
            String.format("clippy/frame_%04d.png", index),
            "clippy/frame_" + index + ".png"
        };
        for (String name : names) {
            URL url = cl.getResource(name);
            if (url != null) {
                try {
                    return ImageIO.read(url);
                } catch (IOException e) {
                    System.err.println(
                        "ClippyAnimation: Failed to read " + name + ": " + e.getMessage());
                }
            }
        }
        return null;
    }
}
