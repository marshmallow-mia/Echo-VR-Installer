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

        frame.dispose();
        UnzipDialog unzipFrame = new UnzipDialog(frameMain);

        SwingUtilities.invokeLater(() -> {
            unzipFrame.setVisible(true);
        });

        new Thread(() -> {
            System.out.println("extract");

            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
                        // some boilerplate ExecutorService crap
                        // regardless, there is probably a better way to implement this.
                        // I am just unsure of how
                        Future<?> task = executor.submit(() -> {
                            try {
                                extractFile(zipFile, entry, filePath);
                            } catch (IOException e) {
                                e.printStackTrace();
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
                error.errorDialog(frame, "Error while unzipping", "Couldn't finish unzipping. Please check storage Space.", 0);
                SwingUtilities.invokeLater(() -> {
                    unzipFrame.setClosable();
                });
            }
        }).start();
    }

    private static void extractFile(ZipFile zipFile, ZipEntry entry, String filePath) throws IOException {
        try (InputStream zipIn = zipFile.getInputStream(entry);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {

            byte[] bytesIn = new byte[65536]; // 64kb buffer
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
}
