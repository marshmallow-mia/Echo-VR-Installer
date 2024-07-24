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
    private JFrame frameMain;
    private String filename;
    private int platform = -1; //0=PC, 1=don't unzip //TODO not needed anymore
    private boolean flg_CancelDownload = false;

    // url, path, name, Label to change progress%, frame to know the position for errorDialog, platform=0=PC/unzip, 1=don't unzip
    public void startDownload(String fileUrl, String localFilePath, String filename, JLabel labelProgress, JDialog frame, JFrame frameMain, int platform, int checkAvailability) {
        this.localFilePath = localFilePath;
        this.labelProgress = labelProgress;
        this.frame = frame;
        this.frameMain = frameMain;
        this.fileUrl = fileUrl;
        this.filename = filename;
        this.platform = platform;
        fileSize = getFileSize(fileUrl);

        if (fileSize > 0) {
            System.out.println("File size: " + fileSize + " bytes");
            new Thread(this).start();
        } else {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while Downloading", "Couldn't finish Download. Please check your Ethernet or try again later. (ERR1)", 0);
        }
    }

    public void run() {
        File file = new File(localFilePath);
        if (!file.exists()) {
            if (file.mkdirs()) {
                System.out.println("Directory created successfully");
            } else {
                System.out.println("Failed to create directory");
                new ErrorDialog().errorDialog(frame, "Restart as Admin", "<html>You tried to download into a path that requires Admin rights.<br>Restart the App in Admin Mode!</html>", -1);
                return; // Exit if the directory could not be created
            }
        } else {
            System.out.println("Directory already exists");
        }

        File outputFile = new File(localFilePath, filename);
        long existingFileSize = outputFile.exists() ? outputFile.length() : 0;




        try {

            URL url = new URL(fileUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("HEAD");
            httpConn.connect();
            long contentLength = httpConn.getContentLength();
            httpConn.disconnect();

            if(existingFileSize < contentLength) {
                        System.out.println("STARTED");

                HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
                if (existingFileSize > 0) {
                    connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }

                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("Server responded with code: " + responseCode);
                }

                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                     RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {

                    raf.seek(existingFileSize);
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    long downloadProgress = existingFileSize;

                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        if (flg_CancelDownload) {
                            return;
                        }
                        raf.write(dataBuffer, 0, bytesRead);
                        downloadProgress += bytesRead;
                        double progressPercent = (100.0 / fileSize * downloadProgress);
                        labelProgress.setText(String.format("%.2f", progressPercent) + "%");
                        frame.repaint();
                    }
                }

            }
            else{
                System.out.println("Already downloaded");
                labelProgress.setText("100.00%");
                frame.repaint();
            }


            if (platform == 0) {
                UnzipFile.unzip(frame, frameMain, localFilePath + "\\" + filename, localFilePath);
                JOptionPane.showMessageDialog(frame, "<html>Installation is done. Path is:<br>" + localFilePath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
            } else if (platform == 3) {
                JOptionPane.showMessageDialog(frame, "<html>Installation is done. Path is:<br>" + localFilePath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
            }



        } catch (IOException e) {
            e.printStackTrace();
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while Downloading", "Couldn't finish Download. Please check your Ethernet or try again later. (ERR2)", 0);
        }
    }

    public void cancelDownload() {
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
        } catch (Exception e) {
            return 0;
        }
    }
}
