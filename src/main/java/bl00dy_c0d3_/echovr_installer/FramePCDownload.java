package bl00dy_c0d3_.echovr_installer;




import com.frostwire.jlibtorrent.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class FramePCDownload extends JDialog {
    TorrentDownload downloader = null;
    FrameMain frameMain = null;
    int frameWidth = 700;
    int frameHeight = 394;
    String path = "C:/EchoVR";


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
        this.setTitle("Echo VR Installer v0.3");
        this.setModal(true);

        Background back = new Background("EchoArena.jpg");
        back.setLayout(null);
        this.setContentPane(back);

        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>If you own Echo on your Meta account, first download it officially, start it once and choose the path to the installation on the next screen!<br>If you don't own Echo on your account just proceed and use the patch afterwards!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

        SpecialLabel labelPcDownloadPath = new SpecialLabel(path, 14);
        labelPcDownloadPath.setLocation(170,100);
        labelPcDownloadPath.setSize(490, 25);
        labelPcDownloadPath.setBackground(new Color(255, 255, 255, 200));
        labelPcDownloadPath.setForeground(Color.BLACK);
        back.add(labelPcDownloadPath);

        SpecialButton pcChoosePath = new SpecialButton("Choose path", "button_up_small.png", "button_down_small.png", "button_highlighted_small.png", 14);
        pcChoosePath.setLocation(20, 100);
        pcChoosePath.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                fileChooser(labelPcDownloadPath);

            }
        });
        back.add(pcChoosePath);


        SpecialLabel labelPcDownloadPathExplenation = new SpecialLabel("Specify the Path for the Echo Installation or leave it as it is.", 14);
        labelPcDownloadPathExplenation.setLocation(20,130);
        back.add(labelPcDownloadPathExplenation);


        SpecialLabel labelPcProgress1 = new SpecialLabel("Progress =", 17);
        labelPcProgress1.setLocation(252,200);
        labelPcProgress1.setSize(155, 38);
        labelPcProgress1.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress1.setForeground(Color.BLACK);
        back.add(labelPcProgress1);


        SpecialLabel labelPcProgress2 = new SpecialLabel(" 0%", 17);
        labelPcProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPcProgress2.setLocation(407,200);
        labelPcProgress2.setSize(170, 38);
        labelPcProgress2.setBackground(new Color(255, 255, 255, 200));
        labelPcProgress2.setForeground(Color.BLACK);
        back.add(labelPcProgress2);


        FramePCDownload thisFrame = this;
        SpecialButton pcStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(20, 200);
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                JOptionPane.showMessageDialog(null, "The Download will start after pressing OK.", "Download started", JOptionPane.INFORMATION_MESSAGE);
                //downloader = new Downloader();
                //downloader.startDownload("https://echo.marceldomain.de:6969/ready-at-dawn-echo-arena.zip", path, "\\echovr.zip", labelPcProgress2, thisFrame, 0);

                SessionManager sessionManager = new SessionManager();
                sessionManager.start();

                downloader = new TorrentDownload(sessionManager);
                downloader.startDownload("p2pFiles/pc.torrent", path, "ready-at-dawn-echo-arena.zip",  labelPcProgress2, thisFrame, frameMain, 0);

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
