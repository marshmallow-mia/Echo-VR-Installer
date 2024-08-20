package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameSteamPatcher extends JDialog {
    Downloader downloader1 = null;
    FrameMain frameMain = null;
    int frameWidth = 700;
    int frameHeight = 200;
    String path = "C://EchoVR/ready-at-dawn-echo-arena";
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
    FrameSteamPatcher outFrame = this;



    //Constructor
    public FrameSteamPatcher(FrameMain frameMain){
        this.frameMain = frameMain;
        initComponents();
        this.setVisible(true);
        this.setModal(true);
    }


    public void dispose(){
        if (downloader1 != null){
            downloader1.cancelDownload();
        }
        super.dispose();
    }

    private void initComponents(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.3c");
        this.setModal(true);


        Background back = new Background("echo_combat1.png");
        back.setLayout(null);
        this.setContentPane(back);

        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>This patch is only needed if you want to use a not Oculus capable Headset like the Valve Index!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);


        SpecialLabel labelPatchProgress1 = new SpecialLabel(" 0%", 17);
        labelPatchProgress1.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress1.setLocation(240,35);
        labelPatchProgress1.setSize(170, 32);
        labelPatchProgress1.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress1.setForeground(Color.BLACK);
        back.add(labelPatchProgress1);


        SpecialButton pcStartDownload = new SpecialButton("Start Install", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(20, 35);
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                boolean checkAdmin = checkForAdmin();

                if (checkAdmin) {
                    if (downloader1 != null) {
                        downloader1.cancelDownload();
                        System.out.println("downloader1 Steam stopped");
                    }
                    pcStartDownload.changeText("Restart Download");


                    JOptionPane.showMessageDialog(null, "The Download will start after pressing OK.", "Download started", JOptionPane.INFORMATION_MESSAGE);
                    downloader1 = new Downloader();
                    downloader1.startDownload("https://github.com/LibreVR/Revive/releases/latest/download/ReviveInstaller.exe", tempPath + "/revive", "/ReviveInstaller.exe", labelPatchProgress1, outFrame, null, 1, true, -1);
                    installRevive();
                }
                else{
                    System.out.println("The application is NOT running with administrative privileges.");
                    ErrorDialog runAsAdmin = new ErrorDialog();
                    runAsAdmin.errorDialog(outFrame, "Please restart as Admin", "<html>To install Revive, you need to restart this app as admin. To do that,<br>close the Installer completely. Then right click on EchoVR_Installer.exe<br>and click on Start as Admin.</html>", -1);

                }


            }
        });
        back.add(pcStartDownload);

        SpecialLabel reviveNote = createSpecialLabel("This will Install the newest version of Revive", 16);
        reviveNote.setLocation(20, 90);
        back.add(reviveNote);



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

    private void installRevive() {
        String installerPath = tempPath + "\\revive\\ReviveInstaller.exe";

        try {
            // Create a ProcessBuilder with the path to the installer
            ProcessBuilder processBuilder = new ProcessBuilder(installerPath);

            // Redirect error and output streams to the console
            processBuilder.redirectErrorStream(true);

            // Start the process (this will run the installer)
            Process process = processBuilder.start();

            // Capture output and error streams
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("OUTPUT: " + line);
            }

            // Wait for the installer process to finish
            int exitValue = process.waitFor();
            System.out.println("Installer exited with code: " + exitValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}
