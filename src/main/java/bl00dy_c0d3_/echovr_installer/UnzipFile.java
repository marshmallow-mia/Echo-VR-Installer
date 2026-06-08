package bl00dy_c0d3_.echovr_installer;

import java.io.*;
import java.util.zip.*;

public class UnzipFile {

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        System.out.println("extract " + zipFilePath + " -> " + destDirectory);

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String filePath = destDirectory + File.separator + entry.getName();
                if (entry.isDirectory()) {
                    new File(filePath).mkdirs();
                } else {
                    File parent = new File(filePath).getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    extractFile(zipFile, entry, filePath);
                }
            }
        }
        System.out.println("done");
    }

    private static void extractFile(ZipFile zipFile, ZipEntry entry, String filePath) throws IOException {
        try (InputStream zipIn = zipFile.getInputStream(entry);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[65536];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
}
