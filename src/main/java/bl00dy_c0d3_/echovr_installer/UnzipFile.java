package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.util.zip.*;

public class UnzipFile {

    public static void unzip(JDialog frame, String zipFilePath, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        UnzipDialog unzipFrame = new UnzipDialog();
        unzipFrame.unzipFrame(frame);

        System.out.println("extract");

        try {

            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // If the entry is a file, extract it
                    extractFile(zipIn, filePath);
                } else {
                    // If the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            unzipFrame.setDoneText();
            unzipFrame.setClosable();
            System.out.println("done");
            zipIn.close();
        } catch (Exception e){
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(frame, "Error while unzipping", "Couldn't finish Download. Please check storage Space.", 0);
            unzipFrame.setClosable();
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
