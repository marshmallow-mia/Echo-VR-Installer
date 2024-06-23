package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Thread.sleep;

public class FrameQuestDownload extends JDialog {
    Downloader downloader = null;
    Downloader downloader2 = null;
    FrameMain frameMain = null;
    int frameWidth = 700;
    int frameHeight = 400;
    public int firstDownloadDone = 0;
    //Get the temp path
    Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    String configPath = "Optional: Choose config.json on the left";

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
        this.setTitle("Echo VR Installer v0.1");
        FrameQuestDownload outFrame = this;

        Background back = new Background("Echo2.jpg");
        back.setLayout(null);
        this.setContentPane(back);
        this.setModal(true);

        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>If you don't own Echo on your account don't use this Installer! Use the \"No licence patch\"<br>down below on the main menu instead and just close the next window!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

        SpecialLabel labelQuestProgress1 = new SpecialLabel("Progress = ", 17);
        labelQuestProgress1.setLocation(282,60);
        labelQuestProgress1.setSize(240, 38);
        labelQuestProgress1.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress1.setForeground(Color.BLACK);
        back.add(labelQuestProgress1);


        SpecialLabel labelQuestProgress2 = new SpecialLabel(" 0%", 15);
        labelQuestProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress2.setLocation(522,60);
        labelQuestProgress2.setSize(130, 19);
        labelQuestProgress2.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress2.setForeground(Color.BLACK);
        back.add(labelQuestProgress2);

        SpecialLabel labelQuestProgress3 = new SpecialLabel(" 0%", 15);
        labelQuestProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress3.setLocation(522,79);
        labelQuestProgress3.setSize(130, 19);
        labelQuestProgress3.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress3.setForeground(Color.BLACK);
        back.add(labelQuestProgress3);


        SpecialButton questStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 18);
        questStartDownload.setLocation(50, 60);
        questStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                JOptionPane.showMessageDialog(null, "The Download will start after pressing OK. Please wait for both files to be done!", "Download started", JOptionPane.INFORMATION_MESSAGE);
                downloader = new Downloader();
                downloader.startDownload("https://echo.marceldomain.de:6969/Echo_patched.apk", targetPath + "", "Echo_patched.apk", labelQuestProgress2, outFrame, 2);
                downloader2 = new Downloader();
                downloader2.startDownload("https://echo.marceldomain.de:6969/main.4987566.com.readyatdawn.r15.obb", targetPath + "", "main.4987566.com.readyatdawn.r15.obb", labelQuestProgress3, outFrame, 2);
                //TODO THE DOWNLOADS RUN SIMULTANEOUSLY. THATS KINDA TRASH!!!
            }
        });
        back.add(questStartDownload);

        SpecialLabel labelConfigPath = new SpecialLabel(configPath, 14);
        labelConfigPath.setLocation(280,140);
        labelConfigPath.setSize(410, 38);
        labelConfigPath.setBackground(new Color(255, 255, 255, 200));
        labelConfigPath.setForeground(Color.BLACK);
        back.add(labelConfigPath);

        SpecialButton chooseConfig = new SpecialButton("OPTIONAL CONFIG", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        chooseConfig.setLocation(50, 140);
        chooseConfig.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                fileChooser(labelConfigPath);
            }
        });
        back.add(chooseConfig);


        SpecialCheckBox checkBoxConfig = new SpecialCheckBox("Check this to use the custom config", 17);
        checkBoxConfig.setSize(500,30);
        checkBoxConfig.setLocation(50, 185);

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
                    int result = checkIfJavaIsInstalled();
                    if (result == 0){
                        ErrorDialog.errorDialog(outFrame, "Java not Found", "<html>No Java Runtime found. For the Config patch to work, you need to install the \"Java Runtime\"</html>");
                    }

                    return;

                    //apkfileName = "Echo_patched.apk";
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



    //This function checks if java is installed
    private int checkIfJavaIsInstalled(){
        Process process = null;
        try {
            process = new ProcessBuilder("javas", "-version").start();
        } catch (IOException e) {
            return 0;
        }
        // StringBuilder to accumulate the output
        StringBuilder stdErrResult = new StringBuilder();

        // Read the output from the process's input stream
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String stdout;
            while ((stdout = stdInput.readLine()) != null) {
                stdErrResult.append(stdout).append("\n"); // Append each line and a newline character
            }
            // Check if the stderr contains the word "version"
            if (stdErrResult.toString().toLowerCase().contains("version")) {
                System.out.println("Java is installed. Version information: " + stdErrResult);
                return 1; // Java is installed
            }
        }
        catch (IOException e){}
        //TODO ^
        return 0;
    }

    private void fileChooser(SpecialLabel labelPcDownloadPath){
        JFileChooser chooser;
        int result;

        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        //
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.repaint();
            System.out.println("getSelectedFile() : "
                    +  chooser.getSelectedFile());
            configPath = chooser.getSelectedFile().getPath();
            labelPcDownloadPath.setText(configPath);

        }
        else {
            System.out.println("No Selection ");
        }

    }
    //TODO Create Class from the fileChooser function



    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }

}
