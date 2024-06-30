package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;


public class FrameMain extends JFrame {
    //Attribute:
    private int frameWidth = 1280;
    private int frameHeight = 720;


    //Konstruktor:
    public FrameMain() {
        //Benutzeroberfläche erzeugen...
        initComponents();
        //Fenster sichtbar machen...
        this.setVisible(true);
    }



    //Initialisiert Benutzeroberfläche:
    private void initComponents() {
        //Grundeinstellungen des Fensters...
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.2 [pre alpha]");
        FrameMain outFrame = this;

        Background back = new Background("Echox720.png");
        back.setLayout(null);
        this.setContentPane(back);


        //PC BUTTONS:
        SpecialButton btn_PCInstallEcho = new SpecialButton("PC Install Echo", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_PCInstallEcho.setLocation((frameWidth / 2 - btn_PCInstallEcho.getWidth()) / 2, 200);

        btn_PCInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                FramePCDownload framePCDownload = new FramePCDownload(outFrame);
            }
        });
        back.add(btn_PCInstallEcho);



        SpecialLabel optionalPCLabel = new SpecialLabel("The following patches are optional", 20);
        optionalPCLabel.setLocation((frameWidth / 2 - optionalPCLabel.getWidth()) / 2, 360);
        back.add(optionalPCLabel);

        SpecialLabel optionalPCLabel2 = new SpecialLabel("(Only use them if you have to)", 10);
        optionalPCLabel2.setLocation((frameWidth / 2 - optionalPCLabel2.getWidth()) / 2, 395);
        back.add(optionalPCLabel2);



        SpecialButton btn_PCnonLicence = new SpecialButton("No licence patch", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_PCnonLicence.setLocation((frameWidth / 2 - btn_PCnonLicence.getWidth()) / 2, 440);
        btn_PCnonLicence.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                FramePCPatcher frame = new FramePCPatcher();
            }
        });
        back.add(btn_PCnonLicence);

        SpecialLabel noLincencePCLabel = new SpecialLabel("If you don't own Echo on your Account, use this Patch", 10);
        noLincencePCLabel.setLocation((frameWidth / 2 - noLincencePCLabel.getWidth()) / 2, 500);
        back.add(noLincencePCLabel);



        SpecialButton btn_PCnoOVRHeadset = new SpecialButton("Steam Patch", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_PCnoOVRHeadset.setLocation((frameWidth / 2 - btn_PCnoOVRHeadset.getWidth()) / 2, 560);
        btn_PCnoOVRHeadset.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                FrameSteamPatcher frameSteamPatcher = new FrameSteamPatcher(outFrame);
            }
        });
        back.add(btn_PCnoOVRHeadset);

        SpecialLabel steamPatchLabel = new SpecialLabel("If you Play with an Steam only Headset use this Patch. (Normally not needed)", 10);
        steamPatchLabel.setLocation((frameWidth / 2 - steamPatchLabel.getWidth()) / 2, 620);
        back.add(steamPatchLabel);


        //QUEST BUTTONS:
        SpecialButton btn_QuestInstallEcho = new SpecialButton("Quest Install Echo", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_QuestInstallEcho.setLocation(819, 200);
        btn_QuestInstallEcho.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
               FrameQuestDownload frameQuestDownload = new FrameQuestDownload(outFrame);
            }
        });
        back.add(btn_QuestInstallEcho);

        SpecialLabel questInstallNoticeLabel1 = new SpecialLabel("If you dont own Echo on your account,", 20);
        questInstallNoticeLabel1.setLocation((frameWidth / 4 * 3 - questInstallNoticeLabel1.getWidth() / 2), 265);
        back.add(questInstallNoticeLabel1);

        SpecialLabel questInstallNoticeLabel2 = new SpecialLabel("use the method below instead of the top one!", 20);
        questInstallNoticeLabel2.setLocation((frameWidth / 4 * 3 - questInstallNoticeLabel2.getWidth() / 2), 300);
        back.add(questInstallNoticeLabel2);



        SpecialLabel optionalQuestLabel = new SpecialLabel("The following patches are optional", 20);
        optionalQuestLabel.setLocation((frameWidth / 4 * 3 - optionalQuestLabel.getWidth() / 2), 360);
        back.add(optionalQuestLabel);

        SpecialLabel optionalQuestLabel2 = new SpecialLabel("(Only use them if you have to)", 10);
        optionalQuestLabel2.setLocation((frameWidth / 4 * 3 - optionalQuestLabel2.getWidth() /2), 395);
        back.add(optionalQuestLabel2);



        SpecialButton btn_QuestNoLicence = new SpecialButton("No licence patch", "button_up.png", "button_down.png", "button_highlighted.png", 20);
        btn_QuestNoLicence.setLocation((frameWidth / 4 * 3 -btn_QuestNoLicence.getWidth() /2), 440);
        btn_QuestNoLicence.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
            FrameQuestPatcher frameQuestPatcher = new FrameQuestPatcher();
            }
        });
        back.add(btn_QuestNoLicence);

        SpecialLabel noLincenceQuestLabel = new SpecialLabel("If you don't own Echo on your Account, use this Patch", 10);
        noLincenceQuestLabel.setLocation((frameWidth / 4 * 3 - noLincenceQuestLabel.getWidth() /2), 500);
        back.add(noLincenceQuestLabel);





        Background rahmen1 = new Background("Rahmenbild.png");
        rahmen1.setLayout(null);
        rahmen1.setLocation((frameWidth / 2 - steamPatchLabel.getWidth()) / 2 - 10, optionalPCLabel.getY() - 10);
        rahmen1.setSize(steamPatchLabel.getWidth() + 20,305);
        rahmen1.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 3));
        rahmen1.setBackground(new Color(255, 255, 255, 0));
        back.add(rahmen1);

        Background rahmen2 = new Background("Rahmenbild.png");
        rahmen2.setLayout(null);
        rahmen2.setLocation((frameWidth / 4 * 3 + optionalQuestLabel.getWidth()) / 2 + 10, optionalQuestLabel.getY() - 10);
        rahmen2.setSize(optionalQuestLabel.getWidth() + 20,186);
        rahmen2.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 3));
        rahmen2.setBackground(new Color(255, 255, 255, 0));
        back.add(rahmen2);

        JLabel easteregg = new JLabel("", SwingConstants.CENTER);
        easteregg.setOpaque(true);
        easteregg.setForeground(new Color(255, 255, 255));
        easteregg.setBackground(Color.BLUE);
        easteregg.setSize(100,100);
        easteregg.setLocation(590,430);
        back.add(easteregg);

        easteregg.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                JOptionPane.showMessageDialog(outFrame, "Never device by 0!", "You found an Easter Egg", JOptionPane.INFORMATION_MESSAGE);
                new TorrentDownloader();

            }
        });



        //Alles fertig machen...
        this.pack();

        //Fenstergröße und Position setzen...
        this.setSize(frameWidth, frameHeight);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (d.width - this.getWidth()) / 2;
        int y = (d.height - this.getHeight()) / 2;
        this.setLocation(x, y);

        JOptionPane.showMessageDialog(this, "<html>Copyright for Echo VR is by Meta/Ready at Dawn!<br>" +
                "    This installer is not at all associated with them!<br>" +
                "    Special thanks to Sick and SirDominik for some of the backgrounds!<br>" +
                "    Special thanks to  F-A-N-G-O-R-N for getting me into Java and helping with this project.<br>" +
                "    I know you still feel shame when you have to look at my source code.<br>" +
                "    This tool is still in early alpha!<br>" +
                "    If you have problems, contact me on Discord 'marcel_one_'.</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

    }




    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }
}
