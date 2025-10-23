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
    int frameWidth = 700;
    int frameHeight = 394;
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
        this.setTitle("Echo VR Installer v0.8.5");
        this.setModal(true);

        Background back = new Background("EchoArena.jpg");
        back.setLayout(null);
        this.setContentPane(back);


        SpecialLabel labelPcDownloadPath = new SpecialLabel(path, 14);
        labelPcDownloadPath.setLocation(170,130);
        labelPcDownloadPath.setSize(490, 25);
        labelPcDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcDownloadPath);


        SpecialButton pcChooseOriginalPath = new SpecialButton("<html>Auto choose Echo path</html>", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 18);
        pcChooseOriginalPath.setLocation(20, 70);
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
        back.add(pcChooseOriginalPath);


        SpecialLabel labelPcOculusPathExplaination = new SpecialLabel("Choose this to search Echo on known paths", 14);
        labelPcOculusPathExplaination.setLocation(252,70);
        back.add(labelPcOculusPathExplaination);


        SpecialButton pcChoosePath = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 14);
        pcChoosePath.setLocation(20, 130);
        pcChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                pathFolderChooser(labelPcDownloadPath, outFrame);
            }
        });
        back.add(pcChoosePath);


        SpecialLabel labelPcDownloadPathExplaination = new SpecialLabel("Specify the echovr.exe location", 14);
        labelPcDownloadPathExplaination.setLocation(20,160);
        SpecialLabel labelPcDownloadPathExplaination2 = new SpecialLabel("Its located inside your echo install folder in  \"bin/win10\"", 14);
        labelPcDownloadPathExplaination2.setLocation(20,187);
        back.add(labelPcDownloadPathExplaination);
        back.add(labelPcDownloadPathExplaination2);


        SpecialLabel labelPcProgress1 = new SpecialLabel("Progress =", 17);
        labelPcProgress1.setLocation(252,230);
        labelPcProgress1.setSize(155, 38);
        labelPcProgress1.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress1.setForeground(Color.BLACK);
        back.add(labelPcProgress1);


        SpecialLabel labelPcProgress2 = new SpecialLabel(" 0%", 17);
        labelPcProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPcProgress2.setLocation(407,230);
        labelPcProgress2.setSize(170, 38);
        labelPcProgress2.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress2.setForeground(Color.BLACK);
        back.add(labelPcProgress2);


        FramePCEchoUpdate thisFrame = this;
        SpecialButton pcStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(20, 230);
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
        back.add(pcStartDownload);

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


}
