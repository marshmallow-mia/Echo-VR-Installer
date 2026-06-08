package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URL;

public class TipBox extends JPanel {

    private static final int DEFAULT_BOX_WIDTH = 300;
    private static final int DEFAULT_BOX_HEIGHT = 100;
    private static final int GAP = 5;
    private static final int PADDING = 8;
    private static final String DEFAULT_TEXT = "Hover over items for tips.";
    private JLabel tipLabel;
    private int tipBoxDisplayW;
    private int currentImgW = -1;
    private int currentImgH = -1;
    private int currentBoxW = -1;
    private int currentBoxH = -1;

    private static final int CLIPPY_RISE_MS = 400;
    private static final int CLIPPY_FALL_MS = 400;
    private boolean clippyAnimating = false;
    private ClippyAnimation clippyAnimation = null;
    private boolean clippyTestStarted = false;

    private final MouseAdapter clippyListener = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            System.err.println("[Clippy] mousePressed clickCount=" + e.getClickCount() + " button=" + e.getButton());
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                triggerClippy();
            }
        }
    };

    public TipBox() {
        this(300, -1, -1, -1);
    }

    /**
     * @param imgW  header image width,  or -1 for native (or auto-scale from imgH)
     * @param imgH  header image height, or -1 for native (or auto-scale from imgW)
     * @param boxW  grey tip box width,  or -1 for default (300)
     * @param boxH  grey tip box height, or -1 for default (100)
     */
    public TipBox(int imgW, int imgH, int boxW, int boxH) {
        setLayout(null);
        setOpaque(false);
        currentImgW = imgW;
        currentImgH = imgH;
        currentBoxW = boxW;
        currentBoxH = boxH;
        rebuild();
        addMouseListener(clippyListener);
        addHierarchyListener(new java.awt.event.HierarchyListener() {
            public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
                if (!clippyTestStarted && isShowing()) {
                    clippyTestStarted = true;
                    System.err.println("[Clippy] Became visible, triggering first test animation in 200ms");
                    new javax.swing.Timer(200, evt2 -> {
                        triggerClippy();
                    }).start();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(200, 0, 150, 200));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        g2.setColor(new Color(50, 50, 50, 255));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
        super.paintChildren(g2);
        g2.dispose();
    }

    public void setHeaderSize(int w, int h) {
        currentImgW = w;
        currentImgH = h;
        removeAll();
        rebuild();
        revalidate();
        repaint();
    }

    private void rebuild() {
        URL url = getClass().getClassLoader().getResource("tipbox_top.png");
        ImageIcon originalIcon = new ImageIcon(url);
        int origW = originalIcon.getIconWidth();
        int origH = originalIcon.getIconHeight();

        // ---- header image dimensions ----
        int imgW, imgH;
        if (currentImgW > 0 && currentImgH > 0) {
            imgW = currentImgW;
            imgH = currentImgH;
        } else if (currentImgW > 0) {
            imgW = currentImgW;
            imgH = (int) ((double) origH * currentImgW / origW);
        } else if (currentImgH > 0) {
            imgH = currentImgH;
            imgW = (int) ((double) origW * currentImgH / origH);
        } else {
            imgW = origW;
            imgH = origH;
        }

        // Scale image if needed
        ImageIcon icon;
        if (imgW == origW && imgH == origH) {
            icon = originalIcon;
        } else {
            icon = new ImageIcon(originalIcon.getImage().getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH));
        }

        // ---- grey tip box dimensions ----
        int boxW = currentBoxW > 0 ? currentBoxW : DEFAULT_BOX_WIDTH;
        int boxH = currentBoxH > 0 ? currentBoxH : DEFAULT_BOX_HEIGHT;

        // ---- overall component (padding on all sides so overlay extends beyond content) ----
        int totalW = Math.max(imgW, boxW) + PADDING * 2;
        int totalH = imgH + GAP + boxH + PADDING * 2;

        setPreferredSize(new Dimension(totalW, totalH));
        setSize(totalW, totalH);

        // ---- header image label (centered, shifted down by PADDING) ----
        JLabel headerLabel = new JLabel("Tipbox", icon, SwingConstants.CENTER);
        headerLabel.setBounds(PADDING + (totalW - PADDING * 2 - imgW) / 2, PADDING, imgW, imgH);
        headerLabel.setHorizontalTextPosition(JLabel.CENTER);
        headerLabel.setVerticalTextPosition(JLabel.CENTER);
        headerLabel.setForeground(Color.WHITE);

        Font headerFont = null;
        try {
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
            if (fontStream != null) {
                try {
                    headerFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                    headerFont = headerFont.deriveFont(Font.PLAIN, 20f);
                } finally {
                    fontStream.close();
                }
            }
        } catch (Exception e) {
        }
        if (headerFont == null) {
            headerFont = new Font("Arial", Font.BOLD, 20);
        }
        headerLabel.setFont(headerFont);

        headerLabel.addMouseListener(clippyListener);

        add(headerLabel);

        // ---- grey tip box (centered, with margin so transparent bg shows around it) ----
        int boxDisplayW = Math.max(boxW - PADDING * 2, 10);
        tipBoxDisplayW = boxDisplayW;
        String htmlDefault = "<html><center>" + DEFAULT_TEXT + "</center></html>";
        tipLabel = new JLabel(htmlDefault, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(70, 70, 70, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(50, 50, 50, 200));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tipLabel.setOpaque(false);
        tipLabel.setBounds(PADDING + (totalW - PADDING * 2 - boxDisplayW) / 2, PADDING + imgH + GAP, boxDisplayW, boxH);
        tipLabel.setForeground(Color.WHITE);
        tipLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        tipLabel.addMouseListener(clippyListener);

        add(tipLabel);
    }

    public void showTip(String tip) {
        tipLabel.setText("<html><center>" + tip + "</center></html>");
    }

    public void showDefault() {
        tipLabel.setText("<html><center>" + DEFAULT_TEXT + "</center></html>");
    }

    private void triggerClippy() {
        System.err.println("[Clippy] === triggerClippy() called at " + System.currentTimeMillis()
                + " isShowing=" + isShowing() + " size=" + getWidth() + "x" + getHeight()
                + " parent=" + (getParent() != null));
        System.err.println("[Clippy] triggerClippy() called");
        if (clippyAnimating) { System.err.println("[Clippy] BLOCKED: already animating"); return; }
        if (!isShowing()) { System.err.println("[Clippy] BLOCKED: not showing"); return; }
        if (getWidth() <= 0 || getHeight() <= 0) { System.err.println("[Clippy] BLOCKED: zero size w="+getWidth()+" h="+getHeight()); return; }
        Container parent = getParent();
        if (parent == null) { System.err.println("[Clippy] BLOCKED: no parent"); return; }
        System.err.println("[Clippy] Starting animation. parent=" + parent.getClass().getSimpleName() + " tipBoxBounds=" + getBounds());
        try {
            clippyAnimation = new ClippyAnimation(CLIPPY_RISE_MS, CLIPPY_FALL_MS);
            clippyAnimating = true;
            Rectangle tipBoxBounds = getBounds();
            clippyAnimation.start(tipBoxBounds, parent, () -> {
                System.err.println("[Clippy] Animation complete, scheduling next in 1s");
                clippyAnimating = false;
                clippyAnimation = null;
                new javax.swing.Timer(1000, evt -> triggerClippy()).start();
            });
        } catch (Exception ex) {
            System.err.println("[Clippy] EXCEPTION: " + ex.getMessage());
            ex.printStackTrace();
            clippyAnimating = false;
            clippyAnimation = null;
        }
    }
}
