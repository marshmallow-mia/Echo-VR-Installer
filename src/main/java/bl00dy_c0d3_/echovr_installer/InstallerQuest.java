package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;


import static bl00dy_c0d3_.echovr_installer.Helpers.*;

//This Class will uninstall echo, install echo and copy obb
//NOT AN OBB ANYMORE, BUT _data, thanks to stupid meta stuff


public class InstallerQuest {
    static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static boolean isChrome = checkIfChromeOs();
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));

    public boolean installAPK(String pathToApkObb, String apkfileName, String obbfileName, SpecialLabel progressLabel, JDialog parrentFrame)  {
        System.out.println(System.getProperty("os.name").toLowerCase());

        prepareAdb();



        // Check if any device is connected
        int deviceConnected = checkQuestStatus();
        if (deviceConnected == 0) {

            File apkFile = new File(pathToApkObb + "/" + apkfileName);
            File obbFile = new File(pathToApkObb + "/" + obbfileName);
            if(!apkFile.exists() ||  !obbFile.exists()) {
                System.out.println("APK OR _data FILE NOT FOUND");
                ErrorDialog error = new ErrorDialog();
                error.errorDialog(parrentFrame, "File not found", "APK or DATA FILE NOT FOUND. PLEASE DOWNLOAD IT ABOVE!", 0);
                return false;
            }



            String commandResult1;
            String commandResult2;
            String commandResult3;
            String commandResult4;
            String commandResult5;
            String commandResult6;
            if (isWindows) {
                String adbPath = "\"" + tempPath + "/platform-tools/adb.exe" + "\"";

                System.out.println("**InstallerQuest ADB DEVICES (Windows 1st step)");
                runShellCommand(adbPath + " devices");

                System.out.println("**Uninstall");
                runShellCommand(adbPath + " uninstall com.readyatdawn.r15");

                System.out.println("**Install");
                runShellCommand(adbPath + " install -g \"" + pathToApkObb + "/" + apkfileName);

                System.out.println("**mkdir: /sdcard/Android/data/com.readyatdawn.r15/files");
                runShellCommand(adbPath + " shell \"mkdir -p /sdcard/Android/data/com.readyatdawn.r15/files\"");

                System.out.println("**push zip");
                runShellCommand(adbPath + " push \"" + pathToApkObb + "/" + obbfileName + "\" \"/sdcard/Android/data/com.readyatdawn.r15/files/\"");

                System.out.println("**unzip");
                runShellCommand(adbPath + " shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; unzip _data.zip\"");

                System.out.println("**rm zip");
                runShellCommand(adbPath + " shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; rm _data.zip\"");

                System.out.println("**Set permissions on all files (post-install)");
                runShellCommand(adbPath + " shell \"chmod -R 777 /sdcard/Android/data/com.readyatdawn.r15/files\"");
            }
            else if (isChrome) {
                System.out.println("**InstallerQuest ADB DEVICES (Chrome 1st step)");
                runShellCommand("adb devices");

                System.out.println("**Uninstall");
                runShellCommand("adb uninstall com.readyatdawn.r15");

                System.out.println("**Install");
                runShellCommand("adb install -g " + pathToApkObb + "/" + apkfileName);

                System.out.println("**mkdir: /sdcard/Android/data/com.readyatdawn.r15/files");
                runShellCommand("adb shell \"mkdir -p /sdcard/Android/data/com.readyatdawn.r15/files\"");

                System.out.println("**push zip");
                runShellCommand("adb push " + pathToApkObb + "/" + obbfileName + " \"/sdcard/Android/data/com.readyatdawn.r15/files/\"");

                System.out.println("**unzip");
                runShellCommand("adb shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; unzip _data.zip\"");

                System.out.println("**rm zip");
                runShellCommand("adb shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; rm _data.zip\"");

                System.out.println("**Set permissions on all files (post-install)");
                runShellCommand("adb shell \"chmod -R 777 /sdcard/Android/data/com.readyatdawn.r15/files\"");
            }
            else if (mac) {
                System.out.println("**InstallerQuest ADB DEVICES (Mac 1st step)");
                runShellCommand(tempPath + "/platform-tools-mac/adb devices");

                System.out.println("**Uninstall");
                runShellCommand(tempPath + "/platform-tools-mac/adb uninstall com.readyatdawn.r15");

                System.out.println("**Install");
                runShellCommand(tempPath + "/platform-tools-mac/adb install -g " + pathToApkObb + "/" + apkfileName);

                System.out.println("**mkdir: /sdcard/Android/data/com.readyatdawn.r15/files");
                runShellCommand(tempPath + "/platform-tools-mac/adb shell \"mkdir -p /sdcard/Android/data/com.readyatdawn.r15/files\"");

                System.out.println("**push zip");
                runShellCommand(tempPath + "/platform-tools-mac/adb push " + pathToApkObb + "/" + obbfileName + " /sdcard/Android/data/com.readyatdawn.r15/files/");

                System.out.println("**unzip");
                runShellCommand(tempPath + "/platform-tools-mac/adb shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; unzip _data.zip\"");

                System.out.println("**rm zip");
                runShellCommand(tempPath + "/platform-tools-mac/adb shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; rm _data.zip\"");

                System.out.println("**Set permissions on all files (post-install)");
                runShellCommand(tempPath + "/platform-tools-mac/adb shell \"chmod -R 777 /sdcard/Android/data/com.readyatdawn.r15/files\"");
            }
            else {
                System.out.println("**InstallerQuest ADB DEVICES (Linux 1st step)");
                runShellCommand(tempPath + "/platform-tools-linux/adb devices");

                System.out.println("**Uninstall");
                runShellCommand(tempPath + "/platform-tools-linux/adb uninstall com.readyatdawn.r15");

                System.out.println("**Install");
                runShellCommand(tempPath + "/platform-tools-linux/adb install -g " + pathToApkObb + "/" + apkfileName);

                System.out.println("**mkdir: /sdcard/Android/data/com.readyatdawn.r15/files");
                runShellCommand(tempPath + "/platform-tools-linux/adb shell \"mkdir -p /sdcard/Android/data/com.readyatdawn.r15/files\"");

                System.out.println("**push zip");
                runShellCommand(tempPath + "/platform-tools-linux/adb push " + pathToApkObb + "/" + obbfileName + " \"/sdcard/Android/data/com.readyatdawn.r15/files/\"");

                System.out.println("**unzip");
                runShellCommand(tempPath + "/platform-tools-linux/adb shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; unzip _data.zip\"");

                System.out.println("**rm zip");
                runShellCommand(tempPath + "/platform-tools-linux/adb shell \"cd /sdcard/Android/data/com.readyatdawn.r15/files; rm _data.zip\"");

                System.out.println("**Set permissions on all files (post-install)");
                runShellCommand(tempPath + "/platform-tools-linux/adb shell \"chmod -R 777 /sdcard/Android/data/com.readyatdawn.r15/files\"");
            }



            return true;
        }
        else if (deviceConnected == 1) {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(parrentFrame, "Not authorized", "<html>You need to allow this PC to use adb on your Quest. <br>Replug the cable and check inside your Quest. You should get asked if you allow the connection.</html>", 0);
            System.out.println("Device is unauthorized!");
            return false;
        }
        else if (deviceConnected == -1) {
            // Create an instance of ErrorDialog
            ErrorDialog errorDialog = new ErrorDialog();

            // Show the error dialog
            errorDialog.errorDialog(parrentFrame, "No Device detected", "<html>Either your Quest is not connected, or you don't have the Developer Mode enabled</html>", 1);

            System.out.println("No device is connected.");
            return false;
        }
        return false;
    }





    //0 = connected, 1 = unauthorized, -1 not connected
    // Method to check if any device is connected based on the adb devices output
    private static int checkQuestStatus(){

        // Start the process
        Process process = null;
        try {
            if(isWindows) {
                process = new ProcessBuilder(tempPath + "/platform-tools/adb.exe", "devices").start();
            }
            else if(mac) {
                process = new ProcessBuilder(tempPath + "/platform-tools-mac/adb", "devices").start();
            }
            else if(isChrome){
                process = new ProcessBuilder("adb", "devices").start();
            }
            else{
                System.out.println("LINUX checkQuestStatus: " + tempPath + "/platform-tools-linux/adb " + "devices");
                process = new ProcessBuilder(tempPath + "/platform-tools-linux/adb", "devices").start();
//                process = new ProcessBuilder("/lib64/ld-linux-x86-64.so.2", tempPath + "/platform-tools-linux/adb", "devices").start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO ^

        // StringBuilder to accumulate the output
        StringBuilder stdOutResult = new StringBuilder();

        // Read the output from the process's input stream
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String stdout;
            while ((stdout = stdInput.readLine()) != null) {
                stdOutResult.append(stdout).append("\n"); // Append each line and a newline character
            }

        }
        catch (IOException e){}
        //TODO ^

        // Print the result
        //System.out.println(stdOutResult + "");

        // Check each line for device connection status
        for (String line : (stdOutResult + "").split("\\r?\\n")) {

            System.out.println(line);
            // Check if the line ends with "device" indicating a connected device
            if (line.endsWith("device")) {
                return 0;
            }
            // Check if the line ends with "unauthorized" indicating a connected device
            if (line.endsWith("unauthorized") || line.endsWith("[http://developer.android.com/tools/device.html]")) {
                return 1;
            }
        }

        // If no device lines containing "device or unauthorized" were found
        return -1;
    }





}
