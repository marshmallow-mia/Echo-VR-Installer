package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;

public class UnzipDialog extends JDialog{
private int frameWidth = 900;
private int frameHeight = 200;
private JDialog zipStart;
private Background back;
private SpecialLabel zipLabel1;
private SpecialLabel zipLabel2;
private SpecialLabel zipLabel3;
private SpecialLabel zipLabel4;



    public void setClosable() {
        if (zipStart != null) {
            zipStart.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }}

    public void setDoneText() {
        if (zipStart != null) {
            zipLabel1.setText("Unzipping is done.");
            zipLabel1.setSize(zipLabel2.getPreferredSize());
            int XPos2 = (frameWidth / 2 - zipLabel1.getWidth() / 2);
            zipLabel1.setLocation(XPos2, 20);
            zipLabel2.setText("You can close this window now.");
            zipLabel2.setSize(zipLabel2.getPreferredSize());
            int XPos3 = (frameWidth / 2 - zipLabel2.getWidth() / 2);
            zipLabel2.setLocation(XPos3, 25  + zipLabel1.getHeight());
            back.add(zipLabel3);
            back.add(zipLabel4);
            back.repaint();
        }}


    public void unzipFrame(JDialog frame) {
        zipStart = new JDialog();
        zipStart.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        back = new Background("vr_lounge_banner.png");
        back.setSize(frameWidth, frameHeight);
        back.setLayout(null);
        zipStart.setContentPane(back);

        zipLabel1 = new SpecialLabel("Unzip will start now.", 16);
        zipLabel1.setSize(zipLabel1.getPreferredSize());
        int XPos2 = (frameWidth / 2 - zipLabel1.getWidth() / 2);
        zipLabel1.setLocation(XPos2 - 8, 20);
        back.add(zipLabel1);

        zipLabel2 = new SpecialLabel("Please wait a minute!", 16);
        zipLabel2.setSize(zipLabel2.getPreferredSize());
        int XPos3 = (frameWidth / 2 - zipLabel2.getWidth() / 2);
        zipLabel2.setLocation(XPos3 - 8, 25  + zipLabel1.getHeight());
        back.add(zipLabel2);

        //This Label will be active after the unzip is done
        zipLabel3 = new SpecialLabel("If you already predownloaded Echo from Oculus, you can start Echo now!", 16);
        zipLabel3.setSize(zipLabel3.getPreferredSize());
        int XPos4 = (frameWidth / 2 - zipLabel3.getWidth() / 2);
        zipLabel3.setLocation(XPos4 - 8, 80);


        //This Label will be active after the unzip is done
        zipLabel4 = new SpecialLabel("If you dont own Echo on Oculus, use the \"No Licence Patch\" on the Main Menu!", 16);
        zipLabel4.setSize(zipLabel4.getPreferredSize());
        int XPos5 = (frameWidth / 2 - zipLabel4.getWidth() / 2);
        zipLabel4.setLocation(XPos5 - 8, 105);




        // TODO Y Calculation it Trash and Wrong!
        // TODO Same for the X Pos due to the Windows-Border


        back.setVisible(true);

        zipStart.setVisible(true);

        zipStart.pack();
        zipStart.setSize(frameWidth, frameHeight);
        int XPos1 = ( frame.getWidth() /2 + frame.getX()) - frameWidth / 2;
        int YPos1 = ( frame.getHeight() /2 + frame.getY()) - frameHeight / 2;
        zipStart.setLocation(XPos1, YPos1);
    }

}
