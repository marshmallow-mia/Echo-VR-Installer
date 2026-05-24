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
    private static final int ITEM_GAP = 10;
    private static final Color SECTION_BOX_FILL = new Color(200, 0, 150, 40);
    private static final Color SECTION_BOX_BORDER = new Color(50, 50, 50, 150);
    // Header: tipbox_top.png 802Ã—72 â†’ scaled to 500Ã—44, label=55, image centered:
    // content bottom = headerY + (55-44)/2 + 44 = headerY + 5 + 44 = headerY + 49
    private static final int SECTION_PADDING = 10;
    private static final int HEADER_CONTENT_OFFSET = 49;
    int frameWidth = 1250;
    int frameHeight = 720;
    String path = "C:/EchoVR/ready-at-dawn-echo-arena";
    //TODO use already used path from FramePCPatcher
    Downloader downloadPatch;
    FramePCPatcher outFrame = this;


    //Constructor
    public FramePCPatcher(){
        initComponents();
        this.setVisible(true);
    }

    private void initComponents(){

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.8.9");
        this.setModal(true);

        Background back = new Background("echo-in-arena.png");
        back.setLayout(null);
        this.setContentPane(back);

        //Tipbox erstellen (muss vor den Buttons sein, damit hover listener darauf zugreifen kÃ¶nnen)
        TipBox tipBox = new TipBox();




        back.add(createStepHeader("1. Join the Echo VR Patcher Discord Server:", 40, 40));


        //SpecialHyperlink hyperlinkPC = new SpecialHyperlink(60, 70, "<html><a href=''>Join the Echo VR Patcher Discord-Server</a></html>", "https://discord.gg/bMpsva6fmA");
        SpecialHyperlink hyperlinkPC = new SpecialHyperlink(40, 40 + HEADER_CONTENT_OFFSET + ITEM_GAP, "Click on me to join the Echo VR Patcher Discord-Server", "https://discord.gg/bMpsva6fmA", 14);
        back.add(hyperlinkPC);

        back.add(createStepHeader("2. React to the message on Discord \nby clicking on the disc:", 40, 185));

        Background reactToMessageImg = new Background("pc_react.png") {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        back.setLayout(null);
        reactToMessageImg.setLocation(25 + (530 - reactToMessageImg.getWidth()) / 2, 185 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        reactToMessageImg.setSize(182,108);
        reactToMessageImg.setVisible(true);
        back.add(reactToMessageImg);


        back.add(createStepHeader("3. Right Click the file and select Copy Link \n- NOT COPY MESSAGE LINK!", 40, 418));


        Background copyLinkImg = new Background("copy_linkPC.png", 240, -1) {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
                super.paintComponent(g2);
                g2.setColor(new Color(50, 50, 50, 255));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        back.setLayout(null);
        copyLinkImg.setLocation(25 + (530 - copyLinkImg.getWidth()) / 2, 418 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        copyLinkImg.setVisible(true);
        back.add(copyLinkImg);

        back.add(createStepHeader("4. Paste the link with CTRL-V:", 582, 40));

        SpecialTextfield textfieldPCPatchLink = new SpecialTextfield();
        textfieldPCPatchLink.specialTextfield(630, 30, 582, 40 + HEADER_CONTENT_OFFSET + ITEM_GAP, 13);
        back.add(textfieldPCPatchLink);

        back.add(createStepHeader("5. Choose your Echo installation path:", 582, 195));


        SpecialLabel labelPcPatchDownloadPath = new SpecialLabel(path, 14);
        labelPcPatchDownloadPath.setLocation(740, 195 + HEADER_CONTENT_OFFSET + ITEM_GAP + 38 + ITEM_GAP);
        labelPcPatchDownloadPath.setSize(450, 25);
        labelPcPatchDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcPatchDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcPatchDownloadPath);


        SpecialButton pcChooseOriginalPath = new SpecialButton("<html>Auto choose original<br>Oculus path</html>", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        pcChooseOriginalPath.setLocation(582, 195 + HEADER_CONTENT_OFFSET + ITEM_GAP);
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
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Choose this to use the original Oculus path. It automatically finds the original Oculus installation path");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });

        back.add(pcChooseOriginalPath);


        SpecialButton pcPatchChoosePath = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 14);
        pcPatchChoosePath.setLocation(582, 195 + HEADER_CONTENT_OFFSET + ITEM_GAP + 38 + ITEM_GAP);
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcPatchDownloadPath, outFrame);
            }
        });
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Choose a custom folder for your Echo VR installation");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });

        back.add(pcPatchChoosePath);

        back.add(createStepHeader("6. Start Patching by pressing this button:", 582, 405));


        SpecialLabel patchProgress = new SpecialLabel(" 0%", 18);
        patchProgress.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        patchProgress.setLocation(887, 405 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        patchProgress.setSize(100, 50);
        patchProgress.setBackground(new Color(255, 255, 255, 200));
        patchProgress.setForeground(Color.BLACK);
        back.add(patchProgress);

        SpecialButton pcStartPatch = new SpecialButton("Start patching", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        pcStartPatch.setLocation(585, 405 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String link = textfieldPCPatchLink.getText();

                if (link.matches("https://cdn.discordapp.com/attachments/.*/pnsovr.dll.*")){
                    File echoPath = new File(labelPcPatchDownloadPath.getText() + "/bin/win10");
                    if (!echoPath.exists() && !echoPath.isDirectory()) {
                        ErrorDialog error = new ErrorDialog();
                        error.errorDialog(outFrame, "Incorrect path to EchoVR", "Error: Choose the main directory of Echo. Like: C:\\echovr\\ready-at-dawn-echo-arena", 0);
                    }
                    else {
                        if (downloadPatch != null){
                            downloadPatch.cancelDownload();
                            pause(1);
                        }
                        pcStartPatch.changeText("Restart Patching");

                        System.out.println(link);
                        downloadPatch = new Downloader();
                        downloadPatch.startDownload(textfieldPCPatchLink.getText(), echoPath + "", "pnsovr.dll", patchProgress, outFrame, null, 3, true, -1, false);
                    }
                }
                else{
                    ErrorDialog error = new ErrorDialog();
                    error.errorDialog(outFrame, "Wrong URL provided", "Your provided Download Link is wrong. Please check!", 0);

                }


            }
        });
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Start patching Echo VR with the downloaded DLL file");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });

        back.add(pcStartPatch);

        //Tipbox positionieren und hinzufÃ¼gen...
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2 + 450, frameHeight - tipBox.getHeight() - 45);
        back.add(tipBox);

        // --- Section Panels (insert behind all content) ---

        // Section 1: step1 header (40,40,500,80) + hyperlink (40,125,360,25)
        int[] s1 = calcBounds(SECTION_PADDING,
            new int[]{40, 40, 500, 80},
            new int[]{40, 40 + HEADER_CONTENT_OFFSET + ITEM_GAP, 360, 25});
        // Fill panel (paints behind content)
        back.add(createSectionPanel(s1[0], s1[1], s1[2], s1[3]));

        // Section 2: step2 header (40,185,500,80) + reactToMessageImg (199,270,182,108)
        int[] s2 = calcBounds(SECTION_PADDING,
            new int[]{40, 185, 500, 80},
            new int[]{199, 185 + HEADER_CONTENT_OFFSET + ITEM_GAP, 182, 108});
        // Fill panel (paints behind content)
        back.add(createSectionPanel(s2[0], s2[1], s2[2], s2[3]));

        // Section 3: step3 header (40,418,500,80) + copyLinkImg
        int cw = copyLinkImg.getWidth();
        int ch = copyLinkImg.getHeight();
        int cx = copyLinkImg.getX();
        int[] s3 = calcBounds(SECTION_PADDING,
            new int[]{40, 418, 500, 80},
            new int[]{cx, 418 + HEADER_CONTENT_OFFSET + ITEM_GAP, cw, ch});
        // Fill panel (paints behind content)
        back.add(createSectionPanel(s3[0], s3[1], s3[2], s3[3]));

        // Section 4: step4 header (582,40,500,80) + textfield (582,125,630,30)
        int[] s4 = calcBounds(SECTION_PADDING,
            new int[]{582, 40, 500, 80},
            new int[]{582, 40 + HEADER_CONTENT_OFFSET + ITEM_GAP, 630, 30});
        // Fill panel (paints behind content)
        back.add(createSectionPanel(s4[0], s4[1], s4[2], s4[3]));

        // Section 5: step5 header (582,195,500,80) + pcChooseOriginalPath (582,280,210,38)
        //           + pcPatchChoosePath (582,340,140,38) + labelPcPatchDownloadPath (740,340,450,25)
        int[] s5 = calcBounds(SECTION_PADDING,
            new int[]{582, 195, 500, 80},
            new int[]{582, 195 + HEADER_CONTENT_OFFSET + ITEM_GAP, 210, 38},
            new int[]{582, 195 + HEADER_CONTENT_OFFSET + ITEM_GAP + 38 + ITEM_GAP, 140, 38},
            new int[]{740, 195 + HEADER_CONTENT_OFFSET + ITEM_GAP + 38 + ITEM_GAP, 450, 25});
        // Fill panel (paints behind content)
        back.add(createSectionPanel(s5[0], s5[1], s5[2], s5[3]));

        // Section 6: step6 header (582,405,500,80) + pcStartPatch (585,490,210,50) + patchProgress (887,490,100,50)
        int[] s6 = calcBounds(SECTION_PADDING,
            new int[]{582, 405, 500, 80},
            new int[]{585, 405 + HEADER_CONTENT_OFFSET + ITEM_GAP, 210, 50},
            new int[]{887, 405 + HEADER_CONTENT_OFFSET + ITEM_GAP, 100, 50});
        // Fill panel (paints behind content)
        back.add(createSectionPanel(s6[0], s6[1], s6[2], s6[3]));

        // --- Section hover detection via mouse motion (does not block button events) ---
        final int[][] secs = {s1, s2, s3, s4, s5, s6};
        back.addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                int cur = -1;
                for (int i = 0; i < 6; i++) {
                    int[] s = secs[i];
                    if (mx >= s[0] && mx <= s[0]+s[2] && my >= s[1] && my <= s[1]+s[3]) {
                        cur = i + 1; break;
                    }
                }
                if (cur == 1)      { /* tipBox.showTip("Section 1 tip"); */ }
                else if (cur == 2) { /* tipBox.showTip("Section 2 tip"); */ }
                else if (cur == 3) { /* tipBox.showTip("Section 3 tip"); */ }
                else if (cur == 4) { tipBox.showTip("Section 4 tip"); }
                else if (cur == 5) { tipBox.showTip("Section 5 tip"); }
                else if (cur == 6) { tipBox.showTip("Section 6 tip"); }
                else { tipBox.showDefault(); }
            }
        });

        //Alles fertig machen...
        this.pack();
        System.out.println(this.getInsets());
        //(this.getHeight() - this.getInsets().top)
        //TODO set positions correctly everywhere

        //Fenstergröße und Position setzen...
        this.setSize(frameWidth, frameHeight);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (d.width - this.getWidth()) / 2;
        int y = (d.height - this.getHeight()) / 2;
        this.setLocation(x, y);
    }



    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }

    private JLabel createStepHeader(String text, int x, int y) {
        ImageIcon icon = new ImageIcon(loadGUI("tipbox_top.png"));
        int w = 500;
        int imgH = (int) ((double) icon.getIconHeight() * w / icon.getIconWidth());
        Image scaled = icon.getImage().getScaledInstance(w, imgH, Image.SCALE_SMOOTH);
        // HTML wrapping for multi-line text support
        String htmlText = "<html><table width='460' align='center'><tr><td align='center'>" + text.replace("\n", "<br>") + "</td></tr></table></html>";
        JLabel label = new JLabel(htmlText, new ImageIcon(scaled), SwingConstants.CENTER);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.CENTER);
        // Give enough height for multi-line text (at least image height, more for long text)
        int labelH = Math.max(imgH, 55);
        label.setBounds(x, y, w, labelH);
        label.setForeground(Color.WHITE);
        try {
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("conthrax-sb.otf");
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                label.setFont(font.deriveFont(Font.PLAIN, 15f));
                fontStream.close();
            } else {
                label.setFont(new Font("Arial", Font.BOLD, 15));
            }
        } catch (Exception e) {
            label.setFont(new Font("Arial", Font.BOLD, 15));
        }
        return label;
    }

    // Berechnet den umschließenden Rechteck aus mehreren Content-Rechtecken + Padding
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

    // Erstellt ein Section-Panel mit purpurnem Hintergrund und abgerundeten Ecken
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
