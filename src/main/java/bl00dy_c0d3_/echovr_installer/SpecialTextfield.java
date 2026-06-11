package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class SpecialTextfield extends JTextField{

    private String placeholder = "";

    /** Grey hint text shown while the field is empty (purpose is self-evident before any input). */
    public void setPlaceholder(String text) {
        this.placeholder = text == null ? "" : text;
        repaint();
    }

    public void specialTextfield(int width, int height, int x, int y, int textSize){
        this.setSize(width, height);
        this.setLocation(x, y);
        // Not opaque: the parent repaints the background behind us, so paintComponent below
        // clears stale text before drawing the current text (prevents ghosting on change),
        // mirroring SpecialLabel. A plain opaque JTextField filling a translucent colour would
        // leave the previous text visible underneath.
        this.setOpaque(false);
        this.setBackground(new Color(30, 30, 30, 200));
        this.setForeground(Color.WHITE);
        this.setCaretColor(Color.WHITE);
        this.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            font  = font.deriveFont(Font.PLAIN, textSize);
        }
        catch (Exception e) {}

        this.setFont(font);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.dispose();
        super.paintComponent(g);

        // Placeholder: drawn only while empty so getText() stays genuinely empty (no fake value).
        if (!placeholder.isEmpty() && getText().isEmpty()) {
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(new Color(170, 170, 170));
            p.setFont(getFont());
            Insets ins = getInsets();
            FontMetrics fm = p.getFontMetrics();
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            p.drawString(placeholder, ins.left, ty);
            p.dispose();
        }
    }
}
