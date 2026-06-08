package bl00dy_c0d3_.echovr_installer;




import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FramePCDownload extends JDialog {
    Downloader downloader = null;
    FrameMain frameMain = null;
    private WizardState wizardState;
    SpecialButton nextButton;
    private static final int SECTION_PADDING = 20;
    private static final int ITEM_GAP = 20;
    private static final Color SECTION_BOX_FILL = new Color(200, 0, 150, 90);
    private static final Color SECTION_BOX_BORDER = new Color(50, 50, 50, 150);
    private static final int HEADER_CONTENT_OFFSET = 49;
    int frameWidth = 700;
    int frameHeight = 419;
    String path = "C:/EchoVR";
    JDialog outFrame = this;
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
    //Constructor
    public FramePCDownload(FrameMain frameMain){
        this.frameMain = frameMain;
        this.wizardState = new WizardState();
        initComponents();
        this.setVisible(true);
    }

    public FramePCDownload(FrameMain frameMain, WizardState wizardState){
        this.frameMain = frameMain;
        this.wizardState = wizardState;
        initComponents();
    }


    public void dispose(){
        if (nextButton != null) {
            setVisible(false);
        }
        if (downloader != null){
            downloader.cancelDownload();
        }
        super.dispose();
    }


    private void initComponents(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.9.3b");
        this.setModal(true);

        Background back = new Background("EchoArena.jpg");
        back.setLayout(null);
        this.setContentPane(back);

        //Tipbox erstellen (muss vor den Buttons sein, damit hover listener darauf zugreifen können)
        TipBox tipBox = new TipBox();

        //Note before installing Echo
        //JOptionPane.showMessageDialog(this, "<html>If you own Echo on your Meta account, first download it officially, start it once and choose the path to the installation on the next screen!<br>If you don't own Echo on your account just proceed and use the patch afterwards!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

        SpecialLabel labelPcDownloadPath = new SpecialLabel(path, 14);
        labelPcDownloadPath.setLocation(182, 100);
        labelPcDownloadPath.setSize(490, 25);
        labelPcDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcDownloadPath);


        SpecialButton pcChooseOriginalPath = new SpecialButton("<html>Auto choose original<br>Oculus path</html>", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        pcChooseOriginalPath.setLocation(32, 40);
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForAdminAndOculusPath(outFrame);
                if (!newPath.matches("")) {
                    labelPcDownloadPath.setText(newPath + "Software\\Software\\");
                    outFrame.repaint();
                }
            }
        });
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Automatically find the original Oculus installation path");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcChooseOriginalPath);




        SpecialButton pcChoosePath = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 14);
        pcChoosePath.setLocation(32, 100);
        pcChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcDownloadPath, outFrame);
            }
        });
        pcChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Choose a custom folder for the Echo VR installation");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcChoosePath);




        SpecialLabel labelPcProgress1 = new SpecialLabel("Progress =", 17);
        labelPcProgress1.setLocation(264, 140);
        labelPcProgress1.setSize(155, 38);
        labelPcProgress1.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress1.setForeground(Color.BLACK);
        back.add(labelPcProgress1);


        SpecialLabel labelPcProgress2 = new SpecialLabel(" 0%", 17);
        labelPcProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPcProgress2.setLocation(419, 140);
        labelPcProgress2.setSize(170, 38);
        labelPcProgress2.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress2.setForeground(Color.BLACK);
        back.add(labelPcProgress2);


        FramePCDownload thisFrame = this;
        SpecialButton pcStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(32, 140);
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                if (downloader != null){
                    downloader.cancelDownload();
                    pause(1);
                }
                pcStartDownload.changeText("Restart Download");

                JOptionPane.showMessageDialog(null, "The Download will start after pressing OK.", "Download started", JOptionPane.INFORMATION_MESSAGE);
                Thread downloadThread1 = new Thread(() -> {
                    downloader = new Downloader();
                    downloader.setOnCompleteListener(() -> {
                        nextButton.setVisible(true);
                        SwingUtilities.invokeLater(() -> {
                            String[] updateFiles = getFileAndReturnArray("https://files.echovr.de/updates/files", "updateFiles");
                            String URL = "https://files.echovr.de/updates/";
                            //Download all updated files
                            for (String file : updateFiles) {
                                System.out.println("Updatefile:" + file);

                                Thread downloadThread2 = new Thread(() -> {
                                    downloader = new Downloader();
                                    downloader.startDownload(URL + file, labelPcDownloadPath.getText() + "/ready-at-dawn-echo-arena/bin/win10", file, labelPcProgress2, thisFrame, frameMain, 1, true, -1, true);
                                });

                                downloadThread2.start();  // This runs the download in a separate thread
                                System.out.println("UPDATE after regular install is DONE");
                            }
                        });
                    });
                    downloader.startDownload("ready-at-dawn-echo-arena.zip", labelPcDownloadPath.getText(), "ready-at-dawn-echo-arena.zip",  labelPcProgress2, thisFrame, frameMain, 0, false, 0, false);
                });

                downloadThread1.start();  // This runs the download in a separate thread
            }
        });
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Start downloading Echo VR to the selected path");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcStartDownload);

        // Next button - initially hidden, made visible after download completes
        nextButton = new SpecialButton("Next", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        nextButton.setLocation(32, 190);
        nextButton.setVisible(false);
        nextButton.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                wizardState.setInstallPath(labelPcDownloadPath.getText());
                WizardState.UserType userType = wizardState.getUserType();
                if (userType == WizardState.UserType.OWNER) {
                    dispose();
                    SwingUtilities.invokeLater(() -> {
                        OptionalPatchesPanel panel = new OptionalPatchesPanel(null, wizardState);
                        panel.setVisible(true);
                    });
                } else if (userType == WizardState.UserType.NEW_PLAYER) {
                    dispose();
                    SwingUtilities.invokeLater(() -> {
                        new FramePCPatcher();
                    });
                } else {
                    dispose();
                }
            }
        });
        back.add(nextButton);

        //Tipbox positionieren und hinzufügen...
        tipBox.setLocation((frameWidth - tipBox.getWidth()) / 2, frameHeight - tipBox.getHeight() - 60);
        back.add(tipBox);

        // --- Section Panel (single box around all content) ---
        int[] bounds = calcBounds(SECTION_PADDING,
            new int[]{pcChooseOriginalPath.getX(), pcChooseOriginalPath.getY(), pcChooseOriginalPath.getWidth(), pcChooseOriginalPath.getHeight()},
            new int[]{pcChoosePath.getX(), pcChoosePath.getY(), pcChoosePath.getWidth(), pcChoosePath.getHeight()},
            new int[]{labelPcDownloadPath.getX(), labelPcDownloadPath.getY(), labelPcDownloadPath.getWidth(), labelPcDownloadPath.getHeight()},
            new int[]{labelPcProgress1.getX(), labelPcProgress1.getY(), labelPcProgress1.getWidth(), labelPcProgress1.getHeight()},
            new int[]{labelPcProgress2.getX(), labelPcProgress2.getY(), labelPcProgress2.getWidth(), labelPcProgress2.getHeight()},
            new int[]{pcStartDownload.getX(), pcStartDownload.getY(), pcStartDownload.getWidth(), pcStartDownload.getHeight()},
            new int[]{nextButton.getX(), nextButton.getY(), nextButton.getWidth(), nextButton.getHeight()}
        );
        back.add(createSectionPanel(bounds[0], bounds[1], bounds[2], bounds[3]));
        // Section hover tip via mouse motion (does not block button events)
        final int[] box = bounds;
        back.addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int mx = e.getX(), my = e.getY();
                if (mx >= box[0] && mx <= box[0]+box[2] && my >= box[1] && my <= box[1]+box[3]) {
                    tipBox.showTip("Section tip");
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
