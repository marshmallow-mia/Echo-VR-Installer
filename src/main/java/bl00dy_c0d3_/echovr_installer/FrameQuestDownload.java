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


//import static bl00dy_c0d3_.echovr_installer.Helpers.jsonFileChooser;
import static bl00dy_c0d3_.echovr_installer.Helpers.*;

// TODO: Remove in v0.9.0 — replaced by FrameGuidanceQuest
/**
 * @deprecated Replaced by {@link FrameGuidanceQuest}. Will be removed in a future version.
 */
@Deprecated
public class FrameQuestDownload extends JDialog {
    Downloader downloader = null;
    Downloader downloader2 = null;
    FrameMain frameMain = null;
    private static final int SECTION_PADDING = 20;
    private static final int ITEM_GAP = 20;
    private static final Color SECTION_BOX_FILL = new Color(200, 0, 150, 90);
    private static final Color SECTION_BOX_BORDER = new Color(50, 50, 50, 150);
    private static final int HEADER_CONTENT_OFFSET = 49;
    int frameWidth = 700;
    int frameHeight = 380;
    public int firstDownloadDone = 0;
    //Get the temp path
    Path targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "echo/");
    String configPath = "Error 204";
    JDialog outFrame = this;
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static boolean isChrome = checkIfChromeOs();
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
    SpecialButton questStartDownload;
    SpecialButton questStartPatching;
    TipBox tipBox;


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
        this.setTitle("Echo VR Installer v0.9.4b.003");
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
        //Tipbox erstellen (muss vor den Buttons sein, damit hover listener darauf zugreifen können)
        tipBox = new TipBox();

        Background back = new Background("Echo2.jpg");
        back.setLayout(null);
        addSpecialLabels(back);
        addSpecialCheckBox(back);
        addStartDownloadButton(back);
        addChooseConfigButton(back);
        addQuestStartPatchingButton(back);

        //Tipbox positionieren und hinzufügen...
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 46);
        back.add(tipBox);

        // --- Section Panel (single box around all content) ---
        int[] bounds = calcBounds(SECTION_PADDING,
            new int[]{questStartDownload.getX(), questStartDownload.getY(), questStartDownload.getWidth(), questStartDownload.getHeight()},
            new int[]{labelQuestProgress2.getX(), labelQuestProgress2.getY(), labelQuestProgress2.getWidth(), labelQuestProgress2.getHeight()},
            new int[]{labelQuestProgress3.getX(), labelQuestProgress3.getY(), labelQuestProgress3.getWidth(), labelQuestProgress3.getHeight()},
            new int[]{questStartPatching.getX(), questStartPatching.getY(), questStartPatching.getWidth(), questStartPatching.getHeight()},
            new int[]{labelQuestInstallProgress.getX(), labelQuestInstallProgress.getY(), labelQuestInstallProgress.getWidth(), labelQuestInstallProgress.getHeight()}
        );
        back.add(createSectionPanel(bounds[0], bounds[1], bounds[2], bounds[3]));
        // Section hover tip via mouse motion (does not block button events)
        final int[] box = bounds;
        back.addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                if (mx >= box[0] && mx <= box[0]+box[2] && my >= box[1] && my <= box[1]+box[3]) {
                    /* tipBox.showTip("Section tip"); */
                } else {
                    tipBox.showDefault();
                }
            }
        });

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



        JOptionPane.showMessageDialog(this, "The Download will start after pressing OK. Please wait for both files to be done!", "Download started", JOptionPane.INFORMATION_MESSAGE);

        questStartDownload.changeText("Restart Download");
        Thread downloadThread = new Thread(() -> {
            downloader = new Downloader();
            downloader.startDownload("r15_26-06-25.apk", targetPath + "", "r15_26-06-25.apk",  labelQuestProgress2, outFrame, null, 2, false, 0, false);
        });

        downloadThread.start();


        pause(2);


        Thread downloadThread2 = new Thread(() -> {
            downloader2 = new Downloader();
            downloader2.startDownload("_data.zip", targetPath + "", "_data.zip",  labelQuestProgress3, outFrame, null, 2, false, 0, false);
        });

        downloadThread2.start();

    }



    private void handleQuestStartPatchingButtonClick() {
        String apkfileName;
        labelQuestInstallProgress.setText("Installation started! Wait!");
        outFrame.repaint();
        JOptionPane.showMessageDialog(outFrame, "<html>Press OK to start the installation. It can take a minute to install!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);





        apkfileName = "r15_26-06-25.apk";

        String obbfileName = "_data.zip";
        InstallerQuest installToQuest = new InstallerQuest();
        boolean installState = installToQuest.installAPK(targetPath + "", apkfileName, obbfileName,labelQuestInstallProgress, outFrame);

        if (installState) {
            labelQuestInstallProgress.setText("Installation is complete!");
            outFrame.repaint();
            JOptionPane.showMessageDialog(outFrame, "<html>Installation of Echo is done. You can start it now on your Quest.<br> DON'T CLICK ON RESTORE IF YOU WILL GET ASKED TO OR YOU NEED TO REINSTALL AGAIN!</html>", "Have Fun!", JOptionPane.INFORMATION_MESSAGE);

        }
        else{
            labelQuestInstallProgress.setText("Installation did not finish!");
            outFrame.repaint();
        }

    }


    //Needs to be declared outside, as its needed outside
    SpecialLabel labelQuestProgress2 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelQuestProgress3 = new SpecialLabel(" 0%", 15);
    SpecialLabel labelConfigPath = new SpecialLabel(configPath, 14);
    SpecialLabel labelQuestInstallProgress = new SpecialLabel("Not started yet", 20);


    private void addSpecialLabels(@NotNull JPanel back) {
        back.add(Helpers.createSpecialLabel("Progress = ", 17, 267, 40, new Dimension(240, 38), Color.BLACK, new Color(255, 255, 255, 200)));


        //Progressbar
        labelQuestProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress2.setLocation(507,40);
        labelQuestProgress2.setSize(159, 19);
        labelQuestProgress2.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress2.setForeground(Color.BLACK);
        back.add(labelQuestProgress2);

        labelQuestProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestProgress3.setLocation(507,59);
        labelQuestProgress3.setSize(159, 19);
        labelQuestProgress3.setBackground(new Color(255, 255, 255, 200));
        labelQuestProgress3.setForeground(Color.BLACK);
        back.add(labelQuestProgress3);

        //ConfigPath
        labelConfigPath.setLocation(35,100);
        labelConfigPath.setSize(600, 25);
        labelConfigPath.setBackground(new Color(255, 255, 255, 200));
        labelConfigPath.setForeground(Color.BLACK);
        //back.add(labelConfigPath);

        //InstallProgress
        labelQuestInstallProgress.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelQuestInstallProgress.setLocation(335,100);
        labelQuestInstallProgress.setSize(330, 50);
        labelQuestInstallProgress.setBackground(new Color(255, 255, 255, 200));
        labelQuestInstallProgress.setForeground(Color.BLACK);
        back.add(labelQuestInstallProgress);


    }


    //Needs to be declared outside, as its needed outside
    SpecialCheckBox checkBoxConfig = new SpecialCheckBox("Error 204", 17);
    private void addSpecialCheckBox(@NotNull JPanel back) {
        checkBoxConfig.setSize(500,30);
        checkBoxConfig.setLocation(60, 130);

        //JCheckBoxen werden Panel hinzugefügt
        //back.add(checkBoxConfig);


    }

    private void addStartDownloadButton(@NotNull JPanel back) {
        questStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        questStartDownload.setLocation(35, 40);
        questStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                handleDownloadButtonClick();
            }
        });
        questStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Download the Echo VR APK and data files");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(questStartDownload);

    }

    private void addChooseConfigButton(@NotNull JPanel back) {
        SpecialButton chooseConfig = new SpecialButton("Error 204", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 15);
        chooseConfig.setLocation(60, 51);
        chooseConfig.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                jsonFileChooser(labelConfigPath, outFrame);
            }
        });
        //back.add(chooseConfig);
    }


    private void addQuestStartPatchingButton(@NotNull JPanel back) {
        questStartPatching = new SpecialButton("Install Echo to Quest", "button_up.png", "button_down.png", "button_highlighted.png", 15);
        questStartPatching.setLocation(35, 100);
        questStartPatching.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                handleQuestStartPatchingButtonClick();
            }
        });
        questStartPatching.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Install the downloaded APK and data to your Quest");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
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

    private int[] calcBounds(int pad, int[]... rects) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxR = Integer.MIN_VALUE, maxB = Integer.MIN_VALUE;
        for (int[] r : rects) {
            minX = Math.min(minX, r[0]);
            minY = Math.min(minY, r[1]);
            maxR = Math.max(maxR, r[0] + r[2]);
            maxB = Math.max(maxB, r[1] + r[3]);
        }
        return new int[]{minX - pad, minY - pad, maxR - minX + pad * 2, maxB - minY + pad * 2};
    }

    private JPanel createSectionPanel(int x, int y, int w, int h) {
        JPanel panel = new JPanel(null) {
            @Override public boolean contains(int x, int y) { return false; }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SECTION_BOX_FILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), SECTION_PADDING, SECTION_PADDING);
                g2.setColor(SECTION_BOX_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, SECTION_PADDING, SECTION_PADDING);
                g2.dispose();
            }
        };
        panel.setBounds(x, y, w, h);
        return panel;
    }

}
