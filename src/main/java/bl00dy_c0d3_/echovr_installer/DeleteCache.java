package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.File;
import java.nio.file.Paths;

public class DeleteCache{
    // Array of file and folder paths to be deleted
    private static final String[] pathsToDelete = {
            "C:/EchoVR/ready-at-dawn-echo-arena.zip",
            "C:/Program Files/Oculus/Software/Software/ready-at-dawn-echo-arena.zip",
            Paths.get(System.getProperty("java.io.tmpdir"), "/echo/") + "",
            Paths.get(System.getProperty("java.io.tmpdir"), "/platform-tools/") + "",
            Paths.get(System.getProperty("java.io.tmpdir"), "/platform-tools-linux/") + "",
        Paths.get(System.getProperty("java.io.tmpdir"), "/platform-tools-mac/") + "",
    };

    public void executeDeletion(JFrame outFrame) {
        // Loop through each path and attempt to delete the file/folder
        for (String path : pathsToDelete) {
            File fileOrDirectory = new File(path);
            if (deleteFileOrDirectory(fileOrDirectory)) {
                System.out.println("Deleted: " + fileOrDirectory.getName());
            } else {
                System.out.println("Failed to delete: " + fileOrDirectory.getName());
            }
        }
        JOptionPane.showMessageDialog(outFrame, "<html>The cached files have been deleted. If you have choosen a different<br>path as the default, you need to delete them manually.</html>", "Deleting done", JOptionPane.INFORMATION_MESSAGE);

    }

    private boolean deleteFileOrDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            // If it's a directory, delete its contents recursively
            File[] allContents = fileOrDirectory.listFiles();
            if (allContents != null) {
                for (File fileContent : allContents) {
                    deleteFileOrDirectory(fileContent);
                }
            }
        }
        // Delete the file or the now-empty directory
        return fileOrDirectory.delete();
    }
}
