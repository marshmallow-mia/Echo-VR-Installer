package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameMain extends JFrame {
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;
    private AdSystem adSystem;
    private Timer autoAdTimer;

    public FrameMain() {
        initComponents();
        adSystem = new AdSystem(this);
        startAutoAdTimer();
        this.setVisible(true);
    }

    private void startAutoAdTimer() {
        autoAdTimer = new Timer();
        autoAdTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> adSystem.showAd());
            }
        }, 120000, 120000);
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setIconImage(loadGUI("icon.png"));
        setTitle("Echo VR Installer v0.8.5 [pre alpha]");

        Background back = new Background("Echox720.png");
        back.setLayout(null);
        setContentPane(back);

        FrameMain outFrame = this;
        addPCButtons(back, outFrame);
        addQuestButtons(back, outFrame);
        addBackgroundFrames(back);
        addEasterEgg(back, outFrame);
        addDeleteCached(back, outFrame);
        addGetLog(back, outFrame);
        //addPlayButton(back);
        //addStopButton(back);

        pack();
        centerFrame(this, FRAME_WIDTH, FRAME_HEIGHT);

        JOptionPane.showMessageDialog(this, "<html>This is not the actual version of the installer and is meant to be a shitpost, and it may not have newest features of the installer<br>" +
                "if you want to use the real installer, click <a href='https://github.com/BL00DY-C0D3/Echo-VR-Installer/releases'>here</a>.</html>", "WARNING - Silly Version", JOptionPane.WARNING_MESSAGE);

    }




    private void addGetLog(JPanel back, JFrame outFrame) {
        SpecialButton btn_addGetLog = new SpecialButton("Get Quest Logs", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        btn_addGetLog.setLocation(818, 547);
        btn_addGetLog.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAd();
                GetLogFilesFromQuest.getLogFilesFromQuest();
            }
        });
        back.add(btn_addGetLog);

        SpecialButton addDeleteIcon = new SpecialButton("", "delete.png", "delete.png", "delete.png", 20);
        addDeleteIcon.setLocation(770, 595);
        //back.add(addDeleteIcon);

        SpecialLabel cacheLabel = createSpecialLabel("Delete the known files cache. (Downloaded files)", 12);
        cacheLabel.setLocation(818, 640);
        //back.add(cacheLabel);

    }

    private void addDeleteCached(JPanel back, JFrame outFrame) {
        SpecialButton btn_deleteCache = new SpecialButton("Delete cache", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        btn_deleteCache.setLocation(818, 595);
        btn_deleteCache.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAd();
                DeleteCache deleteCache = new DeleteCache();
                deleteCache.executeDeletion(outFrame);
            }
        });
        back.add(btn_deleteCache);

        SpecialButton addDeleteIcon = new SpecialButton("", "delete.png", "delete.png", "delete.png", 20);
        addDeleteIcon.setLocation(770, 595);
        back.add(addDeleteIcon);

        SpecialLabel cacheLabel = createSpecialLabel("Delete the known files cache. (Downloaded files)", 12);
        cacheLabel.setLocation(818, 640);
        back.add(cacheLabel);

    }


    private void addPCButtons(JPanel back, FrameMain outFrame) {
        SpecialButton btn_PCInstallEcho = new SpecialButton("PC Install Echo", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_PCInstallEcho.setLocation((FRAME_WIDTH / 2 - btn_PCInstallEcho.getWidth()) / 2, 200);
        btn_PCInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAdThenExecute(() -> new FramePCDownload(outFrame));
            }
        });
        back.add(btn_PCInstallEcho);


        SpecialButton btn_PCUpdateEcho = new SpecialButton("Update Echo (PC)", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        btn_PCUpdateEcho.setLocation(btn_PCInstallEcho.getX(), 280);
        btn_PCUpdateEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAdThenExecute(() -> new FramePCEchoUpdate(outFrame));
            }
        });
        back.add(btn_PCUpdateEcho);


        SpecialLabel optionalPCLabel = createSpecialLabel("The following patches are optional", 20);
        optionalPCLabel.setLocation((FRAME_WIDTH / 2 - optionalPCLabel.getPreferredSize().width) / 2, 360);
        back.add(optionalPCLabel);

        SpecialLabel optionalPCLabel2 = createSpecialLabel("(Only use them if you have to)", 10);
        optionalPCLabel2.setLocation((FRAME_WIDTH / 2 - optionalPCLabel2.getPreferredSize().width) / 2, 395);
        back.add(optionalPCLabel2);

        SpecialButton btn_PCnonLicence = new SpecialButton("No licence patch", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_PCnonLicence.setLocation((FRAME_WIDTH / 2 - btn_PCnonLicence.getWidth()) / 2, 440);
        btn_PCnonLicence.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAdThenExecute(() -> new FramePCPatcher());
            }
        });
        back.add(btn_PCnonLicence);

        SpecialLabel noLincencePCLabel = createSpecialLabel("If you don't own Echo on your Account, use this Patch", 10);
        noLincencePCLabel.setLocation((FRAME_WIDTH / 2 - noLincencePCLabel.getPreferredSize().width) / 2, 500);
        back.add(noLincencePCLabel);

        SpecialButton btn_PCnoOVRHeadset = new SpecialButton("Steam Patch (Revive)", "button_up.png", "button_down.png", "button_highlighted.png", 19);
        btn_PCnoOVRHeadset.setLocation((FRAME_WIDTH / 2 - btn_PCnoOVRHeadset.getWidth()) / 2, 560);
        btn_PCnoOVRHeadset.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAdThenExecute(() -> new FrameSteamPatcher(outFrame));
            }
        });
        back.add(btn_PCnoOVRHeadset);

        SpecialLabel steamPatchLabel = createSpecialLabel("If you Play with an Steam only Headset use this Patch. (Normally not needed)", 10);
        steamPatchLabel.setLocation((FRAME_WIDTH / 2 - steamPatchLabel.getPreferredSize().width) / 2, 620);
        back.add(steamPatchLabel);
    }

    private void addQuestButtons(JPanel back, FrameMain outFrame) {
        SpecialButton btn_QuestInstallEcho = new SpecialButton("Quest Install Echo", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_QuestInstallEcho.setLocation(819, 200);
        btn_QuestInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAdThenExecute(() -> new FrameQuestDownload(outFrame));
            }
        });
        back.add(btn_QuestInstallEcho);

        SpecialLabel questInstallNoticeLabel1 = createSpecialLabel("If you don't own Echo on your account,", 20);
        questInstallNoticeLabel1.setLocation((FRAME_WIDTH / 4 * 3 - questInstallNoticeLabel1.getPreferredSize().width / 2), 265);
        back.add(questInstallNoticeLabel1);

        SpecialLabel questInstallNoticeLabel2 = createSpecialLabel("use the method below instead of the top one!", 20);
        questInstallNoticeLabel2.setLocation((FRAME_WIDTH / 4 * 3 - questInstallNoticeLabel2.getPreferredSize().width / 2), 300);
        back.add(questInstallNoticeLabel2);

        SpecialLabel optionalQuestLabel = createSpecialLabel("The following patches are optional", 20);
        optionalQuestLabel.setLocation((FRAME_WIDTH / 4 * 3 - optionalQuestLabel.getPreferredSize().width / 2), 360);
        back.add(optionalQuestLabel);

        SpecialLabel optionalQuestLabel2 = createSpecialLabel("(Only use them if you have to)", 10);
        optionalQuestLabel2.setLocation((FRAME_WIDTH / 4 * 3 - optionalQuestLabel2.getPreferredSize().width / 2), 395);
        back.add(optionalQuestLabel2);

        SpecialButton btn_QuestNoLicence = new SpecialButton("No licence patch", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_QuestNoLicence.setLocation((FRAME_WIDTH / 4 * 3 - btn_QuestNoLicence.getWidth() / 2), 440);
        btn_QuestNoLicence.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAdThenExecute(() -> new FrameQuestPatcher());
            }
        });
        back.add(btn_QuestNoLicence);

        SpecialLabel noLincenceQuestLabel = createSpecialLabel("If you don't own Echo on your Account, use this Patch", 10);
        noLincenceQuestLabel.setLocation((FRAME_WIDTH / 4 * 3 - noLincenceQuestLabel.getPreferredSize().width / 2), 500);
        back.add(noLincenceQuestLabel);
    }

    private void addBackgroundFrames(JPanel back) {
        SpecialLabel steamPatchLabel = createSpecialLabel("If you Play with an Steam only Headset use this Patch. (Normally not needed)", 10);

        Background rahmen1 = new Background("Rahmenbild.png");
        rahmen1.setLayout(null);
        rahmen1.setLocation((FRAME_WIDTH / 2 - steamPatchLabel.getPreferredSize().width) / 2 - 8, 350);
        rahmen1.setSize(steamPatchLabel.getPreferredSize().width + 30, 305);
        rahmen1.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 3));
        rahmen1.setBackground(new Color(255, 255, 255, 0));
        back.add(rahmen1);

        SpecialLabel optionalQuestLabel = createSpecialLabel("The following patches are optional", 20);


        Background rahmen2 = new Background("Rahmenbild.png");
        rahmen2.setLayout(null);
            rahmen2.setLocation((FRAME_WIDTH / 4 * 3 + optionalQuestLabel.getPreferredSize().width) / 2 + 18, 350);
        rahmen2.setSize(optionalQuestLabel.getPreferredSize().width + 30, 186);
        rahmen2.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 3));
        rahmen2.setBackground(new Color(255, 255, 255, 0));
        back.add(rahmen2);
    }

    private void addEasterEgg(JPanel back, FrameMain outFrame) {
        JLabel easteregg = new JLabel("", SwingConstants.CENTER);
        easteregg.setOpaque(false);
        easteregg.setForeground(new Color(255, 255, 255));
        easteregg.setSize(100, 100);
        easteregg.setLocation(590, 430);
        easteregg.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                adSystem.showAd();
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
