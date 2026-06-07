package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class SpecialLabel extends JLabel {

    static Font baseFont;

    static {
        try (InputStream fs = SpecialLabel.class.getClassLoader().getResourceAsStream("conthrax-sb.otf")) {
            if (fs != null) baseFont = Font.createFont(Font.TRUETYPE_FONT, fs);
        } catch (Exception e) {
            System.err.println("Font conthrax-sb.otf not loadable (temp full?), using Arial fallback");
        }
    }

    // Constructor
    public SpecialLabel(String labelText, int textSize) {
        Font font = baseFont != null ? baseFont.deriveFont(Font.PLAIN, textSize) : new Font("Arial", Font.PLAIN, textSize);

        this.setText(labelText);
        this.setFont(font);
        this.setForeground(Color.WHITE);
        this.setSize(this.getPreferredSize().width + 10, this.getPreferredSize().height + 10);
        this.setBackground(new Color(60, 70, 100, 200));
        this.setHorizontalAlignment(SwingConstants.CENTER);
        this.setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Paint semi-transparent background over the parent (which repaints behind us
        // since we're not opaque). This ensures old text is fully cleared before
        // painting the new text — preventing ghosting/overlap on text changes.
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
