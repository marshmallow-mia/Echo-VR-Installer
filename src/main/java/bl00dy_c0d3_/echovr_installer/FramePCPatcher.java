package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FramePCPatcher extends JDialog {
    int frameWidth = 1280;
    int frameHeight = 720;
    String path = "C:/EchoVR/ready-at-dawn-echo-arena";
    //TODO use already used path from FramePCPatcher
    Downloader downloadPatch;
    FramePCPatcher outFrame = this;


    //Constructor
    public FramePCPatcher(){
        initComponents();
        this.setVisible(true);
    }

    private void initComponents(){

    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setResizable(false);
    this.setIconImage(loadGUI("icon.png"));
    this.setTitle("Echo VR Installer v0.8.5");
    this.setModal(true);

    Background back = new Background("echo-in-arena.png");
    back.setLayout(null);
    this.setContentPane(back);

    // Add reminder to install Echo VR first
    SpecialLabel installReminder = new SpecialLabel("Make sure Echo VR is installed before patching!", 16);
    installReminder.setForeground(Color.RED);
    installReminder.setLocation(40, 10);
    back.add(installReminder);


        SpecialLabel discordLink = new SpecialLabel("1. Join the Echo VR Patcher Disocrd Server:", 16);
        discordLink.setLocation(40, 40);
        back.add(discordLink);


        //SpecialHyperlink hyperlinkPC = new SpecialHyperlink(60, 70, "<html><a href=''>Join the Echo VR Patcher Discord-Server</a></html>", "https://discord.gg/bMpsva6fmA");
        SpecialHyperlink hyperlinkPC = new SpecialHyperlink(40, 95, "Click on me to join the Echo VR Patcher Discord-Server", "https://discord.gg/bMpsva6fmA", 14);
        back.add(hyperlinkPC);

        SpecialLabel react_discord1 = new SpecialLabel("2. React to the message  on Discord", 16);
        react_discord1.setLocation(40, 135);
        back.add(react_discord1);

        SpecialLabel react_discord2 = new SpecialLabel("by clicking on the disc:", 16);
        react_discord2.setLocation(40, 165);
        back.add(react_discord2);

        Background reactToMessageImg = new Background("pc_react.png");
        back.setLayout(null);
        reactToMessageImg.setLocation(40, 220);
        reactToMessageImg.setSize(182,108);
        reactToMessageImg.setVisible(true);
        back.add(reactToMessageImg);


        SpecialLabel copyLink1 = new SpecialLabel("3. You will receive a private Message from the", 16);
        copyLink1.setLocation(40, 352);
        back.add(copyLink1);

        SpecialLabel copyLink2 = new SpecialLabel("\"EchoSignUp\" Bot. Right Click on the file", 16);
        copyLink2.setLocation(40, 382);
        back.add(copyLink2);

        SpecialLabel copyLink3 = new SpecialLabel("and select Copy Link. NOT COPY MESSAGE LINK!", 16);
        copyLink3.setLocation(40, 412);
        back.add(copyLink3);


        Background copyLinkImg = new Background("copy_linkPC.png");
        back.setLayout(null);
        copyLinkImg.setLocation(40, 465);
        copyLinkImg.setSize(279,177);
        copyLinkImg.setVisible(true);
        back.add(copyLinkImg);

        SpecialLabel enterLink = new SpecialLabel("4. Paste the link with CTRL-V:", 16);
        enterLink.setLocation(582, 40);
        back.add(enterLink);

        SpecialTextfield textfieldPCPatchLink = new SpecialTextfield();
        textfieldPCPatchLink.specialTextfield(630, 30, 582, 87, 13);
        back.add(textfieldPCPatchLink);

        SpecialLabel choosePath1 = new SpecialLabel("5. Choose your path or leave it ", 16);
        choosePath1.setLocation(582, 135);
        back.add(choosePath1);

        SpecialLabel choosePath2 = new SpecialLabel("as it is, if it is correct already:", 16);
        choosePath2.setLocation(582, 165);
        back.add(choosePath2);


        SpecialLabel labelPcPatchDownloadPath = new SpecialLabel(path, 14);
        labelPcPatchDownloadPath.setLocation(740,285);
        labelPcPatchDownloadPath.setSize(450, 25);
        labelPcPatchDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcPatchDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcPatchDownloadPath);


        SpecialButton pcChooseOriginalPath = new SpecialButton("<html>Auto choose original<br>Oculus path</html>", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        pcChooseOriginalPath.setLocation(582, 225);
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForAdminAndOculusPath(outFrame);
                if (!newPath.matches("")) {
                    labelPcPatchDownloadPath.setText(newPath + "Software\\Software\\ready-at-dawn-echo-arena");
                    outFrame.repaint();
                }
            }
        });
        back.add(pcChooseOriginalPath);

        SpecialLabel labelPcOculusPathExplaination = new SpecialLabel("Choose this to use the original Oculus path", 14);
        labelPcOculusPathExplaination.setLocation(814,225);
        back.add(labelPcOculusPathExplaination);








        SpecialButton pcPatchChoosePath = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 14);
        pcPatchChoosePath.setLocation(582, 285);
        pcPatchChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcPatchDownloadPath, outFrame);
            }
        });
        back.add(pcPatchChoosePath);

        SpecialLabel startPatch_btn1 = new SpecialLabel("6. Start Patching by pressing", 16);
        startPatch_btn1.setLocation(582, 333);
        back.add(startPatch_btn1);

        SpecialLabel startPatch_btn2 = new SpecialLabel("this button:", 16);
        startPatch_btn2.setLocation(582, 363);
        back.add(startPatch_btn2);


        SpecialLabel patchProgress = new SpecialLabel(" 0%", 18);
        patchProgress.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        patchProgress.setLocation(887,415);
        patchProgress.setSize(100, 50);
        patchProgress.setBackground(new Color(255, 255, 255, 200));
        patchProgress.setForeground(Color.BLACK);
        back.add(patchProgress);

        SpecialButton pcStartPatch = new SpecialButton("Start patching", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        pcStartPatch.setLocation(585, 415);
        pcStartPatch.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String link = textfieldPCPatchLink.getText();

                if (link.matches("https://cdn.discordapp.com/attachments/.*/pnsovr.dll.*")){
                    File echoPath = new File(labelPcPatchDownloadPath.getText() + "/bin/win10");
                    if (!echoPath.exists() && !echoPath.isDirectory()) {
                        ErrorDialog error = new ErrorDialog();
                        error.errorDialog(outFrame, "Incorrect path to EchoVR", "Error: Choose the main directory of Echo. Like: C:\\echovr\\ready-at-dawn-echo-arena", 0);
                    }
                    else {
                        if (downloadPatch != null){
                            downloadPatch.cancelDownload();
                            pause(1);
                        }
                        pcStartPatch.changeText("Restart Patching");

                        System.out.println(link);
                        downloadPatch = new Downloader();
                        downloadPatch.startDownload(textfieldPCPatchLink.getText(), echoPath + "", "pnsovr.dll", patchProgress, outFrame, null, 3, true, -1, false);
                    }
                }
                else{
                    ErrorDialog error = new ErrorDialog();
                    error.errorDialog(outFrame, "Wrong URL provided", "Your provided Download Link is wrong. Please check!", 0);

                }


            }
        });
        back.add(pcStartPatch);

        //Alles fertig machen...
        this.pack();
        System.out.println(this.getInsets());
        //(this.getHeight() - this.getInsets().top)
        //TODO set positions correctly everywhere

        //Fenstergröße und Position setzen...
        this.setSize(frameWidth, frameHeight);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (d.width - this.getWidth()) / 2;
        int y = (d.height - this.getHeight()) / 2;
        this.setLocation(x, y);
    }

    //Lädt eine GUI-Grafik und gibt sie zurück:
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }
}
