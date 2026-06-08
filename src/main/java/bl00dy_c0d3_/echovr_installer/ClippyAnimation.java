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

    private static final int TICK_MS = 80;
    private static final int DEFAULT_HOLD_MS = 2000;

    private List<BufferedImage> frames;
    private int currentFrame;
    private Timer timer;

    private int panelX, startY, targetY, currentY;
    private boolean visible;
    private long phaseStart;
    private boolean rising;
    private boolean falling;
    private int riseFrames;
    private int fallFrames;
    private Runnable onComplete;

    public ClippyAnimation() {
        this(10, 10);
    }

    public ClippyAnimation(int riseFrames, int fallFrames) {
        this.riseFrames = Math.max(riseFrames, 1);
        this.fallFrames = Math.max(fallFrames, 1);
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
        this.rising = true;
        this.falling = false;
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
        long now = System.currentTimeMillis();

        if (rising) {
            // Rise: slide up while playing first riseFrames
            float p = (float) (now - phaseStart) / (riseFrames * TICK_MS);
            currentY = startY - (int) ((startY - targetY) * Math.min(1f, p));
            currentFrame = Math.min(currentFrame, riseFrames - 1);
            if (p >= 1f) {
                currentY = targetY;
                rising = false;
                phaseStart = now;
            }
            setBounds(panelX, currentY, getWidth(), getHeight());
        } else if (falling) {
            // Fall: slide down while playing last fallFrames
            long elapsed = now - phaseStart;
            float p = (float) elapsed / (fallFrames * TICK_MS);
            currentY = targetY + (int) ((startY - targetY) * Math.min(1f, p));
            if (p >= 1f) { cleanup(); return; }
            // Map currentFrame to last fallFrames frames
            int fi = Math.min((int) (elapsed / TICK_MS), fallFrames - 1);
            currentFrame = Math.max(0, frames.size() - fallFrames + fi);
            setBounds(panelX, currentY, getWidth(), getHeight());
        } else {
            // Hold: loop middle frames at top, then start falling
            long elapsed = now - phaseStart;
            currentFrame = riseFrames + (int)((elapsed / TICK_MS) % Math.max(1, frames.size() - riseFrames - fallFrames));
            if (currentFrame >= frames.size() - fallFrames) {
                currentFrame = riseFrames;
            }
            currentY = targetY;
            if (elapsed >= DEFAULT_HOLD_MS) {
                falling = true;
                phaseStart = now;
            }
        }

        if (!frames.isEmpty() && !falling) {
            currentFrame = currentFrame % frames.size();
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
