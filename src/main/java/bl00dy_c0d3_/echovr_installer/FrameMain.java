package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameMain extends JFrame {
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;
    private TipBox tipBox;

    public FrameMain() {
        initComponents();
        this.setVisible(true);
    }


    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try { javafx.application.Platform.exit(); } catch (Exception ignored) {}
                System.exit(0);
            }
        });
        setResizable(false);
        setIconImage(loadGUI("icon.png"));
        setTitle("Echo VR Installer v0.9.4b");

        Background back = new Background("Echox720.png");
        back.setLayout(null);
        setContentPane(back);

        tipBox = new TipBox();
        tipBox.setLocation((FRAME_WIDTH - tipBox.getWidth()) / 2, 240);
        back.add(tipBox);

        FrameMain outFrame = this;
        addPCButtons(back, outFrame);
        addQuestButtons(back, outFrame);
        addBackgroundFrames(back, outFrame);
        addEasterEgg(back, outFrame);
        addDeleteCached(back, outFrame);
        addGetLog(back, outFrame);
        addCredits(back, outFrame);
        //addPlayButton(back);
        //addStopButton(back);

        pack();
        centerFrame(this, FRAME_WIDTH, FRAME_HEIGHT);
    }

    private void addCredits(JPanel back, FrameMain outFrame) {
        int size = 40;
        // Bottom-right, but within the content pane (the window's title bar/borders eat ~40px of
        // the 720 frame height, so anchoring to FRAME_HEIGHT pushed it off-screen). Align with the
        // bottom button row instead.
        int x = FRAME_WIDTH - size - 30;
        int y = 595;
        JLabel credits = new JLabel(infoIcon(size));
        credits.setBounds(x, y, size, size);
        credits.setOpaque(false);
        credits.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        credits.setToolTipText("Credits / About");
        credits.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { showCredits(outFrame); }
            public void mouseEntered(MouseEvent e) { tipBox.showTip("About this installer & credits"); }
            public void mouseExited(MouseEvent e) { tipBox.showDefault(); }
        });
        back.add(credits);
    }

    /** A vector-drawn "ⓘ" info badge: filled magenta circle with a white "i". */
    private static Icon infoIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(200, 0, 150));
                g2.fillOval(x + 1, y + 1, size - 2, size - 2);
                g2.setColor(new Color(255, 255, 255, 190));
                g2.setStroke(new BasicStroke(Math.max(1.5f, size / 22f)));
                g2.drawOval(x + 1, y + 1, size - 3, size - 3);
                // white "i"
                g2.setColor(Color.WHITE);
                int cx = x + size / 2;
                int dotR = Math.max(2, size / 12);
                g2.fillOval(cx - dotR, y + Math.round(size * 0.27f) - dotR, dotR * 2, dotR * 2);
                g2.setStroke(new BasicStroke(Math.max(2f, size / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx, Math.round(y + size * 0.43f), cx, Math.round(y + size * 0.73f));
                g2.dispose();
            }
        };
    }

    private void showCredits(JFrame parent) {
        JOptionPane.showMessageDialog(parent,
            "<html>Copyright for Echo VR is by Meta/Ready at Dawn!<br>"
            + "This installer is not at all associated with them!<br><br>"
            + "Special thanks to Sick and SirDominik for some of the backgrounds!<br>"
            + "Special thanks to F-A-N-G-O-R-N for getting me into Java and helping with this project.<br>"
            + "I know you still feel shame when you have to look at my source code.<br>"
            + "Special thanks to Leon(leon1273) for contributing and cleaning stuff in my code<br>"
            + "This tool is still in early alpha!<br>"
            + "If you have problems, contact me on Discord 'marshmallow_mia'.</html>",
            "Credits", JOptionPane.INFORMATION_MESSAGE);
    }




    private void addGetLog(JPanel back, JFrame outFrame) {
        SpecialButton btn_addGetLog = new SpecialButton("Get Quest Logs", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        btn_addGetLog.setLocation(818, 547);
        btn_addGetLog.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                GetLogFilesFromQuest.getLogFilesFromQuest();
            }
        });
        btn_addGetLog.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Retrieve log files from your Quest for troubleshooting");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(btn_addGetLog);
        btn_addGetLog.setVisible(false);

        SpecialButton addDeleteIcon = new SpecialButton("", "delete.png", "delete.png", "delete.png", 20);
        addDeleteIcon.setLocation(770, 595);
        //back.add(addDeleteIcon);

    }

    private void addDeleteCached(JPanel back, JFrame outFrame) {
        SpecialButton btn_deleteCache = new SpecialButton("Delete cache", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        btn_deleteCache.setLocation(818, 595);
        btn_deleteCache.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                // Create an instance of DeleteCache and call the executeDeletion method
                DeleteCache deleteCache = new DeleteCache();
                deleteCache.executeDeletion(outFrame);
            }
        });
        btn_deleteCache.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Clear cached downloaded files to free up space");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(btn_deleteCache);
        btn_deleteCache.setVisible(true);

        SpecialButton addDeleteIcon = new SpecialButton("", "delete.png", "delete.png", "delete.png", 20);
        addDeleteIcon.setLocation(770, 595);
        back.add(addDeleteIcon);
        addDeleteIcon.setVisible(true);

    }


    private void addPCButtons(JPanel back, FrameMain outFrame) {
        SpecialButton btn_PCInstallEcho = new SpecialButton("Install Echo VR", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_PCInstallEcho.setLocation((FRAME_WIDTH / 2 - btn_PCInstallEcho.getWidth()) / 2, 200);
        btn_PCInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                new FrameGuidancePC(outFrame);
            }
        });
        btn_PCInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Download and install Echo VR for PC");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(btn_PCInstallEcho);


        SpecialButton btn_PCUpdateEcho = new SpecialButton("Update Echo (PC)", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        btn_PCUpdateEcho.setLocation(btn_PCInstallEcho.getX(), 280);
        btn_PCUpdateEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                new FramePCUpdate(outFrame);
            }
        });
        btn_PCUpdateEcho.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Download and install the latest Echo VR game update");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(btn_PCUpdateEcho);
    }

    private void addQuestButtons(JPanel back, FrameMain outFrame) {
        SpecialButton btn_QuestInstallEcho = new SpecialButton("Quest Install Echo", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_QuestInstallEcho.setLocation(819, 200);
        btn_QuestInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                new FrameGuidanceQuest(outFrame);
            }
        });
        btn_QuestInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Download and install Echo VR on your Quest headset");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(btn_QuestInstallEcho);
    }

    private void addBackgroundFrames(JPanel back, FrameMain outFrame) {
        // ---- PC side: No licence patch + Steam Patch ----
        SpecialButton btn_PCnonLicence = new SpecialButton("No licence patch", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_PCnonLicence.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                new FramePCPatcher();
            }
        });
        btn_PCnonLicence.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Patch Echo VR if you don't own it on your Oculus account");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });

        SpecialButton btn_PCnoOVRHeadset = new SpecialButton("Steam Patch (Revive)", "button_up.png", "button_down.png", "button_highlighted.png", 19);
        btn_PCnoOVRHeadset.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                new FrameSteamPatcher(outFrame);
            }
        });
        btn_PCnoOVRHeadset.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Install Revive for SteamVR headsets like Valve Index");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });

        final int ARC = 20;
        final int PAD = 15;

        // Position inside rahmen1 with PAD padding
        int b1w = btn_PCnonLicence.getWidth();
        int b1h = btn_PCnonLicence.getHeight();
        int b2w = btn_PCnoOVRHeadset.getWidth();
        int b2h = btn_PCnoOVRHeadset.getHeight();

        btn_PCnonLicence.setLocation(PAD, PAD);
        btn_PCnoOVRHeadset.setLocation(PAD, PAD + b1h + PAD);

        int pcPanelW = Math.max(b1w, b2w) + PAD * 2;
        int pcPanelH = PAD + b1h + PAD + b2h + PAD;

        JPanel rahmen1 = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(200, 0, 150, 150));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
                g2.setColor(new Color(50, 50, 50, 150));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ARC, ARC));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        rahmen1.setOpaque(false);
        rahmen1.setBounds((FRAME_WIDTH / 2 - pcPanelW) / 2, 420, pcPanelW, pcPanelH);
        rahmen1.add(btn_PCnonLicence);
        rahmen1.add(btn_PCnoOVRHeadset);
        back.add(rahmen1);
        rahmen1.setVisible(false);

    }

    private void addEasterEgg(JPanel back, FrameMain outFrame) {
        JLabel easteregg = new JLabel("", SwingConstants.CENTER);
        easteregg.setOpaque(false);
        easteregg.setForeground(new Color(255, 255, 255));
        easteregg.setSize(100, 100);
        easteregg.setLocation(590, 430);
        easteregg.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                JOptionPane.showMessageDialog(outFrame, "Never divide by 0!", "You found an Easter Egg", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        back.add(easteregg);




    }

    private void addPlayButton(Background back){

        JLabel playButton = addImageTransparent(back, "play.png", 590, 90, 30, 30);
        playButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Trigger play action
                PlayMusic.playMusic("C:/Users/marcel/IdeaProjects/Echo-VR-Installer/src/main/resources/EchoLobby.wav");
            }
        });

    }
    private void addStopButton(Background back) {
        JLabel stopButton = addImageTransparent(back, "stop.png", 657, 90, 30, 30);
        stopButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Trigger play action
                PlayMusic.stopMusic();


            }
        });
    }


    private JLabel addImageTransparent(JPanel panel, String imagePath, int x, int y, int width, int height) {
        ImageIcon icon = new ImageIcon(loadGUI(imagePath));
        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        JLabel label = new JLabel(new ImageIcon(scaledImage));
        label.setBounds(x, y, width, height);
        label.setOpaque(false);  // Make sure the label is transparent
        panel.add(label);
        return label;  // Return the label so that it can be used by the caller
    }


}
