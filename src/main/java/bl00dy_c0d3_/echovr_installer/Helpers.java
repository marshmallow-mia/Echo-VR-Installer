package bl00dy_c0d3_.echovr_installer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;



public class Helpers {
    // Create a Windows shortcut (.lnk) using PowerShell
    public static void createWindowsShortcut(String exePath, String shortcutPath, String name) throws IOException, InterruptedException {
        String psScript =
            "$WshShell = New-Object -ComObject WScript.Shell\n" +
            "$Shortcut = $WshShell.CreateShortcut(\"" + shortcutPath.replace("\\", "\\\\") + "\")\n" +
            "$Shortcut.TargetPath = \"" + exePath.replace("\\", "\\\\") + "\"\n" +
            "$Shortcut.Save()\n";
        File tempPs1 = File.createTempFile("createshortcut", ".ps1");
        try (FileWriter fw = new FileWriter(tempPs1)) {
            fw.write(psScript);
        }
        System.out.println("[Shortcut Powershell] Temp script: " + tempPs1.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempPs1.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
        }
        int exitCode = p.waitFor();
        System.out.println("[Shortcut Powershell] Exit code: " + exitCode);
        System.out.println("[Shortcut Powershell] Output:\n" + output);
        tempPs1.delete();
    }


    // Add shortcut to Start Menu
    public static void addShortcutsAll(String exePath, String shortcutName) throws Exception {
        String appData = System.getenv("APPDATA");
        String userProfile = System.getenv("USERPROFILE");
        String startMenu = appData + "\\Microsoft\\Windows\\Start Menu\\Programs";
        String desktop = userProfile + "\\Desktop";
        String shortcutPathStart = startMenu + "\\" + shortcutName + ".lnk";
        String shortcutPathDesktop = desktop + "\\" + shortcutName + ".lnk";
        System.out.println("[Shortcut] exePath: " + exePath);
        System.out.println("[Shortcut] Start Menu: " + shortcutPathStart);
        System.out.println("[Shortcut] Desktop: " + shortcutPathDesktop);
        createWindowsShortcut(exePath, shortcutPathStart, shortcutName);
        createWindowsShortcut(exePath, shortcutPathDesktop, shortcutName);
    }
    static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static boolean linux = System.getProperty("os.name").toLowerCase().contains("linux");
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));


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
            return;
            //new ErrorDialog().errorDialog(outFrame, "Wrong filetype provided", "Your provided file is not a config.json. Please check again!", 0);
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


    public static boolean checkIfChromeOs(){

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


    public static String runShellCommand(String shellCommand) {
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        BufferedReader stdInput = null;
        BufferedReader stdError = null;

        if (linux || mac){
            try {
                ProcessBuilder builder = new ProcessBuilder("bash", "-c", shellCommand);
                Process process = builder.start();

                stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));


                int exitCode = process.waitFor();
                output.append("Process exited with code ").append(exitCode).append("\n");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                System.out.println("HelpersClass runShellCommand: " + shellCommand);
                Process process = Runtime.getRuntime().exec(shellCommand);
                stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        try {
        // Read the output from the command
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            output.append(s).append("\n");
        }

        // Read any errors from the attempted command
        String e = null;
        while ((e = stdError.readLine()) != null) {
            errorOutput.append(e).append("\n");
        }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Combine standard output and error output (optional)
        if (!errorOutput.isEmpty()) {
            output.append("ERROR OUTPUT:\n").append(errorOutput.toString());
        }
        System.out.println(output);

        return output.toString();
    }


    //CAN SOMEONE PLEASE TELL ME WHY I HAVE TWO FUNCTIONS FOR PRETTY MUCH THE SAME SHIT?????

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

    public static String checkForEchoOnKnownPaths(JDialog outFrame){
        String command = "powershell.exe -Command \"(New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)\"";
        String output = runShellCommandWithOutput(command);

        boolean checkAdmin = checkForAdmin();

        // Parse and display the result
        if (! checkAdmin) {
            System.out.println("The application is NOT running with administrative privileges.");
            ErrorDialog runAsAdmin = new ErrorDialog();
            runAsAdmin.errorDialog(outFrame, "Please restart as Admin", "<html>To auto choose the echovr.exe path, you need to restart this app as admin. To do that,<br>close the Installer completely. Then right click on EchoVR_Installer.exe<br>and click on Start as Admin.</html>", -1);
            return "";
        }
        else{
            String commandGetOculusPath = "powershell.exe -Command \"(Get-ItemProperty -Path 'HKLM:\\SOFTWARE\\WOW6432Node\\Oculus VR, LLC\\Oculus').Base\"";
            String oculusPath = runShellCommandWithOutput(commandGetOculusPath);
            String[] knownPaths = {"C:/EchoVR", oculusPath};

            for (String path : knownPaths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File foundFile = findFileRecursive(dir, "echovr.exe");
                    if (foundFile != null) {

                        return foundFile.getAbsolutePath().replace("echovr.exe", ""); // Return first match
                    }
                }
            }


        }

        return "";

    }

    private static File findFileRecursive(File dir, String filename) {
        File[] files = dir.listFiles();
        if (files == null) return null; // Check for access issues

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileRecursive(file, filename); // Recursively search subdirs
                if (found != null) return found;
            } else if (file.getName().equalsIgnoreCase(filename)) {
                return file;
            }
        }
        return null;
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


    //Method to download a textfile and return each line as array object
    public static String[] getFileAndReturnArray(String fileUrl, String outputFile){
        //String fileUrl = this.fileUrl; // Replace with actual URL
        String destination = tempPath + "/" + outputFile; // File where data will be saved
        List<String> lines = null;
        try {
            // Download the file
            downloadFile(fileUrl, destination);

            // Read file and store lines in an array
            lines = readFileToArray(destination);

            // Print stored objects
            for (String line : lines) {
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines.toArray(new String[0]); // Correct way to return an array
    }

    // Method to download file from URL
    public static void downloadFile(String fileUrl, String destination) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }

    // Method to read file and store each line in an array
    public static List<String> readFileToArray(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
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
                if (linux) {
                    System.out.println("**Linux prepareAdb: chmod +x " + tempPath + "/" + folder + "/adb");
                    runShellCommand("chmod +x " + tempPath + "/" + folder + "/adb");
                }

            } catch (Exception e) {
                e.printStackTrace();

            }
            //TODO ^
        }

    }






}
