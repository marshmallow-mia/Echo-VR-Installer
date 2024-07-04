package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader implements Runnable {
    private long fileSize = 0;
    private String fileUrl;
    private String localFilePath;
    private JLabel labelProgress;
    private JDialog frame;
    private String filename;
    private int platform = -1; //0=PC, 1=don't unzip
    private boolean flg_CancelDownload = false;


    // url, path, name, Label to change progress%, frame to know the position for errorDialog, platform=0=PC/unzip, 1=don't unzip
    public void startDownload(String fileUrl, String localFilePath, String filename, JLabel labelProgress, JDialog frame, int platform){
        this.localFilePath = localFilePath;
        this.labelProgress = labelProgress;
        this.frame = frame;
        this.fileUrl = fileUrl;
        this.filename = filename;
        this.platform = platform;
        fileSize = getFileSize(fileUrl);

        if (fileSize > 0) {
            System.out.println("File size: " + fileSize + " bytes");
            new Thread(this).start();
        }
        else {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while Downloading", "Couldn't finish Download. Please check your Ethernet or try again later.", 0);
        }
    }





    public void run() {
        System.out.println("1");
        File theDir = new File(localFilePath);
        if (!theDir.exists()){
            System.out.println("2");
            theDir.mkdirs();
        }
        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(localFilePath + "/" + filename)) {
            System.out.println("3");
            long downloadProgress = 0;
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                if (flg_CancelDownload){
                    in.close();
                    fileOutputStream.close();
                    return;
                }
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                downloadProgress = downloadProgress + bytesRead;
                System.out.println(downloadProgress);
                System.out.println(fileSize);
                double  progressPercent = (100.0/fileSize*downloadProgress);
                String.format("%.2f", progressPercent);

                if (labelProgress != null) {
                    labelProgress.setText(" " + String.format("%.2f", progressPercent) + "%");
                    frame.repaint();
                }
            }


        } catch (IOException e) {
            // handle exception
            System.out.println("4");
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while Downloading", "Couldn't finish Download. Please check your Ethernet or try again later.", 0);


        }


    }

    public void cancelDownload(){
        flg_CancelDownload = true;
    }


    private long getFileSize(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return connection.getContentLengthLong();
            } else {
                return 0;
            }
        }
        catch(Exception e){
            return 0;
        }
    }

}
