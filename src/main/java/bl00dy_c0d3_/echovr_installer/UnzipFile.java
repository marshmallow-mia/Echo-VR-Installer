package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.util.zip.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class UnzipFile {

    public static void unzip(JDialog frame, JFrame frameMain, String zipFilePath, String destDirectory) {

        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Check available disk space before extraction
        long requiredSpace = 0;
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    requiredSpace += entry.getSize();
                }
            }
        } catch (IOException e) {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while unzipping", "Couldn't read zip file. Please check the file and try again.", 0);
            return;
        }

        File rootDrive = new File(destDir.getAbsolutePath()).getAbsoluteFile();
        while (rootDrive.getParentFile() != null) {
            rootDrive = rootDrive.getParentFile();
        }
        final String driveLetter = rootDrive.getAbsolutePath();
        long usableSpace = destDir.getUsableSpace();
        if (usableSpace < requiredSpace) {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Storage Full", "Not enough space on drive: " + driveLetter + " (needed: " + (requiredSpace / (1024*1024)) + " MB, available: " + (usableSpace / (1024*1024)) + " MB). Please free up space or choose another location.", 0);
            return;
        }

        frame.dispose();
        UnzipDialog unzipFrame = new UnzipDialog(frameMain);


        SwingUtilities.invokeLater(() -> {
            unzipFrame.setVisible(true);
        });

        new Thread(() -> {
            System.out.println("extract");

            // Detect if running on SSD or HDD (simple heuristic: SSD if C: exists and is not removable)
            int maxThreads = 2;
            try {
                File cDrive = new File("C:\\");
                if (cDrive.exists() && cDrive.getTotalSpace() > 0 && cDrive.getUsableSpace() > 0) {
                    // Assume SSD if C: is large and not removable
                    maxThreads = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
                }
            } catch (Exception ignore) {}
            ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
            List<Future<?>> tasks = new ArrayList<>();

            try (ZipFile zipFile = new ZipFile(zipFilePath)) {
                var entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String filePath = destDirectory + File.separator + entry.getName();

                    if (entry.isDirectory()) {
                        File dir = new File(filePath);
                        dir.mkdirs();
                    } else {
                        Future<?> task = executor.submit(() -> {
                            try {
                                extractFile(zipFile, entry, filePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                                ErrorDialog error = new ErrorDialog();
                                error.errorDialog(frame, "Extraction Error", "Failed to extract file: " + entry.getName() + "\nReason: " + e.getMessage(), 0);
                            }
                        });
                        tasks.add(task);
                    }
                }

                for (Future<?> task : tasks) {
                    task.get();
                }

                executor.shutdown();

                SwingUtilities.invokeLater(() -> {
                    unzipFrame.setDoneText();
                    unzipFrame.setClosable();
                });
                System.out.println("done");

            } catch (Exception e) {
                ErrorDialog error = new ErrorDialog();
                error.errorDialog(frame, "Error while unzipping", "Couldn't finish unzipping. Error: " + e.getMessage() + "\nDrive: " + driveLetter, 0);
                SwingUtilities.invokeLater(() -> {
                    unzipFrame.setClosable();
                });
            }
        }).start();
    }

    private static void extractFile(ZipFile zipFile, ZipEntry entry, String filePath) throws IOException {
        // Use NIO Files.copy for faster file operations and a larger buffer
        try (InputStream zipIn = zipFile.getInputStream(entry)) {
            java.nio.file.Path outPath = java.nio.file.Paths.get(filePath);
            java.nio.file.Files.createDirectories(outPath.getParent());
            // Use REPLACE_EXISTING to overwrite if needed
            java.nio.file.Files.copy(zipIn, outPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
