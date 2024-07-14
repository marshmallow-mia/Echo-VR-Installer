package bl00dy_c0d3_.echovr_installer;

import com.frostwire.jlibtorrent.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static bl00dy_c0d3_.echovr_installer.Helpers.jsonFileChooser;

public class FrameQuestDownload extends JDialog {
    TorrentDownload downloader = null;
    TorrentDownload downloader2 = null;
    FrameMain frameMain = null;
    int frameWidth = 700;
    int frameHeight = 400;
    public int firstDownloadDone = 0;
    //Get the temp path
    Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    String configPath = "Optional: Choose config.json on the button above";

    //Constructor
    public FrameQuestDownload(FrameMain frameMain){
        this.frameMain = frameMain;
        initComponents();
        this.setVisible(true);
    }


    public void dispose(){
        if (downloader != null){
            downloader.cancelDownload();
        }
        if (downloader2 != null){
            downloader2.cancelDownload();
        }
        super.dispose();
    }


    private void initComponents(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.3");
        FrameQuestDownload outFrame = this;

        Background back = new Background("Echo2.jpg");
        back.setLayout(null);
        this.setContentPane(back);
        this.setModal(true);

        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>If you don't own Echo on your account don't use this Installer! Use the \"No licence patch\"<br>down below on the main menu instead and just close the next window!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

        SpecialLabel labelQuestProgress1 = new SpecialLabel("Progress = ", 17);
        labelQuestProgress1.setLocation(282,40);
        labelQuestProgress1.setSize(240, 38);
        labelQuestProgress1.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress1.setForeground(Color.BLACK);
        back.add(labelQuestProgress1);


        SpecialLabel labelQuestProgress2 = new SpecialLabel(" 0%", 15);
        labelQuestProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress2.setLocation(522,40);
        labelQuestProgress2.setSize(130, 19);
        labelQuestProgress2.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress2.setForeground(Color.BLACK);
        back.add(labelQuestProgress2);

        SpecialLabel labelQuestProgress3 = new SpecialLabel(" 0%", 15);
        labelQuestProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress3.setLocation(522,59);
        labelQuestProgress3.setSize(130, 19);
        labelQuestProgress3.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress3.setForeground(Color.BLACK);
        back.add(labelQuestProgress3);


        SpecialButton questStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 18);
        questStartDownload.setLocation(50, 40);
        questStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                JOptionPane.showMessageDialog(null, "The Download will start after pressing OK. Please wait for both files to be done!", "Download started", JOptionPane.INFORMATION_MESSAGE);
                //Added SessionManager as the TorrentDownloader isnt able to download more then file at the same time otherwise
                SessionManager sessionManager = new SessionManager();
                sessionManager.start();
                SessionManager sessionManager2 = new SessionManager();
                sessionManager2.start();

                downloader = new TorrentDownload(sessionManager);
                downloader.startDownload("p2pFiles/apk.torrent", targetPath + "", "Echo_patched.apk",  labelQuestProgress2, outFrame, null, 2);
                downloader2 = new TorrentDownload(sessionManager2);
                downloader2.startDownload("p2pFiles/obb.torrent", targetPath + "", "main.4987566.com.readyatdawn.r15.obb",  labelQuestProgress3, outFrame, null, 2);
            }
        });
        back.add(questStartDownload);

        SpecialLabel labelConfigPath = new SpecialLabel(configPath, 14);
        labelConfigPath.setLocation(50,160);
        labelConfigPath.setSize(600, 25);
        labelConfigPath.setBackground(new Color(255, 255, 255, 200));
        labelConfigPath.setForeground(Color.BLACK);

        back.add(labelConfigPath);

        SpecialButton chooseConfig = new SpecialButton("OPTIONAL CONFIG", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        chooseConfig.setLocation(50, 111);
        chooseConfig.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                jsonFileChooser(labelConfigPath, outFrame);
            }
        });
        back.add(chooseConfig);


        SpecialCheckBox checkBoxConfig = new SpecialCheckBox("Check this to use the custom config", 17);
        checkBoxConfig.setSize(500,30);
        checkBoxConfig.setLocation(50, 190);

        //JCheckBoxen werden Panel hinzugefügt
        back.add(checkBoxConfig);



        SpecialLabel labelQuestInstallProgress = new SpecialLabel("Not started yet", 20);
        labelQuestInstallProgress.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestInstallProgress.setLocation(350,250);
        labelQuestInstallProgress.setSize(330, 50);
        labelQuestInstallProgress.setBackground(new Color(255, 255, 255, 200));
        labelQuestInstallProgress.setForeground(Color.BLACK);
        back.add(labelQuestInstallProgress);


        SpecialButton questStartPatching = new SpecialButton("Install Echo to Quest", "button_up.png", "button_down.png", "button_highlighted.png", 15);
        questStartPatching.setLocation(50, 250);
        questStartPatching.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String apkfileName;
                if (checkBoxConfig.isSelected()){

                    File f = new File(targetPath + "/Echo_patched.apk");
                    if(f.exists() && !f.isDirectory()) {
                        PatchAPK patchAPK = new PatchAPK();
                        if (!patchAPK.patchAPK(targetPath + "", "Echo_patched.apk", labelConfigPath.getText(), labelConfigPath, outFrame)) {
                            return;
                        }

                    }
                    else {
                        ErrorDialog error2 = new ErrorDialog();
                        error2.errorDialog(outFrame, "Echo not found", "Echo wasn't found. Please use the top Button first", 2);
                        return;
                    }
                    apkfileName = "changedConfig-aligned-debugSigned.apk";
                }
                else {
                    apkfileName = "Echo_patched.apk";
                }
                String obbfileName = "main.4987566.com.readyatdawn.r15.obb";
                InstallerQuest installtoQuest = new InstallerQuest();
                installtoQuest.installAPK(targetPath + "", apkfileName, obbfileName,labelQuestInstallProgress, outFrame);

            }
        });
        back.add(questStartPatching);




        //Alles fertig machen...
        this.pack();

        //Fenstergröße und Position setzen...
        this.setSize(frameWidth, frameHeight);
        int x = frameMain.getX() + (frameMain.getWidth() - this.getWidth()) / 2;
        int y = frameMain.getY() + (frameMain.getHeight() - this.getHeight()) / 2;
        this.setLocation(x, y);
    }


    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }

}
