package bl00dy_c0d3_.echovr_installer;

import com.frostwire.jlibtorrent.SessionManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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

public class FrameQuestPatcher extends JDialog {
    int frameWidth = 1280;
    int frameHeight = 720;
    String path = "C:\\\\EchoVR";
    //TODO use already used path from FramePCPatcher
    Downloader downloader = null;
    TorrentDownload downloader2 = null;
    //Get the temp path
    Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    String configPath = "Optional: Choose config.json on the button above";



    //Constructor
    public FrameQuestPatcher(){
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
        this.setTitle("Echo VR Installer v0.4");
        this.setModal(true);
        Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
        FrameQuestPatcher outFrame = this;





        Background back = new Background("echo-vr-cierra.png");
        back.setLayout(null);
        this.setContentPane(back);


        SpecialLabel discordLink = new SpecialLabel("1. Join the Echo VR Patcher Disocrd Server:", 16);
        discordLink.setLocation(40, 40);
        back.add(discordLink);


        //SpecialHyperlink hyperlinkPC = new SpecialHyperlink(60, 70, "<html><a href=''>Join the Echo VR Patcher Discord-Server</a></html>", "https://discord.gg/bMpsva6fmA");
        SpecialHyperlink hyperlinkPC = new SpecialHyperlink(40, 95, "Click on me to join the Echo VR Patcher Discord-Server", "https://discord.gg/bMpsva6fmA", 14);
        back.add(hyperlinkPC);

        SpecialLabel react_discord1 = new SpecialLabel("2. React to the message  on Discord", 16);
        react_discord1.setLocation(40, 135);
        back.add(react_discord1);

        SpecialLabel react_discord2 = new SpecialLabel("by clicking on the smiley:", 16);
        react_discord2.setLocation(40, 165);
        back.add(react_discord2);

        Background reactToMessageImg = new Background("quest_react.png");
        back.setLayout(null);
        reactToMessageImg.setLocation(40, 220);
        reactToMessageImg.setSize(182,108);
        reactToMessageImg.setVisible(true);
        back.add(reactToMessageImg);


        SpecialLabel copyLink1 = new SpecialLabel("3. You will receive a private Message from the", 16);
        copyLink1.setLocation(40, 352);
        back.add(copyLink1);

        SpecialLabel copyLink2 = new SpecialLabel("\"EchoSignUp\" Bot. Right Click on the blue URL", 16);
        copyLink2.setLocation(40, 382);
        back.add(copyLink2);

        SpecialLabel copyLink3 = new SpecialLabel("and select Copy Link. NOT COPY MESSAGE LINK!", 16);
        copyLink3.setLocation(40, 412);
        back.add(copyLink3);


        Background copyLinkImg = new Background("copy_linkQuest.png");
        back.setLayout(null);
        copyLinkImg.setLocation(40, 465);
        copyLinkImg.setSize(279,177);
        copyLinkImg.setVisible(true);
        back.add(copyLinkImg);

        SpecialLabel enterLink = new SpecialLabel("4. Paste the link with CTRL-V:", 16);
        enterLink.setLocation(582, 40);
        back.add(enterLink);

        SpecialTextfield textfieldQuestPatchLink = new SpecialTextfield();
        textfieldQuestPatchLink.specialTextfield(630, 30, 582, 87, 13);
        back.add(textfieldQuestPatchLink);


        SpecialLabel startDownload_btn1 = new SpecialLabel("5. Start the Download Process:", 16);
        startDownload_btn1.setLocation(582, 155);
        back.add(startDownload_btn1);

        SpecialLabel labelQuestProgress1 = new SpecialLabel("Progress = ", 17);
        labelQuestProgress1.setLocation(810,205);
        labelQuestProgress1.setSize(130, 38);
        labelQuestProgress1.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress1.setForeground(Color.BLACK);
        back.add(labelQuestProgress1);


        SpecialLabel labelQuestProgress2 = new SpecialLabel(" 0%", 15);
        labelQuestProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress2.setLocation(940,205);
        labelQuestProgress2.setSize(130, 19);
        labelQuestProgress2.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress2.setForeground(Color.BLACK);
        back.add(labelQuestProgress2);

        SpecialLabel labelQuestProgress3 = new SpecialLabel(" 0%", 15);
        labelQuestProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress3.setLocation(940,224);
        labelQuestProgress3.setSize(130, 19);
        labelQuestProgress3.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress3.setForeground(Color.BLACK);
        back.add(labelQuestProgress3);



        SpecialButton questStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 18);
        questStartDownload.setLocation(582, 205);
        questStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {

                if (textfieldQuestPatchLink.getText().matches("https://tmpfiles.org.*")) {
                    JOptionPane.showMessageDialog(null, "The Download will start after pressing OK. Please wait for both files to be done!", "Download started", JOptionPane.INFORMATION_MESSAGE);
                    downloader = new Downloader();
                    //String needs to be corrected to download it
                    String fixedURL = textfieldQuestPatchLink.getText().replace("org", "org/dl");
                    System.out.println(fixedURL);
                    downloader.startDownload(fixedURL, targetPath + "", "personilizedechoapk.apk", labelQuestProgress2, outFrame, 2);
                    System.out.println(targetPath + "");
                    SessionManager sessionManager = new SessionManager();
                    sessionManager.start();

                    downloader2 = new TorrentDownload(sessionManager);
                    downloader2.startDownload("p2pFiles/obb.torrent", targetPath + "", "main.4987566.com.readyatdawn.r15.obb",  labelQuestProgress3, outFrame, null, 2);
                }
                else{
                    ErrorDialog error = new ErrorDialog();
                    error.errorDialog(outFrame, "Wrong URL provided", "Your provided Download Link is wrong. Please check!", 0);
                }

            }
        });
        back.add(questStartDownload);



        SpecialLabel optionalConfig = new SpecialLabel("5(a). Optional config.json. Dont use if you dont need to:", 16);
        optionalConfig.setLocation(582, 295);
        back.add(optionalConfig);

        SpecialLabel labelConfigPath = new SpecialLabel(configPath, 14);
        labelConfigPath.setLocation(582,395);
        labelConfigPath.setSize(600, 25);
        labelConfigPath.setBackground(new Color(255, 255, 255, 200));
        labelConfigPath.setForeground(Color.BLACK);

        back.add(labelConfigPath);

        SpecialButton chooseConfig = new SpecialButton("OPTIONAL CONFIG", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        chooseConfig.setLocation(582, 345);
        chooseConfig.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                fileChooser(labelConfigPath);
            }
        });
        back.add(chooseConfig);


        SpecialCheckBox checkBoxConfig = new SpecialCheckBox("Check this to use the custom config", 17);
        checkBoxConfig.setSize(500,30);
        checkBoxConfig.setLocation(582, 425);
        checkBoxConfig.setOpaque(true);
        checkBoxConfig.setBackground(new Color(50, 50, 50));

        //JCheckBoxen werden Panel hinzugefügt
        back.add(checkBoxConfig);




        SpecialLabel startPatch_btn1 = new SpecialLabel("6. After the Download above is finished start ", 16);
        startPatch_btn1.setLocation(582, 505);
        back.add(startPatch_btn1);

        SpecialLabel startPatch_btn2 = new SpecialLabel("this button:", 16);
        startPatch_btn2.setLocation(582, 535);
        back.add(startPatch_btn2);


        SpecialLabel labelPatchProgress = new SpecialLabel(" Not started yet", 18);
        labelPatchProgress.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress.setLocation(900,587);
        labelPatchProgress.setSize(330, 50);
        labelPatchProgress.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress.setForeground(Color.BLACK);
        back.add(labelPatchProgress);


        SpecialButton pcStartPatch = new SpecialButton("Start patching", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        pcStartPatch.setLocation(585, 587);
        FrameQuestPatcher outframe = this;
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String apkfileName;
                if (checkBoxConfig.isSelected()){
                    int result = checkIfJavaIsInstalled();
                    if (result == 0){
                        ErrorDialog error = new ErrorDialog();
                        error.errorDialog(outFrame, "Java not Found", "<html>No Java Runtime found. For the Config patch to work, you need to install the \"Java Runtime\"</html>", 2);
                        return;
                    }

                    File f = new File(targetPath + "/personilizedechoapk.apk");
                    if(f.exists() && !f.isDirectory()) {
                        PatchAPK patchAPK = new PatchAPK();
                        patchAPK.patchAPK(targetPath + "", "personilizedechoapk.apk", labelConfigPath.getText(), labelConfigPath, outFrame);
                        Object resultPatcher = patchAPK.patchAPK(targetPath + "", "personilizedechoapk.apk", labelConfigPath.getText(), labelConfigPath, outFrame);
                        if (resultPatcher.equals(-1)){
                            return;
                        }
                    }
                    else {
                        ErrorDialog error2 = new ErrorDialog();
                        error2.errorDialog(outFrame, "Echo not found", "Echo wasn't found. Please use the Download Button first", 2);
                        return;
                    }
                    apkfileName = "changedConfig-aligned-debugSigned.apk";
                }
                else {
                    apkfileName = "personilizedechoapk.apk";
                }



                String obbfileName = "main.4987566.com.readyatdawn.r15.obb";
                InstallerQuest installtoQuest = new InstallerQuest();
                installtoQuest.installAPK(targetPath + "", apkfileName, obbfileName, labelPatchProgress, outframe);

            }
        });
        back.add(pcStartPatch);

        //Alles fertig machen...
        this.pack();

        //Fenstergröße und Position setzen...
        this.setSize(frameWidth, frameHeight);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (d.width - this.getWidth()) / 2;
        int y = (d.height - this.getHeight()) / 2;
        this.setLocation(x, y);
    }



    //This function checks if java is installed
    private int checkIfJavaIsInstalled(){
        Process process = null;
        try {
            process = new ProcessBuilder("java", "-version").start();
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
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
        chooser.setFileFilter(filter);
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
