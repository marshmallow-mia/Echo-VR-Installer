package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FrameQuestPatcher extends JDialog {
    int frameWidth = 1280;
    int frameHeight = 720;
    String path = "C:\\\\EchoVR";
    //TODO use already used path from FramePCPatcher
    Downloader downloader = null;
    Downloader downloader2 = null;
    //Get the temp path
    Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");


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
        this.setTitle("Echo VR Installer v0.1");
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
                    downloader2 = new Downloader();
                    downloader2.startDownload("https://echo.marceldomain.de:6969/main.4987566.com.readyatdawn.r15.obb", targetPath + "", "main.4987566.com.readyatdawn.r15.obb", labelQuestProgress3, outFrame, 2);
                    //TODO THE DOWNLOADS RUN SIMULTANEOUSLY. THATS KINDA TRASH!!!
                }
                else{
                    ErrorDialog.errorDialog(outFrame, "Wrong URL provided", "Your provided Download Link is wrong. Please check!");
                }

            }
        });
        back.add(questStartDownload);

        SpecialLabel startPatch_btn1 = new SpecialLabel("6. After the Download above is finished start ", 16);
        startPatch_btn1.setLocation(582, 283);
        back.add(startPatch_btn1);

        SpecialLabel startPatch_btn2 = new SpecialLabel("this button:", 16);
        startPatch_btn2.setLocation(582, 313);
        back.add(startPatch_btn2);


        SpecialLabel labelPatchProgress = new SpecialLabel(" Not started yet", 18);
        labelPatchProgress.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress.setLocation(900,365);
        labelPatchProgress.setSize(330, 50);
        labelPatchProgress.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress.setForeground(Color.BLACK);
        back.add(labelPatchProgress);


        SpecialButton pcStartPatch = new SpecialButton("Start patching", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        pcStartPatch.setLocation(585, 365);
        FrameQuestPatcher outframe = this;
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String apkfileName = "personilizedechoapk.apk";
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
            path = chooser.getSelectedFile().getPath();
            labelPcDownloadPath.setText(path);

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
