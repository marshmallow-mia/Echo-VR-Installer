package bl00dy_c0d3_.echovr_installer;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;


import static bl00dy_c0d3_.echovr_installer.Helpers.jsonFileChooser;
import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameQuestDownload extends JDialog {
    Downloader downloader = null;
    Downloader downloader2 = null;
    FrameMain frameMain = null;
    int frameWidth = 700;
    int frameHeight = 400;
    public int firstDownloadDone = 0;
    //Get the temp path
    Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    String configPath = "Optional: Choose config.json on the button above";
    JDialog outFrame = this;
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
    SpecialButton questStartDownload;


    //Constructor
    public FrameQuestDownload(FrameMain frameMain){
        this.frameMain = frameMain;
        initComponents();
        this.setVisible(true);
    }


    public void dispose(){
        super.dispose();
        if (downloader != null){
            downloader.cancelDownload();
        }
        if (downloader2 != null){
            downloader2.cancelDownload();
        }
    }


    private void initComponents(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.3c");
        FrameQuestDownload outFrame = this;


        this.setContentPane(createContentPane());
        this.setModal(true);

        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>If you don't own Echo on your account don't use this Installer! Use the \"No licence patch\"<br>down below on the main menu instead and just close the next window!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);


        //Alles fertig machen...
        this.pack();

        //Fenstergröße und Position setzen...
        this.setSize(frameWidth, frameHeight);
        int x = frameMain.getX() + (frameMain.getWidth() - this.getWidth()) / 2;
        int y = frameMain.getY() + (frameMain.getHeight() - this.getHeight()) / 2;
        this.setLocation(x, y);
    }


    private @NotNull JPanel createContentPane() {
        Background back = new Background("Echo2.jpg");
        back.setLayout(null);
        addSpecialLabels(back);
        addSpecialCheckBox(back);
        addStartDownloadButton(back);
        addChooseConfigButton(back);
        addQuestStartPatchingButton(back);
        return back;
    }

    private void handleDownloadButtonClick() {
        if (downloader != null){
            downloader.cancelDownload();
            System.out.println("downloader1 stopped");
        }
        if (downloader2 != null){
            downloader2.cancelDownload();
            System.out.println("downloader2 stopped");
        }
        questStartDownload.changeText("Restart Download");



        JOptionPane.showMessageDialog(this, "The Download will start after pressing OK. Please wait for both files to be done!", "Download started", JOptionPane.INFORMATION_MESSAGE);


        downloader = new Downloader();
        downloader.startDownload("https://echo.marceldomain.de:6969/Echo_patched.apk", targetPath + "", "Echo_patched.apk",  labelQuestProgress2, outFrame, null, 2, -1);

        pause(1);

        downloader2 = new Downloader();
        downloader2.startDownload("https://echo.marceldomain.de:6969/main.4987566.com.readyatdawn.r15.obb", targetPath + "", "main.4987566.com.readyatdawn.r15.obb",  labelQuestProgress3, outFrame, null, 2, -1);
    }



    private void handleQuestStartPatchingButtonClick() {
        String apkfileName;
        labelQuestInstallProgress.setText("Installation started! Wait!");
        outFrame.repaint();
        JOptionPane.showMessageDialog(outFrame, "<html>Press OK to start the installation. It can take a minute to install!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

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
        InstallerQuest installToQuest = new InstallerQuest();
        installToQuest.installAPK(targetPath + "", apkfileName, obbfileName,labelQuestInstallProgress, outFrame);
        labelQuestInstallProgress.setText("Installation is complete!");
        outFrame.repaint();
        JOptionPane.showMessageDialog(outFrame, "<html>Installation of Echo is done. You can start it now on your Quest.<br> DON'T CLICK ON RESTORE IF YOU WILL GET ASKED TO OR YOU NEED TO REINSTALL AGAIN!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

    }


    //Needs to be declared outside, as its needed outside
    SpecialLabel labelQuestProgress2 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelQuestProgress3 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelConfigPath = new SpecialLabel(configPath, 14);
    SpecialLabel labelQuestInstallProgress = new SpecialLabel("Not started yet", 20);


    private void addSpecialLabels(@NotNull JPanel back) {
        back.add(Helpers.createSpecialLabel("Progress = ", 17, 282, 40, new Dimension(240, 38), Color.BLACK, new Color(255, 255, 255, 200)));


        //Progressbar
        labelQuestProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress2.setLocation(522,40);
        labelQuestProgress2.setSize(130, 19);
        labelQuestProgress2.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress2.setForeground(Color.BLACK);
        back.add(labelQuestProgress2);

        labelQuestProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress3.setLocation(522,59);
        labelQuestProgress3.setSize(130, 19);
        labelQuestProgress3.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress3.setForeground(Color.BLACK);
        back.add(labelQuestProgress3);


        //ConfigPath
        labelConfigPath.setLocation(50,160);
        labelConfigPath.setSize(600, 25);
        labelConfigPath.setBackground(new Color(255, 255, 255, 200));
        labelConfigPath.setForeground(Color.BLACK);
        back.add(labelConfigPath);

        //InstallProgress
        labelQuestInstallProgress.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestInstallProgress.setLocation(350,250);
        labelQuestInstallProgress.setSize(330, 50);
        labelQuestInstallProgress.setBackground(new Color(255, 255, 255, 200));
        labelQuestInstallProgress.setForeground(Color.BLACK);
        back.add(labelQuestInstallProgress);


    }


    //Needs to be declared outside, as its needed outside
    SpecialCheckBox checkBoxConfig = new SpecialCheckBox("Check this to use the custom config", 17);
    private void addSpecialCheckBox(@NotNull JPanel back) {
        checkBoxConfig.setSize(500,30);
        checkBoxConfig.setLocation(50, 190);

        //JCheckBoxen werden Panel hinzugefügt
        back.add(checkBoxConfig);


    }

    private void addStartDownloadButton(@NotNull JPanel back) {
        questStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        questStartDownload.setLocation(50, 40);
        questStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                handleDownloadButtonClick();
            }
        });
        back.add(questStartDownload);

    }

    private void addChooseConfigButton(@NotNull JPanel back) {
        SpecialButton chooseConfig = new SpecialButton("OPTIONAL CONFIG", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        chooseConfig.setLocation(50, 111);
        chooseConfig.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                jsonFileChooser(labelConfigPath, outFrame);
            }
        });
        back.add(chooseConfig);
    }


    private void addQuestStartPatchingButton(@NotNull JPanel back) {
        SpecialButton questStartPatching = new SpecialButton("Install Echo to Quest", "button_up.png", "button_down.png", "button_highlighted.png", 15);
        questStartPatching.setLocation(50, 250);
        questStartPatching.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                handleQuestStartPatchingButtonClick();
            }
        });
        back.add(questStartPatching);
    }


    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }



}

