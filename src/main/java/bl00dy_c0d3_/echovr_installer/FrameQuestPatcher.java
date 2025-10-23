package bl00dy_c0d3_.echovr_installer;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;


import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FrameQuestPatcher extends JDialog {
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;
    private static final String DEFAULT_PATH = "C:\\EchoVR";
    private final Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    static boolean isChrome = checkIfChromeOs();
    private SpecialTextfield textfieldQuestPatchLink;
    private SpecialLabel labelConfigPath;
    private SpecialCheckBox checkBoxConfig;
    private Downloader downloader = null;
    private Downloader downloader2 = null;
    SpecialButton questStartDownload;
    JDialog outFrame = this;

    public FrameQuestPatcher() {
        initComponents();
        this.setVisible(true);
    }


    @Override
    public void dispose() {
        if (downloader != null) {
            downloader.cancelDownload();
        }
        if (downloader2 != null) {
            downloader2.cancelDownload();
        }
        super.dispose();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setIconImage(loadGUI("icon.png"));
        setTitle("Echo VR Installer v0.8.5");

        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setModal(true);
        setContentPane(createContentPane());

        pack();
        centerFrame(this, FRAME_WIDTH, FRAME_HEIGHT);
    }

    private @NotNull JPanel createContentPane() {
        Background back = new Background("echo-vr-cierra.png");
        back.setLayout(null);

        addSpecialLabels(back);
        addSpecialHyperlinks(back);
        addImages(back);
        addTextFields(back);
        addButtons(back);
        addCheckBoxes(back);

        return back;
    }

    private void handleDownloadButtonClick() {

        if (textfieldQuestPatchLink.getText().matches("https://files.echovr.de.*")) {
            JOptionPane.showMessageDialog(null, "The Download will start after pressing OK. Please wait for both files to be done!", "Download started", JOptionPane.INFORMATION_MESSAGE);
            if (downloader != null){
                downloader.cancelDownload();
                System.out.println("downloader1 stopped");
            }
            if (downloader2 != null){
                downloader2.cancelDownload();
                System.out.println("downloader2 stopped");
            }
            questStartDownload.changeText("Restart Download");



            Thread downloadThread1 = new Thread(() -> {
                downloader = new Downloader();
                String fixedURL = textfieldQuestPatchLink.getText().replace("org", "org/dl");
                downloader.startDownload(fixedURL, targetPath.toString(), "personilizedechoapk.apk", labelQuestProgress2, this, null, 2, true, -1, false);
            });

            downloadThread1.start();  // This runs the download in a separate thread





            pause(1);


            Thread downloadThread2 = new Thread(() -> {
                downloader2 = new Downloader();
                            downloader2.startDownload("_data.zip", targetPath.toString(), "_data.zip", labelQuestProgress3, this, null, 2, false, 0, false);
            });

            downloadThread2.start();  // This runs the download in a separate thread





        } else {
            new ErrorDialog().errorDialog(this, "Wrong URL provided", "Your provided Download Link is wrong. Please check!", 0);
        }
    }

    private void handleChooseConfigClick() {
        jsonFileChooser(labelConfigPath, outFrame);
    }

    private void handlePatchingButtonClick() {
        labelQuestProgress4.setText("Installation started! Wait!");
        outFrame.repaint();
        JOptionPane.showMessageDialog(outFrame, "<html>Press OK to start the installation. It can take a minute to install!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
        String apkfileName;
        apkfileName = "personilizedechoapk.apk";

        InstallerQuest installtoQuest = new InstallerQuest();
        boolean installState = installtoQuest.installAPK(targetPath.toString(), apkfileName, "_data.zip", labelQuestProgress4, this);

        if (installState) {
            labelQuestProgress4.setText("Installation is complete!");
            outFrame.repaint();
            JOptionPane.showMessageDialog(outFrame, "<html>Installation of Echo is done. You can start it now on your Quest.<br> DON'T CLICK ON RESTORE IF YOU WILL GET ASKED TO OR YOU NEED TO REINSTALL AGAIN!</html>", "Have Fun!", JOptionPane.INFORMATION_MESSAGE);
        }
        else{
            labelQuestProgress4.setText("Installation did not finish!");
            outFrame.repaint();
        }
    }





    SpecialLabel labelQuestProgress2 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelQuestProgress3 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelQuestProgress4 = new SpecialLabel("Not started yet", 18);

    private void addSpecialLabels(@NotNull JPanel back) {
        back.add(Helpers.createSpecialLabel("1. Join the Echo VR Patcher Discord Server:", 16, 40, 40));
        back.add(Helpers.createSpecialLabel("2. React to the message on Discord", 16, 40, 135));
        back.add(Helpers.createSpecialLabel("by clicking on the smiley:", 16, 40, 165));

        back.add(Helpers.createSpecialLabel("3. You will receive a private Message from the", 16, 40, 335));
        back.add(Helpers.createSpecialLabel("\"EchoSignUp\" Bot. Right Click on the blue URL ", 16, 40, 365));
        back.add(Helpers.createSpecialLabel("and select Copy Link. NOT COPY MESSAGE LINK!", 16, 40, 395));

        back.add(Helpers.createSpecialLabel("4. Paste the link with CTRL-V:", 16, 582, 40));
        back.add(Helpers.createSpecialLabel("5. Start the Download Process:", 16, 582, 170));
        //back.add(Helpers.createSpecialLabel("Error 204", 16, 582, 295));
        back.add(Helpers.createSpecialLabel("6. After the Download above is finished, click this button:", 16, 582, 300));
        back.add(Helpers.createSpecialLabel("Progress = ", 17, 810, 210, new Dimension(130, 38), Color.BLACK, Color.WHITE));

        String configPath = "Error 204";
        labelConfigPath = Helpers.createSpecialLabel(configPath, 14, 582, 455, new Dimension(600, 25), Color.BLACK, Color.WHITE);
        //back.add(labelConfigPath);

        //THIS NEED TO BE SET MANUALLY, AS I NEED TO ACCESS IT LATER
        labelQuestProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress2.setLocation(940,210);
        labelQuestProgress2.setSize(130, 19);
        labelQuestProgress2.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress2.setForeground(Color.BLACK);
        back.add(labelQuestProgress2);

        labelQuestProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress3.setLocation(940,229);
        labelQuestProgress3.setSize(130, 19);
        labelQuestProgress3.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress3.setForeground(Color.BLACK);
        back.add(labelQuestProgress3);

        labelQuestProgress4.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress4.setLocation(885,340);
        labelQuestProgress4.setSize(300, 50);
        labelQuestProgress4.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress4.setForeground(Color.BLACK);
        back.add(labelQuestProgress4);

    }

    private void addSpecialHyperlinks(@NotNull JPanel back) {
        back.add(new SpecialHyperlink(40, 95, "Click on me to join the Echo VR Patcher Discord-Server", "https://discord.gg/bMpsva6fmA", 14));
    }

    private void addBackgroundImage(@NotNull JPanel back, String imagePath, int x, int y, int width, int height) {
        Background image = new Background(imagePath);
        image.setLocation(x, y);
        image.setSize(width, height);
        image.setVisible(true);
        back.add(image);
    }


    private void addImages(JPanel back) {
        addBackgroundImage(back, "quest_react.png", 40, 215, 182, 108);
        addBackgroundImage(back, "copy_linkQuest.png", 40, 465, 279, 177);
    }

    private void addTextFields(@NotNull JPanel back) {
        textfieldQuestPatchLink = new SpecialTextfield();
        textfieldQuestPatchLink.specialTextfield(630, 30, 582, 87, 13);
        back.add(textfieldQuestPatchLink);
    }

    private void addButtons(JPanel back) {
        addStartDownloadButton(back);
        addChooseConfigButton(back);
        addStartPatchingButton(back);
    }

    private void addCheckBoxes(@NotNull JPanel back) {
        checkBoxConfig = new SpecialCheckBox("Error 204", 17);
        checkBoxConfig.setSize(500, 30);
        checkBoxConfig.setLocation(582, 525);
        checkBoxConfig.setOpaque(true);
        checkBoxConfig.setBackground(new Color(50, 50, 50));
        //back.add(checkBoxConfig);
    }

    private void addStartDownloadButton(@NotNull JPanel back) {
        questStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        questStartDownload.setLocation(582, 210);
        questStartDownload.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                handleDownloadButtonClick();
            }
        });
        back.add(questStartDownload);
    }


    private void addChooseConfigButton(@NotNull JPanel back) {
        SpecialButton chooseConfig = new SpecialButton("Error 204", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        chooseConfig.setLocation(582, 405);
        chooseConfig.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                handleChooseConfigClick();
            }
        });
        //back.add(chooseConfig);
    }

    private void addStartPatchingButton(@NotNull JPanel back) {
        SpecialButton pcStartPatch = new SpecialButton("Start patching", "button_up.png", "button_down.png", "button_highlighted.png", 18);
        pcStartPatch.setLocation(585, 340);
        pcStartPatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                handlePatchingButtonClick();
            }
        });
        back.add(pcStartPatch);
    }
}
