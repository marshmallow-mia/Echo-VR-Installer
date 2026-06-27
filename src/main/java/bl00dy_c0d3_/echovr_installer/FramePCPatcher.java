package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FramePCPatcher extends JDialog {
    private static final int ITEM_GAP = 6;
    private static final Color SECTION_BOX_FILL = new Color(200, 0, 150, 40);
    private static final Color SECTION_BOX_BORDER = new Color(50, 50, 50, 150);
    private static final int SECTION_PADDING = 10;
    private static final int HEADER_CONTENT_OFFSET = 49;
    int frameWidth = 700;
    int frameHeight = 394;
    String path = "C:/EchoVR/ready-at-dawn-echo-arena";
    Downloader downloadPatch;
    FramePCPatcher outFrame = this;
    private Runnable onPatchComplete;

    // Step indicators
    private JLabel[] stepLabels = new JLabel[6];
    private int currentStep = 1;

    // Constructor
    public FramePCPatcher(){
        initComponents();
        this.setVisible(true);
    }

    public FramePCPatcher(Runnable onPatchComplete) {
        this.onPatchComplete = onPatchComplete;
        initComponents();
        this.setVisible(true);
    }

    private void initComponents(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.9.4b-005");
        this.setModal(true);

        Background back = new Background("EchoArena.jpg");
        back.setLayout(null);
        this.setContentPane(back);

        TipBox tipBox = new TipBox();

        int centerX = frameWidth / 2;
        int y = 10;

        // === Step 1: Join Discord ===
        String step1Text = "1. Join the Echo VR Patcher Discord Server:";
        JLabel step1Label = new JLabel(step1Text);
        step1Label.setBounds(10, y, frameWidth - 20, 20);
        step1Label.setForeground(Color.WHITE);
        step1Label.setFont(new Font("Arial", Font.BOLD, 13));
        back.add(step1Label);
        y += 22;

        SpecialHyperlink hyperlinkPC = new SpecialHyperlink(
            (frameWidth - 400) / 2, y,
            "Click here to join the Echo VR Patcher Discord-Server",
            "https://discord.gg/bMpsva6fmA", 12);
        back.add(hyperlinkPC);
        y += 28;

        // === Step 2: React and Copy ===
        JLabel step2Label = new JLabel("2. React to the message and copy the link:");
        step2Label.setBounds(10, y, frameWidth - 20, 20);
        step2Label.setForeground(Color.WHITE);
        step2Label.setFont(new Font("Arial", Font.BOLD, 13));
        back.add(step2Label);
        y += 22;

        // Images side by side centered
        Background reactImg = new Background("pc_react.png") {
            @Override public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        reactImg.setLocation(centerX - 95, y);
        reactImg.setSize(90, 55);
        back.add(reactImg);

        Background copyImg = new Background("copy_linkPC.png", 120, -1) {
            @Override public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        copyImg.setLocation(centerX + 5, y);
        copyImg.setSize(90, 55);
        back.add(copyImg);
        y += 60;

        // === Step 3: Paste ===
        JLabel step3Label = new JLabel("3. Paste the link (CTRL-V):");
        step3Label.setBounds(10, y, frameWidth - 20, 20);
        step3Label.setForeground(Color.WHITE);
        step3Label.setFont(new Font("Arial", Font.BOLD, 13));
        back.add(step3Label);
        y += 22;

        SpecialTextfield textfieldPCPatchLink = new SpecialTextfield();
        textfieldPCPatchLink.specialTextfield(480, 25, centerX - 240, y, 12);
        back.add(textfieldPCPatchLink);
        y += 32;

        // === Step 4: Choose path ===
        JLabel step4Label = new JLabel("4. Choose your Echo installation path:");
        step4Label.setBounds(10, y, frameWidth - 20, 20);
        step4Label.setForeground(Color.WHITE);
        step4Label.setFont(new Font("Arial", Font.BOLD, 13));
        back.add(step4Label);
        y += 22;

        SpecialLabel labelPcPatchDownloadPath = new SpecialLabel(path, 12);
        labelPcPatchDownloadPath.setLocation(centerX - 225, y + 30);
        labelPcPatchDownloadPath.setSize(450, 22);
        labelPcPatchDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcPatchDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcPatchDownloadPath);

        SpecialButton pcChooseOriginalPath = new SpecialButton(
            "<html>Auto choose original<br>Oculus path</html>",
            "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 12);
        pcChooseOriginalPath.setLocation(centerX - 150, y);
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForAdminAndOculusPath(outFrame);
                if (!newPath.matches("")) {
                    labelPcPatchDownloadPath.setText(newPath + "Software\\Software\\ready-at-dawn-echo-arena");
                    outFrame.repaint();
                }
            }
        });
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) { tipBox.showTip("Auto-find Oculus installation path"); }
            public void mouseExited(MouseEvent event) { tipBox.showDefault(); }
        });
        back.add(pcChooseOriginalPath);

        SpecialButton pcPatchChoosePath = new SpecialButton("Choose path",
            "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 12);
        pcPatchChoosePath.setLocation(centerX + 10, y);
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcPatchDownloadPath, outFrame);
            }
        });
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) { tipBox.showTip("Choose a custom folder"); }
            public void mouseExited(MouseEvent event) { tipBox.showDefault(); }
        });
        back.add(pcPatchChoosePath);
        y += 58;

        // === Step 5: Start Patching ===
        JLabel step5Label = new JLabel("5. Start Patching:");
        step5Label.setBounds(10, y, frameWidth - 20, 20);
        step5Label.setForeground(Color.WHITE);
        step5Label.setFont(new Font("Arial", Font.BOLD, 13));
        back.add(step5Label);
        y += 22;

        SpecialLabel patchProgress = new SpecialLabel(" 0%", 16);
        patchProgress.setHorizontalAlignment(SwingConstants.LEFT);
        patchProgress.setLocation(centerX + 70, y);
        patchProgress.setSize(80, 30);
        patchProgress.setBackground(new Color(255, 255, 255, 200));
        patchProgress.setForeground(Color.BLACK);
        back.add(patchProgress);

        SpecialButton pcStartPatch = new SpecialButton("Start patching",
            "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        pcStartPatch.setLocation(centerX - 130, y);
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String link = textfieldPCPatchLink.getText();
                if (link.matches("https://cdn.discordapp.com/attachments/.*/pnsovr.dll.*")){
                    File echoPath = new File(labelPcPatchDownloadPath.getText() + "/bin/win10");
                    if (!echoPath.exists() && !echoPath.isDirectory()) {
                        ErrorDialog error = new ErrorDialog();
                        error.errorDialog(outFrame, "Incorrect path to EchoVR", "Error: Choose the main directory of Echo. Like: C:\\echovr\\ready-at-dawn-echo-arena", 0);
                    } else {
                        if (downloadPatch != null){ downloadPatch.cancelDownload(); pause(1); }
                        pcStartPatch.changeText("Restart Patching");
                        System.out.println(link);
                        downloadPatch = new Downloader();
                        downloadPatch.setOnCompleteListener(() -> {
                            if (onPatchComplete != null) {
                                SwingUtilities.invokeLater(onPatchComplete);
                            }
                        });
                        downloadPatch.startDownload(link, echoPath + "", "pnsovr.dll", patchProgress, outFrame, null, 3, true, -1, false);
                    }
                } else {
                    ErrorDialog error = new ErrorDialog();
                    error.errorDialog(outFrame, "Wrong URL provided", "Your provided Download Link is wrong. Please check!", 0);
                }
            }
        });
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) { tipBox.showTip("Start patching Echo VR"); }
            public void mouseExited(MouseEvent event) { tipBox.showDefault(); }
        });
        back.add(pcStartPatch);
        y += 35;

        // === Progress bar ===
        String[] stepNames = {"Join Discord", "React", "Copy Link", "Paste", "Path", "Patch"};
        Color stepBg = new Color(50, 50, 50);
        for (int i = 0; i < 6; i++) {
            stepLabels[i] = new JLabel((i+1) + ". " + stepNames[i], SwingConstants.CENTER);
            stepLabels[i].setBounds(8 + i * 56, frameHeight - 68, 52, 18);
            stepLabels[i].setFont(new Font("Arial", Font.PLAIN, 9));
            stepLabels[i].setForeground(Color.LIGHT_GRAY);
            stepLabels[i].setOpaque(true);
            stepLabels[i].setBackground(stepBg);
            back.add(stepLabels[i]);

            // Arrow between steps
            if (i < 5) {
                JLabel arrow = new JLabel("\u2192", SwingConstants.CENTER);
                arrow.setBounds(56 + i * 56, frameHeight - 68, 12, 18);
                arrow.setFont(new Font("Arial", Font.PLAIN, 9));
                arrow.setForeground(Color.GRAY);
                back.add(arrow);
            }
        }
        // Highlight first step
        stepLabels[0].setForeground(new Color(0, 255, 0));

        // === TipBox ===
        back.add(tipBox);
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 40);

        // === Section panel around all content ===
        int[] bounds = calcBounds(SECTION_PADDING,
            new int[]{5, 5, frameWidth - 15, frameHeight - 80});
        back.add(createSectionPanel(bounds[0], bounds[1], bounds[2], bounds[3]));

        // Finish
        this.pack();
        this.setSize(frameWidth, frameHeight);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((d.width - frameWidth) / 2, (d.height - frameHeight) / 2);
    }

    // --- Helper methods ---

    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }

    private int[] calcBounds(int pad, int[]... rects) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxR = Integer.MIN_VALUE, maxB = Integer.MIN_VALUE;
        for (int[] r : rects) {
            minX = Math.min(minX, r[0]);
            minY = Math.min(minY, r[1]);
            maxR = Math.max(maxR, r[0] + r[2]);
            maxB = Math.max(maxB, r[1] + r[3]);
        }
        return new int[]{minX - pad, minY - pad, maxR - minX + pad * 2, maxB - minY + pad * 2};
    }

    private JPanel createSectionPanel(int x, int y, int w, int h) {
        JPanel panel = new JPanel(null) {
            @Override public boolean contains(int x, int y) { return false; }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SECTION_BOX_FILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), SECTION_PADDING, SECTION_PADDING);
                g2.setColor(SECTION_BOX_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, SECTION_PADDING, SECTION_PADDING);
                g2.dispose();
            }
        };
        panel.setBounds(x, y, w, h);
        return panel;
    }
}
