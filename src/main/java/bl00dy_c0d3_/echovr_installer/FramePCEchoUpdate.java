package bl00dy_c0d3_.echovr_installer;




import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class FramePCEchoUpdate extends JDialog {
    Downloader downloader = null;
    FrameMain frameMain = null;
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
    public FramePCEchoUpdate(FrameMain frameMain){
        this.frameMain = frameMain;
        initComponents();
        this.setVisible(true);
    }


    public void dispose(){
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

        SpecialLabel labelPcDownloadPath = new SpecialLabel(path, 14);
        labelPcDownloadPath.setLocation(182,100);
        labelPcDownloadPath.setSize(490, 25);
        labelPcDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcDownloadPath);


        SpecialButton pcChooseOriginalPath = new SpecialButton("<html>Auto choose Echo path</html>", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 18);
        pcChooseOriginalPath.setLocation(32, 40);
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForEchoOnKnownPaths(outFrame);
                if (!newPath.matches("")) {
                    System.out.println("Echo found at path: " + newPath);
                    JOptionPane.showMessageDialog(outFrame, "<html>echovr.exe was found at the following path. If thats wrong, set the path manually!!!<br>" + newPath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
                    labelPcDownloadPath.setText(newPath);
                    outFrame.repaint();
                }
            }
        });
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Automatically search for Echo VR on known installation paths");
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
                tipBox.showTip("Manually specify the location of echovr.exe");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcChoosePath);


        SpecialLabel labelPcProgress1 = new SpecialLabel("Progress =", 17);
        labelPcProgress1.setLocation(264,140);
        labelPcProgress1.setSize(155, 38);
        labelPcProgress1.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress1.setForeground(Color.BLACK);
        back.add(labelPcProgress1);


        SpecialLabel labelPcProgress2 = new SpecialLabel(" 0%", 17);
        labelPcProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPcProgress2.setLocation(419,140);
        labelPcProgress2.setSize(170, 38);
        labelPcProgress2.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress2.setForeground(Color.BLACK);
        back.add(labelPcProgress2);


        FramePCEchoUpdate thisFrame = this;
        SpecialButton pcStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(32, 140);
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                if (downloader != null){
                    downloader.cancelDownload();
                    pause(1);
                }
                pcStartDownload.changeText("Restart Download");

                String filePath  = labelPcDownloadPath.getText();
                if (Files.exists(Path.of(filePath + "echovr.exe"))) {
                    System.out.println("echovr.exe does exist: " + filePath);
                    String[] updateFiles = getFileAndReturnArray("https://files.echovr.de/updates/files", "updateFiles");
                    String URL = "https://files.echovr.de/updates/";
                    //Download all updated files
                    for (String file : updateFiles) {
                        System.out.println("Updatefile:" + file);

                        Thread downloadThread1 = new Thread(() -> {
                            downloader = new Downloader();
                            downloader.setOnCompleteListener(() -> {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(null, "Updating is successfull! ", "Update done", JOptionPane.INFORMATION_MESSAGE);

                                });
                            });
                            downloader.startDownload(URL + file, labelPcDownloadPath.getText(), file, labelPcProgress2, thisFrame, frameMain, 1, true, -1, true);
                        });

                        downloadThread1.start();  // This runs the download in a separate thread
                    }
                } else {
                    System.out.println("echovr.exe does not exist: " + filePath);
                    JOptionPane.showMessageDialog(null, "Wrong path to echovr.exe. Choose the right path please!", "Wrong path", JOptionPane.INFORMATION_MESSAGE);

                }



            }
        });
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                tipBox.showTip("Start downloading the Echo VR update to the selected path");
            }
            public void mouseExited(MouseEvent event) {
                tipBox.showDefault();
            }
        });
        back.add(pcStartDownload);

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
            new int[]{pcStartDownload.getX(), pcStartDownload.getY(), pcStartDownload.getWidth(), pcStartDownload.getHeight()}
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
    private Image loadGUI(String imageName) {
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
