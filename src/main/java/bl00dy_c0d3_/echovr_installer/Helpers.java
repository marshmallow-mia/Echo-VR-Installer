package bl00dy_c0d3_.echovr_installer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

public class Helpers {
    static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");


    @Contract("_, _ -> new")
    public static @NotNull SpecialLabel createSpecialLabel(String text, int fontSize) {
        return new SpecialLabel(text, fontSize);
    }

    public static @NotNull SpecialLabel createSpecialLabel(@NotNull String text, int fontSize, int x, int y) {
        SpecialLabel label = createSpecialLabel(text, fontSize);
        label.setLocation(x, y);
        return label;
    }

    public static @NotNull SpecialLabel createSpecialLabel(String text, int fontSize, int x, int y, Dimension size, Color foreground, @NotNull Color background) {
        SpecialLabel label = createSpecialLabel(text, fontSize, x, y);
        label.setSize(size);
        label.setForeground(foreground);
        label.setBackground(new Color(background.getRed(), background.getGreen(), background.getBlue(), 200));
        return label;
    }

    public static @Nullable Image loadGUI(String imageName) {
        URL imageURL = Helpers.class.getClassLoader().getResource(imageName);
        return imageURL == null ? null : new ImageIcon(imageURL, imageName).getImage();
    }

    public static void centerFrame(@NotNull Window frame, int width, int height) {
        frame.setSize(width, height);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (d.width - frame.getWidth()) / 2;
        int y = (d.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
    }

        public static void jsonFileChooser(SpecialLabel labelPcDownloadPath, JDialog outFrame) {
        FileDialog fd = new FileDialog((Frame) null, "Select a JSON file", FileDialog.LOAD);
        fd.setFile("*.json");
        fd.setVisible(true);

        String directory = fd.getDirectory();
        String filename = fd.getFile();

        if (filename != null && filename.endsWith(".json")) {
            String configPath = new File(directory, filename).getPath();
            labelPcDownloadPath.setText(configPath);
            outFrame.repaint();
        }
        else{
            new ErrorDialog().errorDialog(outFrame, "Wrong filetype provided", "Your provided file is not a config.json. Please check again!", 0);
        }
    }


    public static void pathFolderChooser(SpecialLabel labelPcDownloadPath, JDialog outFrame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            String directory = chooser.getSelectedFile().getPath();
            labelPcDownloadPath.setText(directory);
            outFrame.repaint();
        }
    }


    public static void pause(int timeInSecond){
        try {
            TimeUnit.SECONDS.sleep(timeInSecond);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    public static boolean chechIfChromeOs(){

        // Create a File object
        File file = new File("/opt/google/cros-containers/etc/lsb-release");

        // Check if the file exists
        if (file.exists()) {
            System.out.println("OS is chromeOS.");
            return true;

        } else {
            System.out.println("OS is NOT chromeOS.");
            return false;
        }
    }


    public static void runShellCommand(String shellCommand){
        try {
                Process process = Runtime.getRuntime().exec(shellCommand);
                process.waitFor();
                System.out.println("DONE");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    public static String runShellCommandWithOutput(String command){
        Process process;
        StringBuilder output = null;

        try {

            process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            output = new StringBuilder();

            // Read the output from the command
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            reader.close();


        }
        catch(Exception e){
            e.printStackTrace();
        }
        return output + "";
    }


    public static String checkForAdminAndOculusPath(JDialog outFrame){
        String command = "powershell.exe -Command \"(New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)\"";
        String output = runShellCommandWithOutput(command);

        boolean checkAdmin = checkForAdmin();

        // Parse and display the result
        if (! checkAdmin) {
            System.out.println("The application is NOT running with administrative privileges.");
            ErrorDialog runAsAdmin = new ErrorDialog();
            runAsAdmin.errorDialog(outFrame, "Please restart as Admin", "<html>To use the Oculus original path, you need to restart this app as admin. To do that,<br>close the Installer completely. Then right click on EchoVR_Installer.exe<br>and click on Start as Admin.</html>", -1);
            return "";
        }
        else{
            String commandGetOculusPath = "powershell.exe -Command \"(Get-ItemProperty -Path 'HKLM:\\SOFTWARE\\WOW6432Node\\Oculus VR, LLC\\Oculus').Base\"";
            String oculusPath = runShellCommandWithOutput(commandGetOculusPath);
            return oculusPath;

        }

    }


    public static boolean checkForAdmin(){
        String command = "powershell.exe -Command \"(New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)\"";
        String output = runShellCommandWithOutput(command);

        // Parse and display the result
        if (output.trim().equalsIgnoreCase("False")) {
            return false;
        }
        else{
            return true;
        }
    }



    public static void prepareAdb(){
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
        String[] fileList;
        String[] fileList3;
        Path targetPath;
        Path targetPath2;
        Path targetPath3;
        String libcName;
        String folder;


        if (isWindows) {
            String dir = System.getProperty("java.io.tmpdir") + "platform-tools/";
            File file = new File(dir);
            if (!file.exists()){
                file.mkdirs();
            }

            fileList = new String[]{"adb.exe", "AdbWinApi.dll", "AdbWinUsbApi.dll", "etc1tool.exe", "fastboot.exe", "hprof-conv.exe", "libwinpthread-1.dll", "make_f2fs.exe", "make_f2fs_casefold.exe", "mke2fs.conf", "mke2fs.exe", "NOTICE.txt", "source.properties", "sqlite3.exe"};
            //TODO read filelist from the folder instead of that
            for (int a = 0; a < fileList.length; a++) {
                targetPath = Paths.get(tempPath + "/platform-tools/" + fileList[a]);
                try {
                    InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream("platform-tools/" + fileList[a]);
                    Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {

                }
                //TODO ^
            }
        }
        else{
            if (mac){
                folder = "platform-tools-mac";
                libcName = "libc++.dylib";
                targetPath2 = Paths.get(tempPath + "/" + folder + "/lib64/" + libcName);
            }
            else{
                folder = "platform-tools-linux";
                libcName = "libc++.so";
                targetPath2 = Paths.get(tempPath + "/" + folder + "/lib64/" + libcName);
            }
            String dir = tempPath + "/" + folder + "/";
            File file = new File(dir);
            if (!file.exists()){
                file.mkdirs();
            }
            String dir2 = tempPath + "/" + folder + "/lib64/";
            File file2 = new File(dir2);
            if (!file2.exists()){
                file2.mkdirs();
            }
            try {
                InputStream stream2 = ClassLoader.getSystemClassLoader().getResourceAsStream(folder + "/lib64/" + libcName);
                Files.copy(stream2, targetPath2, StandardCopyOption.REPLACE_EXISTING);


                fileList3 = new String[]{"adb", "fastboot", "make_f2fs_casefold", "mke2fs.conf", "source.properties", "etc1tool", "hprof-conv", "make_f2fs", "mke2fs", "NOTICE.txt", "sqlite3"};
                //TODO read filelist from the folder instead of that
                for (int b = 0; b < fileList3.length; b++) {
                    targetPath3 = Paths.get(tempPath + "/" + folder + "/" + fileList3[b]);
                    InputStream stream3 = ClassLoader.getSystemClassLoader().getResourceAsStream(folder + "/" + fileList3[b]);
                    Files.copy(stream3, targetPath3, StandardCopyOption.REPLACE_EXISTING);
                    stream2.close();
                }
                if (mac) {
                    runShellCommand("chmod -R +x " + tempPath + "/" + folder + "/");
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
            //TODO ^
        }

    }



}
