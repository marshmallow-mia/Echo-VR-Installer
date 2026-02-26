package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static bl00dy_c0d3_.echovr_installer.Helpers.getFileAndReturnArray;

public class Downloader implements Runnable {
    private long fileSize = 0;
    private String fileUrl;
    private String localFilePath;
    private JLabel labelProgress;
    private JDialog frame;
    private JFrame frameMain;
    private String filename;
    private int platform = -1; //0=PC, 1=don't unzip
    private boolean flg_CancelDownload = false;
    boolean deleteFileBeforeDownloading;
    int downloadServer;
    boolean checkHashForUpdateFiles;
    private Runnable onComplete;


    // url, path, name, Label to change progress%, frame to know the position for errorDialog, platform=0=PC/unzip, 1=don't unzip, deleteFileBeforeDownloading, checkBestDownloadServer, check hashes (used for updates)
    public void startDownload(String fileUrl, String localFilePath, String filename, JLabel labelProgress, JDialog frame, JFrame frameMain, int platform, boolean deleteFileBeforeDownloading, int downloadServer, boolean checkHashForUpdateFiles) {
        this.localFilePath = localFilePath;
        this.labelProgress = labelProgress;
        this.frame = frame;
        this.frameMain = frameMain;
        this.filename = filename;
        this.platform = platform;
        this.deleteFileBeforeDownloading = deleteFileBeforeDownloading;
        this.downloadServer = downloadServer;
        this.checkHashForUpdateFiles = checkHashForUpdateFiles;

        if (downloadServer == 0) {
            String fastestServer = getDownloadSpeed(labelProgress, frame);
            this.fileUrl = fastestServer + fileUrl;
            fileUrl = this.fileUrl;
        }
        else{
            this.fileUrl = fileUrl;
        }

        fileSize = getFileSize(fileUrl);



        if (fileSize > 0) {
            System.out.println("Online File size: " + fileSize + " bytes");
            new Thread(this).start();
        } else {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while Downloading", "Couldn't finish Download. Please check your Ethernet or try again later. (ERR1)", 0);
        }
    }

    public void run() {

        System.out.println("Downloader Logs:");
        System.out.println("File URL: " + fileUrl);
        System.out.println("localFilePath: " + localFilePath);

        if (deleteFileBeforeDownloading) {
            try {
                Path path = Paths.get(localFilePath + "/" + filename);
                if (Files.exists(path)) {
                    Files.delete(path);
                    System.out.println("File exists...deleted");
                }
            } catch (Exception e) {
                System.out.println("Error wile checking for existing file or deleting");
                e.printStackTrace();
            }
        }



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


            if(existingFileSize < fileSize) {
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

            if (checkHashForUpdateFiles){
                try {
                    checkHash();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }

            if (platform == 0) {
                UnzipFile.unzip(frame, frameMain, localFilePath + "\\" + filename, localFilePath);
                JOptionPane.showMessageDialog(frame, "<html>Installation is done. Path is:<br>" + localFilePath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);

                // Suggest adding to Start, Taskbar, Quick Launch
                String exePath = localFilePath + "\\ready-at-dawn-echo-arena\\bin\\win10\\echovr.exe";
                int result = JOptionPane.showConfirmDialog(frame,
                        "Would you like to add Echo VR to your Start Menu and Desktop?\n\nThis will add: " + exePath,
                        "Add Echo VR Shortcut?",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        Helpers.addShortcutsAll(exePath, "Echo VR");
                        JOptionPane.showMessageDialog(frame, "Shortcuts added to Start Menu and Desktop.", "Shortcut Created", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Failed to create shortcut: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (platform == 3) {
                JOptionPane.showMessageDialog(frame, "<html>Installation is done. Path is:<br>" + localFilePath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
            }



        } catch (IOException e) {
            e.printStackTrace();
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while Downloading", "Couldn't finish Download. Please check your Ethernet or try again later. (ERR2)", 0);
        }


        if (onComplete != null) {
            onComplete.run();
        }
    }

    // Add a method to set the completion callback
    public void setOnCompleteListener(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public void cancelDownload() {
        flg_CancelDownload = true;
    }

    public void checkHash() throws IOException, NoSuchAlgorithmException {
        String[] fileHash = getFileAndReturnArray("https://echo.marceldomain.de:6969/updates/" + filename + ".hash",filename  + ".hash");
        Path filePath = Path.of(localFilePath + "/" + filename);
        System.out.println("File to check hash: " + filePath);

        byte[] data = Files.readAllBytes(filePath);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        String checksum = new BigInteger(1, hash).toString(16);
        System.out.println(checksum);


        if (fileHash[0].equals(checksum)){
            System.out.println("Same hash of: " + filename);
        }
        else{
            System.out.println("NOT the same hash of: " + filename);
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while Downloading", "Error when checking hash of updated files. Please restart (ERR3)", 0);
            cancelDownload();
        }
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

    // Choose the fastest available http server
    private static String getDownloadSpeed(JLabel labelProgress, JDialog frame) {
        String[] servers = new String[2];
        servers[0] = "https://echo.marceldomain.de:6969/";
        servers[1] = "https://evr.echo.taxi/";
        String testFile = "randomDownloadTestFile";

        labelProgress.setText("Testing download servers...");
        frame.repaint();
        System.out.println("Server Test started");

        String fastestServer = null;
        long fastestTime = Long.MAX_VALUE;

        int serverCount = servers.length;
        int tested = 0;
        for (String server : servers) {
            tested++;
            labelProgress.setText("Testing server " + tested + "/" + serverCount);
            frame.repaint();
            System.out.println("Testing server: " + server);
            long timeTaken = measureDownloadSpeed(server + testFile, 128 * 1024, 2000); // 128KB, 2s timeout
            System.out.println(server + ": " + timeTaken);
            if (timeTaken < fastestTime) {
                fastestTime = timeTaken;
                fastestServer = server;
            }
        }

        if (fastestServer == null) {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Server Test Failed", "Could not reach any download server. Please check your internet connection.", 0);
            // fallback to first server
            return servers[0];
        }
        return fastestServer;
    }

    private static long measureDownloadSpeed(String fileUrl) {
        return measureDownloadSpeed(fileUrl, 128 * 1024, 2000);
    }

    // Overloaded: limit bytes and timeout
    private static long measureDownloadSpeed(String fileUrl, int maxBytes, int timeoutMs) {
        long startTime = System.nanoTime();
        int totalRead = 0;
        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream())) {
            byte[] dataBuffer = new byte[1024];
            int read;
            long start = System.currentTimeMillis();
            while ((read = in.read(dataBuffer, 0, Math.min(1024, maxBytes - totalRead))) != -1) {
                totalRead += read;
                if (totalRead >= maxBytes) break;
                if (System.currentTimeMillis() - start > timeoutMs) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Long.MAX_VALUE; // If there's an error, return a large number to skip this server
        }
        long endTime = System.nanoTime();
        return endTime - startTime;
    }
}
