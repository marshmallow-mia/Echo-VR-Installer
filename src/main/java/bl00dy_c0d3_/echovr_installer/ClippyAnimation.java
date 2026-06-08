package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.net.URL;

public class ClippyAnimation extends JPanel {

    private enum Phase { RISE, HOLD, FALL }

    private static final int DEFAULT_DURATION_MS = 400;
    private static final int HOLD_MS = 2000;
    private static final int TICK_MS = 16;

    private final int riseMs, fallMs;
    private Timer timer;
    private int startY, targetY, panelX, currentY;
    private Container parent;
    private Runnable onComplete;
    private long phaseStart;
    private Phase phase;
    private boolean active;
    private final boolean hasGif;

    public ClippyAnimation() {
        this(DEFAULT_DURATION_MS, DEFAULT_DURATION_MS);
    }

    public ClippyAnimation(int riseMs, int fallMs) {
        this.riseMs = Math.max(riseMs, 1);
        this.fallMs = Math.max(fallMs, 1);
        setOpaque(false);
        setLayout(new BorderLayout());

        URL gifUrl = getClass().getClassLoader().getResource("clippy/anim2.gif");
        if (gifUrl != null) {
            ImageIcon icon = new ImageIcon(gifUrl);
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            if (w > 0 && h > 0) {
                setSize(w, h);
                setPreferredSize(new Dimension(w, h));
            }
            JLabel label = new JLabel(icon);
            label.setOpaque(false);
            add(label, BorderLayout.CENTER);
            hasGif = true;
        } else {
            hasGif = false;
        }
        addHierarchyListener(this::onHierarchyChanged);
    }

    public void start(Rectangle tipBoxBounds, Container parent, Runnable onComplete) {
        if (active || !hasGif) return;
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        panelX  = tipBoxBounds.x + (tipBoxBounds.width - w) / 2;
        startY  = tipBoxBounds.y + tipBoxBounds.height;
        targetY = Math.max(0, tipBoxBounds.y - h);
        currentY = startY;
        setBounds(panelX, currentY, w, h);

        this.parent = parent;
        this.onComplete = onComplete;
        this.active = true;
        this.phase = Phase.RISE;
        this.phaseStart = System.currentTimeMillis();

        parent.add(this);
        parent.setComponentZOrder(this, parent.getComponentCount() - 1);
        parent.revalidate();
        parent.repaint();

        if (timer == null) timer = new Timer(TICK_MS, e -> tick());
        timer.start();
    }

    private void tick() {
        if (!active) return;
        long elapsed = System.currentTimeMillis() - phaseStart;
        switch (phase) {
            case RISE:
                float rp = Math.min(1f, (float) elapsed / riseMs);
                currentY = startY - (int)((startY - targetY) * rp);
                if (rp >= 1f) { currentY = targetY; phase = Phase.HOLD; phaseStart = System.currentTimeMillis(); }
                break;
            case HOLD:
                currentY = targetY;
                if (elapsed >= HOLD_MS) { phase = Phase.FALL; phaseStart = System.currentTimeMillis(); }
                break;
            case FALL:
                float fp = Math.min(1f, (float) elapsed / fallMs);
                currentY = targetY + (int)((startY - targetY) * fp);
                if (fp >= 1f) { currentY = startY; cleanup(); return; }
                break;
        }
        setBounds(panelX, currentY, getWidth(), getHeight());
    }

    private void onHierarchyChanged(HierarchyEvent e) {
        if (active && getParent() == null) cleanup();
    }

    private void cleanup() {
        if (!active) return;
        active = false;
        if (timer != null) timer.stop();
        if (parent != null) { parent.remove(this); parent.revalidate(); parent.repaint(); parent = null; }
        Runnable cb = onComplete;
        onComplete = null;
        if (cb != null) cb.run();
    }
}
