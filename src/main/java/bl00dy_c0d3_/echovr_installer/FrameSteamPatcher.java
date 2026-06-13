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
    private static final int SECTION_PADDING = 20;
    private static final int ITEM_GAP = 20;
    private static final Color SECTION_BOX_FILL = new Color(200, 0, 150, 90);
    private static final Color SECTION_BOX_BORDER = new Color(50, 50, 50, 150);
    private static final int HEADER_CONTENT_OFFSET = 49;
    int frameWidth = 600;
    int frameHeight = 350;
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
        this.setTitle("Echo VR Installer v0.9.4b.002");
        this.setModal(true);


        Background back = new Background("echo_combat1.png");
        back.setLayout(null);
        this.setContentPane(back);

        //Tipbox erstellen (muss vor den Buttons sein, damit hover listener darauf zugreifen können)
        TipBox tipBox = new TipBox();

        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>This patch is only needed if you want to use a not Oculus capable Headset like the Valve Index!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);


        SpecialLabel labelPatchProgress1 = new SpecialLabel(" 0%", 17);
        labelPatchProgress1.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress1.setLocation(285,35);
        labelPatchProgress1.setSize(170, 38);
        labelPatchProgress1.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress1.setForeground(Color.BLACK);
        back.add(labelPatchProgress1);


        SpecialButton pcStartDownload = new SpecialButton("Start Install", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(65, 35);
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
                    downloader1.setOnCompleteListener(() -> {
                        SwingUtilities.invokeLater(() -> {
                            installRevive();
                        });
                    });
                    downloader1.startDownload("https://github.com/LibreVR/Revive/releases/download/3.1.1/ReviveInstaller.exe", tempPath + "/revive", "/ReviveInstaller.exe", labelPatchProgress1, outFrame, null, 1, true, -1, false);

                }
                else{
                    System.out.println("The application is NOT running with administrative privileges.");
                    ErrorDialog runAsAdmin = new ErrorDialog();
                    runAsAdmin.errorDialog(outFrame, "Please restart as Admin", "<html>To install Revive, you need to restart this app as admin. To do that,<br>close the Installer completely. Then right click on EchoVR_Installer.exe<br>and click on Start as Admin.</html>", -1);

                }


            }
        });
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Download and install the latest version of Revive");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcStartDownload);

        SpecialLabel reviveNote = new SpecialLabel("This will Install the newest version of Revive", 14);
        reviveNote.setLocation(65, 90);
        reviveNote.setSize(450, 25);
        reviveNote.setBackground(new Color(255, 255, 255, 200));
        reviveNote.setForeground(Color.BLACK);
        back.add(reviveNote);

        //Tipbox positionieren und hinzufügen...
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 50 );
        back.add(tipBox);

        // --- Section Panel (single box around all content) ---
        int[] bounds = calcBounds(SECTION_PADDING,
            new int[]{pcStartDownload.getX(), pcStartDownload.getY(), pcStartDownload.getWidth(), pcStartDownload.getHeight()},
            new int[]{labelPatchProgress1.getX(), labelPatchProgress1.getY(), labelPatchProgress1.getWidth(), labelPatchProgress1.getHeight()},
            new int[]{reviveNote.getX(), reviveNote.getY(), reviveNote.getWidth(), reviveNote.getHeight()}
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

    // Berechnet den umschließenden Rechteck aus mehreren Content-Rechtecken + Padding
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

    // Erstellt ein Section-Panel mit purpurnem Hintergrund und abgerundeten Ecken
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
