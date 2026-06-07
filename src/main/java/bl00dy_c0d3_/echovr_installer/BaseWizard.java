package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public abstract class BaseWizard extends JDialog {

    protected int FW = 700;
    protected int FH = 594;
    protected static final Color BOX_BORDER = new Color(50, 50, 50, 150);
    protected static final int SIDEBAR_W = 120;

    protected JPanel contentPanel;
    protected JPanel progPanel;
    protected JPanel statusBarBox;
    protected SpecialButton backBtn;
    protected SpecialButton nextBtn;
    protected TipBox tipBox;
    protected FrameMain frameMain;

    protected Downloader downloader;
    protected Downloader downloadPatch;
    protected JLabel dlProgressLabel;
    protected SpecialButton dlButton;

    protected JPanel sidebarPanel;
    protected JLabel sidebarStepLabel;
    protected JLabel[] sidebarSubLabels;

    protected boolean stepInProgress = false;
    protected boolean stepCompleted = false;
    protected int currentStep = 0;
    protected int currentSubstep = 0;
    protected javax.swing.Timer progressAnimator;
    protected float animPhase = 0f;

    protected abstract String getBackgroundImage();
    protected abstract int getWindowWidth();
    protected abstract int getWindowHeight();
    protected abstract String getWindowTitle();
    protected abstract int getStepCount();
    protected abstract int getSubstepCount(int step);
    protected abstract String getSubstepName(int step, int sub);
    protected abstract String getChipLabel(int step);
    protected abstract void buildContent(int step, int sub, int cx);
    protected abstract void updateStatusText(int step, int sub);

    public BaseWizard(FrameMain frameMain) {
        super(frameMain, true);
        this.frameMain = frameMain;
        initWindow();
        progressAnimator = new javax.swing.Timer(50, e -> {
            animPhase += 0.15f;
            if (progPanel != null) progPanel.repaint();
            if (statusBarBox != null) statusBarBox.repaint();
        });
    }

    protected void initWindow() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (confirmAbortDownload()) dispose();
            }
        });
        setResizable(false);
        setIconImage(loadGUI("icon.png"));
        setTitle(getWindowTitle());

        Background back = new Background(getBackgroundImage(), -1, FH);
        int w = back.getWidth(); if (w > 0) FW = w;
        back.setLayout(null); setContentPane(back);

        tipBox = new TipBox();
        int contentBoxX = SIDEBAR_W + 30; // 10(edge) + 130(sidebar) + 10(gap) = 150
        int contentBoxW = FW - contentBoxX - 10; // right margin 10px

        int statusBarY = 10;
        int statusBarH = 32;
        statusBarBox = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill;
                if (stepInProgress) {
                    float pulse = (float)(Math.sin(animPhase) * 0.5 + 0.5);
                    fill = new Color((int)(50 + pulse * 40), (int)(90 + pulse * 50), (int)(150 + pulse * 60), 255);
                } else if (stepCompleted) {
                    fill = new Color(40, 130, 40, 255);
                } else {
                    fill = new Color(50, 90, 150, 255);
                }
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BOX_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        statusBarBox.setBounds(contentBoxX, statusBarY, contentBoxW, statusBarH);
        statusBarBox.setOpaque(false);
        back.add(statusBarBox);

        dlProgressLabel = new JLabel("", SwingConstants.CENTER);
        dlProgressLabel.setForeground(Color.WHITE);
        dlProgressLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dlProgressLabel.setOpaque(false);
        dlProgressLabel.setBounds(contentBoxX, statusBarY, contentBoxW, statusBarH);
        back.add(dlProgressLabel);
        back.setComponentZOrder(dlProgressLabel, 0);

        int cy = 72;
        contentPanel = new JPanel(null);
        contentPanel.setBounds(contentBoxX + 10, cy, contentBoxW - 20, 245);
        contentPanel.setOpaque(false);
        back.add(contentPanel);
        int ce = Math.max(cy + 245, cy + contentPanel.getHeight());

        int tsy = ce + 10;
        int tw = tipBox.getWidth(), th = tipBox.getHeight();
        tipBox.setLocation(contentBoxX + (contentBoxW - tw) / 2, tsy + 8);
        back.add(tipBox);
        int bY = contentPanel.getY() - 20;
        int bH = (tipBox.getY() + th) - bY + 20;
        back.add(sectionBoxAt(contentBoxX, bY, bH, 15, contentBoxW, new Color(200, 0, 150, 90)));
    }

    protected boolean confirmAbortDownload() {
        if (!stepInProgress) return true;
        int choice = JOptionPane.showConfirmDialog(this,
            "Installation is still in progress.\n\nAbort and continue?",
            "Install not done", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            if (downloader != null) downloader.cancelDownload();
            stepInProgress = false;
            progressAnimator.stop();
            if (dlButton != null) dlButton.changeText("Start Download");
            return true;
        }
        return false;
    }

    protected JPanel sectionBox(int y, int h, int arc, int w, Color fill) {
        JPanel p = new JPanel(null) {
            @Override public boolean contains(int x, int y) { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(BOX_BORDER); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); g2.dispose();
            }
        };
        p.setBounds((FW - w) / 2, y, w, h); p.setOpaque(false); return p;
    }

    protected JPanel sectionBoxAt(int x, int y, int h, int arc, int w, Color fill) {
        JPanel p = new JPanel(null) {
            @Override public boolean contains(int x, int y) { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(BOX_BORDER); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); g2.dispose();
            }
        };
        p.setBounds(x, y, w, h); p.setOpaque(false); return p;
    }

    protected JLabel makeHeader(String text) {
        ImageIcon icon = new ImageIcon(loadGUI("tipbox_top.png"));
        int w = 450, ih = (int) ((double) icon.getIconHeight() * w / icon.getIconWidth());
        Image s = icon.getImage().getScaledInstance(w, ih, Image.SCALE_SMOOTH);
        String html = "<html><table width='400' align='center'><tr><td align='center'>" + text + "</td></tr></table></html>";
        JLabel l = new JLabel(html, new ImageIcon(s), SwingConstants.CENTER);
        l.setHorizontalTextPosition(JLabel.CENTER); l.setVerticalTextPosition(JLabel.CENTER);
        l.setSize(w, Math.max(ih, 55)); l.setForeground(Color.WHITE);
        l.setFont(SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(Font.PLAIN, 14f) : new Font("Arial", Font.BOLD, 14));
        return l;
    }

    protected SpecialLabel makeRoundedLabel(String text, int fontSize) {
        SpecialLabel lbl = new SpecialLabel(text, fontSize) {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        return lbl;
    }

    protected void addRoundedImage(String img, int x, int y, int w, int h) {
        Background bg = new Background(img, w, h) {
            @Override public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10); g2.dispose();
            }
        }; bg.setLocation(x, y); bg.setSize(w, h); contentPanel.add(bg);
    }

    protected void updateBar() {
        if (progPanel != null) progPanel.repaint();
    }

    protected void buildSidebar(int y, int h, Background back) {
        int sidebarBoxX = 10;
        int sidebarBoxW = SIDEBAR_W + 10;
        JPanel box = sectionBoxAt(sidebarBoxX, y, h, 15, sidebarBoxW, new Color(100, 0, 50, 220));
        back.add(box);

        sidebarPanel = new JPanel(null);
        sidebarPanel.setBounds(sidebarBoxX + 5, y + 10, SIDEBAR_W, h - 20);
        sidebarPanel.setOpaque(false);

        Font sf = SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(Font.BOLD, 13f) : new Font("Arial", Font.BOLD, 13);

        sidebarStepLabel = new JLabel();
        sidebarStepLabel.setFont(sf); sidebarStepLabel.setForeground(Color.WHITE);
        sidebarStepLabel.setBounds(8, 12, SIDEBAR_W - 16, 20);
        sidebarPanel.add(sidebarStepLabel);

        int maxSub = 0;
        for (int i = 0; i < getStepCount(); i++) {
            maxSub = Math.max(maxSub, getSubstepCount(i));
        }
        maxSub = Math.max(3, maxSub);
        sidebarSubLabels = new JLabel[maxSub];
        for (int i = 0; i < maxSub; i++) {
            sidebarSubLabels[i] = new JLabel();
            sidebarSubLabels[i].setFont(new Font("Arial", Font.PLAIN, 14));
            sidebarSubLabels[i].setBounds(8, 38 + i * 22, SIDEBAR_W - 16, 22);
            sidebarPanel.add(sidebarSubLabels[i]);
        }
        sidebarPanel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                int my = e.getY();
                int sc = getSubstepCount(currentStep);
                for (int i = 0; i < sc; i++) {
                    int sy = 38 + i * 18;
                    if (my >= sy && my < sy + 16 && i < currentSubstep) {
                        if (!confirmAbortDownload()) break;
                        showStep(currentStep, i); break;
                    }
                }
            }
        });
        back.add(sidebarPanel);
        back.setComponentZOrder(sidebarPanel, 0);
    }

    protected void updateSidebar() {
        if (sidebarStepLabel == null || sidebarSubLabels == null) return;
        int sc = getSubstepCount(currentStep);
        if (sc > sidebarSubLabels.length) {
            JLabel[] newLabels = new JLabel[sc];
            System.arraycopy(sidebarSubLabels, 0, newLabels, 0, sidebarSubLabels.length);
            for (int i = sidebarSubLabels.length; i < sc; i++) {
                newLabels[i] = new JLabel();
                newLabels[i].setFont(new Font("Arial", Font.PLAIN, 14));
                newLabels[i].setBounds(8, 38 + i * 22, SIDEBAR_W - 16, 22);
                sidebarPanel.add(newLabels[i]);
            }
            sidebarSubLabels = newLabels;
            sidebarPanel.revalidate();
        }
        sidebarStepLabel.setText("Step " + (currentStep + 1));
        for (int i = 0; i < sidebarSubLabels.length; i++) {
            if (i < sc) {
                sidebarSubLabels[i].setVisible(true);
                String p; Color c;
                if (i < currentSubstep) { p = "\u2713 "; c = Color.GRAY; }
                else if (i == currentSubstep) { p = "\u25CF "; c = new Color(0, 180, 0); }
                else { p = "\u25CB "; c = Color.WHITE; }
                sidebarSubLabels[i].setText(p + getSubstepName(currentStep, i));
                sidebarSubLabels[i].setForeground(c);
            } else {
                sidebarSubLabels[i].setVisible(false);
            }
        }
    }

    protected void buildBar(Background back, int barY) {
        int barH = 42;
        int contentBoxX = SIDEBAR_W + 30;
        int contentBoxW = FW - contentBoxX - 10;

        backBtn = new SpecialButton("< Back", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        backBtn.setEnabled(false);
        backBtn.addMouseListener(new MouseAdapter() { public void mouseReleased(MouseEvent e) { goBack(); } });
        back.add(backBtn);

        nextBtn = new SpecialButton("Next >", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 11);
        nextBtn.setEnabled(false);
        nextBtn.addMouseListener(new MouseAdapter() { public void mouseReleased(MouseEvent e) { advanceWithConfirm(); } });
        back.add(nextBtn);

        int stepCount = getStepCount();
        String[] chipNames = new String[stepCount];
        for (int i = 0; i < stepCount; i++) chipNames[i] = getChipLabel(i);
        int[] chipWidths = new int[stepCount];
        Font chipFont = SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(9f) : new Font("Arial", Font.PLAIN, 9);
        FontMetrics chipFm = getFontMetrics(chipFont);
        for (int i = 0; i < stepCount; i++) {
            chipWidths[i] = Math.max(40, Math.min(74, chipFm.stringWidth(chipNames[i]) + 16));
        }
        int gap = 12;
        int totalChipsW = 0; for (int w : chipWidths) totalChipsW += w;
        int totalNavW = totalChipsW + (stepCount - 1) * gap;

        progPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int chipH = 24;
                int totalW = 0; for (int w : chipWidths) totalW += w; totalW += (stepCount - 1) * gap;
                int chipY = (barH - chipH) / 2;
                int sx = (getWidth() - totalW) / 2;
                int cx = sx;
                for (int i = 0; i < stepCount; i++) {
                    int cw = chipWidths[i];
                    Color bg, fg;
                    if (i < currentStep) { bg = new Color(60, 60, 60); fg = Color.LIGHT_GRAY; }
                    else if (i == currentStep) {
                        if (stepInProgress) {
                            float pulse = (float)(Math.sin(animPhase) * 0.5 + 0.5);
                            int green = (int)(140 + pulse * 80);
                            bg = new Color(0, Math.min(green, 255), 0); fg = Color.WHITE;
                        } else { bg = new Color(0, 180, 0); fg = Color.WHITE; }
                    }
                    else { bg = new Color(40, 40, 40); fg = Color.WHITE; }
                    g2.setColor(bg); g2.fillRoundRect(cx, chipY, cw, chipH, 8, 8);
                    g2.setFont(SpecialLabel.baseFont != null ? SpecialLabel.baseFont.deriveFont(9f) : new Font("Arial", Font.PLAIN, 9));
                    g2.setColor(fg);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(chipNames[i], cx + (cw - fm.stringWidth(chipNames[i])) / 2, chipY + ((chipH - fm.getHeight()) / 2) + fm.getAscent());
                    if (i < stepCount - 1) { g2.setFont(new Font("Arial", Font.PLAIN, 12)); g2.setColor(Color.GRAY); g2.drawString(">", cx + cw + 5, chipY + chipH / 2 + 5); }
                    cx += cw + gap;
                }
                g2.dispose();
            }
        };
        progPanel.setOpaque(false);
        back.add(progPanel);
        progPanel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                int totalChipW = 0; for (int w : chipWidths) totalChipW += w; totalChipW += (stepCount - 1) * gap;
                int sx = (totalNavW - totalChipW) / 2;
                int mx = e.getX();
                int cx = sx;
                for (int i = 0; i < stepCount; i++) {
                    int cw = chipWidths[i];
                    if (mx >= cx && mx < cx + cw) {
                        if (!confirmAbortDownload()) break;
                        onChipClick(i);
                        break;
                    }
                    cx += cw + gap;
                }
            }
        });

        int btnH = 25;
        int itemY = barY + (barH - btnH) / 2;
        // center chips in pill, then center buttons in the left/right side gaps
        int chipsX = contentBoxX + (contentBoxW - totalNavW) / 2;
        progPanel.setBounds(chipsX, barY, totalNavW, barH);
        int chipRight = chipsX + totalNavW;
        int leftGap = chipsX - contentBoxX;
        int rightGap = (contentBoxX + contentBoxW) - chipRight;
        backBtn.setLocation(Math.max(contentBoxX, contentBoxX + (leftGap - backBtn.getWidth()) / 2), itemY);
        nextBtn.setLocation(Math.min(contentBoxX + contentBoxW - nextBtn.getWidth(), chipRight + (rightGap - nextBtn.getWidth()) / 2), itemY);
        back.add(sectionBoxAt(contentBoxX, barY, barH, 15, contentBoxW, new Color(100, 0, 50, 220)));
    }

    protected void onChipClick(int step) {
        if (step < currentStep) showStep(step, getSubstepCount(step) - 1);
        else if (step == currentStep) showStep(currentStep, 0);
        else showStep(step, 0);
    }

    protected boolean canAdvanceFrom(int step, int sub) {
        return true;
    }

    protected void showStep(int s, int sub) {
        currentStep = s;
        currentSubstep = sub;
        stepCompleted = false;
        stepInProgress = false;
        progressAnimator.stop();
        updateBar();
        updateSidebar();
        contentPanel.removeAll();
        buildContent(s, sub, contentPanel.getWidth());
        contentPanel.revalidate();
        contentPanel.repaint();
        backBtn.setEnabled(!(s == 0 && sub == 0));
        int sc = getSubstepCount(s);
        boolean lastSub = (sub >= sc - 1);
        nextBtn.setEnabled(s < getStepCount() - 1);
        nextBtn.changeText(s == getStepCount() - 1 ? "Finish" : "Next >");
        updateStatusText(s, sub);
    }

    protected void advance() {
        if (currentStep >= getStepCount()) { dispose(); return; }
        if (!canAdvanceFrom(currentStep, currentSubstep)) return;
        int sc = getSubstepCount(currentStep);
        if (currentSubstep < sc - 1) showStep(currentStep, currentSubstep + 1);
        else showStep(currentStep + 1, 0);
    }

    protected void advanceWithConfirm() {
        if (!confirmAbortDownload()) return;
        if (downloader != null) downloader.cancelDownload();
        advance();
    }

    protected void goBack() {
        if (!confirmAbortDownload()) return;
        if (currentSubstep > 0) showStep(currentStep, currentSubstep - 1);
        else if (currentStep > 0) showStep(currentStep - 1, getSubstepCount(currentStep - 1) - 1);
    }

    protected void resetAfterError(SpecialButton btn) {
        stepInProgress = false;
        progressAnimator.stop();
        btn.setEnabled(true);
        dlProgressLabel.setText("Patch failed. Try again.");
        progPanel.repaint();
        statusBarBox.repaint();
    }
}
