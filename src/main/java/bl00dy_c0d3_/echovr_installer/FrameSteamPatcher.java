package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

public class FrameSteamPatcher extends JDialog {
    Downloader downloader1 = null;
    Downloader downloader2 = null;
    Downloader downloader3 = null;
    Downloader downloader4 = null;
    FrameMain frameMain = null;
    int frameWidth = 700;
    int frameHeight = 394;
    String path = "C://EchoVR/ready-at-dawn-echo-arena";
    FrameSteamPatcher outframe = this;



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
        if (downloader2 != null){
            downloader2.cancelDownload();
        }
        if (downloader3 != null){
            downloader3.cancelDownload();
        }
        if (downloader4 != null){
            downloader4.cancelDownload();
        }
        super.dispose();
    }


    private void initComponents(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(loadGUI("icon.png"));
        this.setTitle("Echo VR Installer v0.3");
        this.setModal(true);


        Background back = new Background("echo_combat1.png");
        back.setLayout(null);
        this.setContentPane(back);

        //Note before installing Echo
        JOptionPane.showMessageDialog(this, "<html>This patch is only needed if you want to use a not Oculus capable Headset like the Valve Index!</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);



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


        SpecialLabel labelPatchProgress1 = new SpecialLabel(" 0%", 17);
        labelPatchProgress1.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress1.setLocation(240,180);
        labelPatchProgress1.setSize(170, 32);
        labelPatchProgress1.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress1.setForeground(Color.BLACK);
        back.add(labelPatchProgress1);

        SpecialLabel labelPatchProgress2 = new SpecialLabel(" 0%", 17);
        labelPatchProgress2.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress2.setLocation(240,212);
        labelPatchProgress2.setSize(170, 32);
        labelPatchProgress2.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress2.setForeground(Color.BLACK);
        back.add(labelPatchProgress2);

        SpecialLabel labelPatchProgress3 = new SpecialLabel(" 0%", 17);
        labelPatchProgress3.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress3.setLocation(240,244);
        labelPatchProgress3.setSize(170, 32);
        labelPatchProgress3.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress3.setForeground(Color.BLACK);
        back.add(labelPatchProgress3);

        SpecialLabel labelPatchProgress4 = new SpecialLabel(" 0%", 17);
        labelPatchProgress4.setHorizontalAlignment(SwingConstants.LEFT);  // Set text alignment to left
        labelPatchProgress4.setLocation(240,276);
        labelPatchProgress4.setSize(170, 32);
        labelPatchProgress4.setBackground(new Color(255, 255, 255, 200));
        labelPatchProgress4.setForeground(Color.BLACK);
        back.add(labelPatchProgress4);


        SpecialButton pcStartDownload = new SpecialButton("Start Download", "button_up_middle.png", "button_down_middle.png", "button_highlighted_middle.png", 17);
        pcStartDownload.setLocation(20, 230);
        pcStartDownload.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent event) {
                File echoPath = new File(path + "/bin/win10");

                if (!echoPath.exists() && !echoPath.isDirectory()) {
                    ErrorDialog error = new ErrorDialog();
                    error.errorDialog(outframe, "Incorrect path to EchoVE", "Error: Choose the main directory of Echo. Like: C:\\echovr\\ready-at-dawn-echo-arena", 0);
                }
                else {
                    JOptionPane.showMessageDialog(null, "The Download will start after pressing OK.", "Download started", JOptionPane.INFORMATION_MESSAGE);
                    downloader1 = new Downloader();
                    downloader1.startDownload("https://echo.marceldomain.de:6969/LibRevive64.dll", echoPath + "", "/LibRevive64.dll", labelPatchProgress1, outframe, 1);
                    downloader2 = new Downloader();
                    downloader2.startDownload("https://echo.marceldomain.de:6969/openvr_api64.dll", echoPath + "", "/openvr_api64.dll", labelPatchProgress2, outframe, 1);
                    downloader3 = new Downloader();
                    downloader3.startDownload("https://echo.marceldomain.de:6969/xinput1_3.dll", echoPath + "", "/xinput1_3.dll", labelPatchProgress3, outframe, 1);
                    downloader4 = new Downloader();
                    downloader4.startDownload("https://echo.marceldomain.de:6969/xinput9_1_0.dll", echoPath + "", "/xinput9_1_0.dll", labelPatchProgress4, outframe, 1);
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
