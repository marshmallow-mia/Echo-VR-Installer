package bl00dy_c0d3_.echovr_installer;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;


import static bl00dy_c0d3_.echovr_installer.Helpers.*;

// TODO: Remove in v0.9.0 — replaced by FrameGuidanceQuest
/**
 * @deprecated Replaced by {@link FrameGuidanceQuest}. Will be removed in a future version.
 */
@Deprecated
public class FrameQuestPatcher extends JDialog {
    private static final int SECTION_PADDING = 20;
    private static final int ITEM_GAP = 20;
    private static final Color SECTION_BOX_FILL = new Color(200, 0, 150, 40);
    private static final Color SECTION_BOX_BORDER = new Color(50, 50, 50, 150);
    private static final int HEADER_CONTENT_OFFSET = 49;
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;
    private static final String DEFAULT_PATH = "C:\\EchoVR";
    private final Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    static boolean isChrome = checkIfChromeOs();
    private SpecialTextfield textfieldQuestPatchLink;
    private SpecialLabel labelConfigPath;
    private SpecialCheckBox checkBoxConfig;
    private Downloader downloader = null;
    private Downloader downloader2 = null;
    SpecialButton questStartDownload;
    JDialog outFrame = this;

    public FrameQuestPatcher() {
        initComponents();
        this.setVisible(true);
    }


    @Override
    public void dispose() {
        if (downloader != null) {
            downloader.cancelDownload();
        }
        if (downloader2 != null) {
            downloader2.cancelDownload();
        }
        super.dispose();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setIconImage(loadGUI("icon.png"));
        setTitle("Echo VR Installer v0.9.4b.002");

        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setModal(true);
        setContentPane(createContentPane());

        pack();
        centerFrame(this, FRAME_WIDTH, FRAME_HEIGHT);
    }

    private @NotNull JPanel createContentPane() {
        Background back = new Background("echo-vr-cierra.png");
        back.setLayout(null);

        addSpecialLabels(back);
        addSpecialHyperlinks(back);
        addImages(back);
        addTextFields(back);

        //Tipbox erstellen (muss vor den Buttons sein, damit hover listener darauf zugreifen kÃ¶nnen)
        TipBox tipBox = new TipBox();
        addStartDownloadButton(back, tipBox);
        addChooseConfigButton(back);
        addStartPatchingButton(back, tipBox);

        //Tipbox positionieren und hinzufÃ¼gen...
        tipBox.setLocation((FRAME_WIDTH - tipBox.getWidth()) / 2 + 450, FRAME_HEIGHT - tipBox.getHeight() - 45);
        back.add(tipBox);

        addCheckBoxes(back);

        // --- Section Panels (insert behind all content) ---
        // Section 1: step1 header (40,40,500,80) + hyperlink (40,120,300,25)
        int[] s1 = calcBounds(SECTION_PADDING,
            new int[]{50, 40, 500, 55},
            new int[]{60, 60 + HEADER_CONTENT_OFFSET + ITEM_GAP, 300, 25});
        back.add(createSectionPanel(s1[0], s1[1], s1[2], s1[3]));

        // Section 2: step2 header (40,150,500,80) + questReactImg (199,231,182,108)
        int[] s2 = calcBounds(SECTION_PADDING,
            new int[]{50, 184, 500, 55},
            new int[]{209, 184 + HEADER_CONTENT_OFFSET + ITEM_GAP, 182, 108});
        back.add(createSectionPanel(s2[0], s2[1], s2[2], s2[3]));

        // Section 3: step3 header (40,352,500,80) + copyLinkQuestImg (150,433,279,177)
        int[] s3 = calcBounds(SECTION_PADDING,
            new int[]{50, 411, 500, 55},
            new int[]{160, 411 + HEADER_CONTENT_OFFSET + ITEM_GAP, 279, 177});
        back.add(createSectionPanel(s3[0], s3[1], s3[2], s3[3]));

        // Section 4: step4 header (582,40,500,80) + textfield (582,113,630,30)
        int[] s4 = calcBounds(SECTION_PADDING,
            new int[]{602, 40, 500, 55},
            new int[]{602, 40 + HEADER_CONTENT_OFFSET + ITEM_GAP, 630, 30});
        back.add(createSectionPanel(s4[0], s4[1], s4[2], s4[3]));

        // Section 5: step5 header (582,170,500,80) + questStartDownload (582,210,212,38)
        //           + "Progress =" (810,210,130,38) + labelQuestProgress2 (940,210,130,19)
        //           + labelQuestProgress3 (940,229,130,19)
        int[] s5 = calcBounds(SECTION_PADDING,
            new int[]{602, 189, 500, 55},
            new int[]{602, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP, 212, 38},
            new int[]{830, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP, 130, 38},
            new int[]{960, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP, 130, 19},
            new int[]{960, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP + 19, 130, 19});
        back.add(createSectionPanel(s5[0], s5[1], s5[2], s5[3]));

        // Section 6: step6 header (582,300,500,80) + pcStartPatch (585,340,282,50)
        //           + labelQuestProgress4 (885,340,300,50)
        int[] s6 = calcBounds(SECTION_PADDING,
            new int[]{602, 346, 500, 55},
            new int[]{605, 346 + HEADER_CONTENT_OFFSET + ITEM_GAP, 282, 50},
            new int[]{905, 346 + HEADER_CONTENT_OFFSET + ITEM_GAP, 300, 50});
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
                else if (cur == 3) { tipBox.showTip("Right Click on the blue URL and select Copy Link. NOT COPY MESSAGE LINK!"); }
                else if (cur == 4) { /* tipBox.showTip("Section 4 tip"); */ }
                else if (cur == 5) { /* tipBox.showTip("Section 5 tip"); */ }
                else if (cur == 6) { /* tipBox.showTip("Section 3 tip"); */ }
                else { tipBox.showDefault(); }
            }
        });

        return back;
    }

    private void handleDownloadButtonClick() {

        if (textfieldQuestPatchLink.getText().matches("https://files.echovr.de.*")) {
            JOptionPane.showMessageDialog(null, "The Download will start after pressing OK. Please wait for both files to be done!", "Download started", JOptionPane.INFORMATION_MESSAGE);
            if (downloader != null){
                downloader.cancelDownload();
                System.out.println("downloader1 stopped");
            }
            if (downloader2 != null){
                downloader2.cancelDownload();
                System.out.println("downloader2 stopped");
            }
            questStartDownload.changeText("Restart Download");



            Thread downloadThread1 = new Thread(() -> {
                downloader = new Downloader();
                String fixedURL = textfieldQuestPatchLink.getText().replace("org", "org/dl");
                downloader.startDownload(fixedURL, targetPath.toString(), "personilizedechoapk.apk", labelQuestProgress2, this, null, 2, true, -1, false);
            });

            downloadThread1.start();  // This runs the download in a separate thread





            pause(1);


            Thread downloadThread2 = new Thread(() -> {
                downloader2 = new Downloader();
                            downloader2.startDownload("_data.zip", targetPath.toString(), "_data.zip", labelQuestProgress3, this, null, 2, false, 0, false);
            });

            downloadThread2.start();  // This runs the download in a separate thread





        } else {
            new ErrorDialog().errorDialog(this, "Wrong URL provided", "Your provided Download Link is wrong. Please check!", 0);
        }
    }

    private void handleChooseConfigClick() {
        jsonFileChooser(labelConfigPath, outFrame);
    }

    private void handlePatchingButtonClick() {
        labelQuestProgress4.setText("Installation started! Wait!");
        outFrame.repaint();
        JOptionPane.showMessageDialog(outFrame, "<html>Press OK to start the installation. It can take a minute to install!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
        String apkfileName;
        apkfileName = "personilizedechoapk.apk";

        InstallerQuest installtoQuest = new InstallerQuest();
        boolean installState = installtoQuest.installAPK(targetPath.toString(), apkfileName, "_data.zip", labelQuestProgress4, this);

        if (installState) {
            labelQuestProgress4.setText("Installation is complete!");
            outFrame.repaint();
            JOptionPane.showMessageDialog(outFrame, "<html>Installation of Echo is done. You can start it now on your Quest.<br> DON'T CLICK ON RESTORE IF YOU WILL GET ASKED TO OR YOU NEED TO REINSTALL AGAIN!</html>", "Have Fun!", JOptionPane.INFORMATION_MESSAGE);
        }
        else{
            labelQuestProgress4.setText("Installation did not finish!");
            outFrame.repaint();
        }
    }





    SpecialLabel labelQuestProgress2 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelQuestProgress3 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelQuestProgress4 = new SpecialLabel("Not started yet", 18);

    private void addSpecialLabels(@NotNull JPanel back) {
        back.add(createStepHeader("1. Join the Echo VR Patcher Discord Server:", 50, 40));
        back.add(createStepHeader("2. React to the message on Discord\nby clicking on the smiley:", 50, 184));

        back.add(createStepHeader("3. You will receive a Message from the\n\"EchoSignUp\" Bot. Copy that.", 50, 411));

        back.add(createStepHeader("4. Paste the link with CTRL-V:", 592, 40));
        back.add(createStepHeader("5. Start the Download Process:", 592, 189));
        //back.add(Helpers.createSpecialLabel("Error 204", 16, 582, 295));
        back.add(createStepHeader("6. After the Download above is finished, click this button:", 592, 346));
        back.add(Helpers.createSpecialLabel("Progress = ", 17, 830, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP, new Dimension(130, 38), Color.BLACK, Color.WHITE));

        String configPath = "Error 204";
        labelConfigPath = Helpers.createSpecialLabel(configPath, 14, 602, 455, new Dimension(600, 25), Color.BLACK, Color.WHITE);
        //back.add(labelConfigPath);

        //THIS NEED TO BE SET MANUALLY, AS I NEED TO ACCESS IT LATER
        labelQuestProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress2.setLocation(960, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        labelQuestProgress2.setSize(130, 19);
        labelQuestProgress2.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress2.setForeground(Color.BLACK);
        back.add(labelQuestProgress2);

        labelQuestProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress3.setLocation(960, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP + 19);
        labelQuestProgress3.setSize(130, 19);
        labelQuestProgress3.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress3.setForeground(Color.BLACK);
        back.add(labelQuestProgress3);

        labelQuestProgress4.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress4.setLocation(905, 346 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        labelQuestProgress4.setSize(300, 50);
        labelQuestProgress4.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress4.setForeground(Color.BLACK);
        back.add(labelQuestProgress4);

    }

    private void addSpecialHyperlinks(@NotNull JPanel back) {
        back.add(new SpecialHyperlink(50, 60 + HEADER_CONTENT_OFFSET + ITEM_GAP, "Click on me to join the Echo VR Patcher Discord-Server", "https://discord.gg/bMpsva6fmA", 14));
    }

    private void addImages(JPanel back) {
        Background questReactImg = new Background("quest_react.png") {
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
        questReactImg.setLocation(209, 184 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        questReactImg.setSize(182, 108);
        questReactImg.setVisible(true);
        back.add(questReactImg);

        Background copyLinkQuestImg = new Background("copy_linkQuest.png") {
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
        copyLinkQuestImg.setLocation(160, 411 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        copyLinkQuestImg.setSize(279, 177);
        copyLinkQuestImg.setVisible(true);
        back.add(copyLinkQuestImg);
    }

    private void addTextFields(@NotNull JPanel back) {
        textfieldQuestPatchLink = new SpecialTextfield();
        textfieldQuestPatchLink.specialTextfield(630, 30, 602, 40 + HEADER_CONTENT_OFFSET + ITEM_GAP, 13);
        back.add(textfieldQuestPatchLink);
    }



    private void addCheckBoxes(@NotNull JPanel back) {
        checkBoxConfig = new SpecialCheckBox("Error 204", 17);
        checkBoxConfig.setSize(500, 30);
        checkBoxConfig.setLocation(602, 525);
        checkBoxConfig.setOpaque(true);
        checkBoxConfig.setBackground(new Color(50, 50, 50));
        //back.add(checkBoxConfig);
    }

    private void addStartDownloadButton(@NotNull JPanel back, @NotNull TipBox tipBox) {
        questStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        questStartDownload.setLocation(602, 189 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        questStartDownload.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                handleDownloadButtonClick();
            }
        });
        questStartDownload.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Start the download of the personalized Echo VR APK and data files");
            }
            @Override
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(questStartDownload);
    }


    private void addChooseConfigButton(@NotNull JPanel back) {
        SpecialButton chooseConfig = new SpecialButton("Error 204", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        chooseConfig.setLocation(602, 405);
        chooseConfig.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                handleChooseConfigClick();
            }
        });
        //back.add(chooseConfig);
    }

    private void addStartPatchingButton(@NotNull JPanel back, @NotNull TipBox tipBox) {
        SpecialButton pcStartPatch = new SpecialButton("Start patching", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        pcStartPatch.setLocation(605, 346 + HEADER_CONTENT_OFFSET + ITEM_GAP);
        pcStartPatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                handlePatchingButtonClick();
            }
        });
        pcStartPatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Install the downloaded APK and data files onto your Quest device");
            }
            @Override
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcStartPatch);
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
