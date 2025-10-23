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
    int frameWidth = 700;
    int frameHeight = 394;
    String path = "C:/EchoVR";
    JDialog outFrame = this;
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
    //Constructor
    public FramePCDownload(FrameMain frameMain){
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
        this.setTitle("Echo VR Installer v0.8.5 ");
        this.setModal(true);

        Background back = new Background("EchoArena.jpg");
        back.setLayout(null);
        this.setContentPane(back);


        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>If you own Echo on your Meta account, first download it officially, start it once and choose the path to the installation on the next screen!<br>If you don't own Echo on your account just proceed and use the patch afterwards!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

        SpecialLabel labelPcDownloadPath = new SpecialLabel(path, 14);
        labelPcDownloadPath.setLocation(170,130);
        labelPcDownloadPath.setSize(490, 25);
        labelPcDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcDownloadPath);


        SpecialButton pcChooseOriginalPath = new SpecialButton("<html>Auto choose original<br>Oculus path</html>", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 14);
        pcChooseOriginalPath.setLocation(20, 70);
        pcChooseOriginalPath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                String newPath = checkForAdminAndOculusPath(outFrame);
                if (!newPath.matches("")) {
                    labelPcDownloadPath.setText(newPath + "Software\\Software\\");
                    outFrame.repaint();
                }
            }
        });
        back.add(pcChooseOriginalPath);


        SpecialLabel labelPcOculusPathExplaination = new SpecialLabel("Choose this to use the original Oculus path", 14);
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


        SpecialLabel labelPcDownloadPathExplaination = new SpecialLabel("Specify the Path for the Echo Installation or leave it as it is.", 14);
        labelPcDownloadPathExplaination.setLocation(20,160);
        back.add(labelPcDownloadPathExplaination);


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


        FramePCDownload thisFrame = this;
        SpecialButton pcStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(20, 230);
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
    private java.awt.Image loadGUI(String imageName) {
        URL imageURL = getClass().getClassLoader().getResource(imageName);
        if (imageURL == null) return null;
        else return (new ImageIcon(imageURL, imageName)).getImage();
    }


}
