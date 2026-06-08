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

    private enum Phase { RISE, HOLD, FALL }

    private static final int DEFAULT_DURATION_MS = 400;
    private static final int HOLD_MS = 2000;
    private static final int TICK_MS = 80; // match GIF frame rate

    private final int riseMs, fallMs;
    private Timer timer;
    private int layeredX, startY, targetY, currentY;
    private int panelW, panelH;
    private JLayeredPane layeredPane;
    private Runnable onComplete;
    private long phaseStart;
    private Phase phase;
    private boolean active;
    private List<BufferedImage> frames;
    private int currentFrame;
    private boolean framesAvailable;

    public ClippyAnimation() {
        this(DEFAULT_DURATION_MS, DEFAULT_DURATION_MS);
    }

    public ClippyAnimation(int riseMs, int fallMs) {
        this.riseMs = Math.max(riseMs, 1);
        this.fallMs = Math.max(fallMs, 1);
        setOpaque(false);
        setLayout(null);

        // Extract all frames from the animated GIF
        frames = new ArrayList<>();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("clippy/anim2.gif");
            if (is != null) {
                ImageInputStream iis = ImageIO.createImageInputStream(is);
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(iis);
                    int numFrames = reader.getNumImages(true);
                    for (int i = 0; i < numFrames; i++) {
                        BufferedImage frame = reader.read(i);
                        frames.add(frame);
                    }
                    reader.dispose();
                }
                iis.close();
                is.close();
            }
        } catch (Exception e) {
            System.err.println("[ClippyAnim] GIF frame extraction failed: " + e.getMessage());
        }

        framesAvailable = !frames.isEmpty();
        if (framesAvailable) {
            panelW = frames.get(0).getWidth();
            panelH = frames.get(0).getHeight();
        }
        if (panelW <= 0) { panelW = 100; panelH = 100; }
        setSize(panelW, panelH);
        setPreferredSize(new Dimension(panelW, panelH));
        addHierarchyListener(this::onHierarchyChanged);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // DEBUG: pulsing circle — obvious animation to verify frame cycling works
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background: dark base
        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        // Pulsing circle: size changes with currentFrame
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int r = 5 + (currentFrame % 15);
        float hue = (currentFrame * 0.03f) % 1.0f;
        g2.setColor(Color.getHSBColor(hue, 1f, 1f));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        
        // Frame counter
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.drawString("f:" + currentFrame + " y:" + currentY, 4, 12);
        
        g2.dispose();
        
        // Red border
        g.setColor(Color.RED);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    public void start(Component anchor, Runnable onComplete) {
        if (active) return;

        Window rootWindow = SwingUtilities.getWindowAncestor(anchor);
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
        this.currentFrame = 0;

        layeredPane.add(this, JLayeredPane.POPUP_LAYER);
        layeredPane.repaint();

        if (timer == null) timer = new Timer(TICK_MS, e -> tick());
        timer.start();
    }

    private void tick() {
        if (!active) return;

        // Advance frame every tick
        if (framesAvailable) {
            currentFrame = (currentFrame + 1) % frames.size();
        }

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
        repaint();
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
        if (frames != null) frames.clear();
        Runnable cb = onComplete;
        onComplete = null;
        if (cb != null) cb.run();
    }
}
