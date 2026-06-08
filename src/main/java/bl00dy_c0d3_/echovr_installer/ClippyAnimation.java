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
    private int layeredX, startY, targetY, currentY;
    private int panelW, panelH;
    private JLayeredPane layeredPane;
    private Window rootWindow;
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
            panelW = icon.getIconWidth();
            panelH = icon.getIconHeight();
            if (panelW > 0 && panelH > 0) {
                setSize(panelW, panelH);
                setPreferredSize(new Dimension(panelW, panelH));
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

    public void start(Component anchor, Runnable onComplete) {
        if (active || !hasGif || panelW <= 0) return;

        rootWindow = SwingUtilities.getWindowAncestor(anchor);
        if (rootWindow == null) return;

        if (rootWindow instanceof JFrame) {
            layeredPane = ((JFrame) rootWindow).getLayeredPane();
        } else if (rootWindow instanceof JDialog) {
            layeredPane = ((JDialog) rootWindow).getLayeredPane();
        } else {
            return;
        }

        Point tipBoxScreen = anchor.getLocationOnScreen();
        Point lpScreen = layeredPane.getLocationOnScreen();

        layeredX = tipBoxScreen.x - lpScreen.x + (anchor.getWidth() - panelW) / 2;
        startY  = tipBoxScreen.y + anchor.getHeight() - lpScreen.y;
        targetY = Math.max(0, tipBoxScreen.y - lpScreen.y - panelH);
        currentY = startY;

        setBounds(layeredX, currentY, panelW, panelH);

        this.onComplete = onComplete;
        this.active = true;
        this.phase = Phase.RISE;
        this.phaseStart = System.currentTimeMillis();

        layeredPane.add(this, JLayeredPane.POPUP_LAYER);
        layeredPane.repaint();

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

        setBounds(layeredX, currentY, panelW, panelH);
    }

    private void onHierarchyChanged(HierarchyEvent e) {
        if (active && getParent() == null) cleanup();
    }

    private void cleanup() {
        if (!active) return;
        active = false;
        if (timer != null) timer.stop();
        if (layeredPane != null) {
            layeredPane.remove(this);
            layeredPane.repaint();
            layeredPane = null;
        }
        Runnable cb = onComplete;
        onComplete = null;
        if (cb != null) cb.run();
    }
}
