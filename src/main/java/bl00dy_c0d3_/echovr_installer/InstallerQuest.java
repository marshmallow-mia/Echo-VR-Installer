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

            String adbPath = "";

            if (isWindows) {
                adbPath = "\"" + tempPath + "/platform-tools/adb.exe" + "\"";
            } else if (isChrome) {
                adbPath = "adb";
            } else if (mac) {
                adbPath = tempPath + "/platform-tools-mac/adb";
            } else { // Linux or other
                adbPath = tempPath + "/platform-tools-linux/adb";
            }

            // Game data lives in the app-owned external media dir. Unlike /sdcard/readyatdawn,
            // this needs no storage permission, so it works on secondary Quest accounts.
            // It MUST be staged AFTER the APK is installed, because Android wipes
            // /sdcard/Android/media/<pkg> when the app is uninstalled.
            String dataDir = "/sdcard/Android/media/com.readyatdawn.r15/files";

            System.out.println("**adb kill-server");
            runShellCommand(adbPath + " kill-server");

            System.out.println("**InstallerQuest ADB DEVICES (1st step)");
            runShellCommand(adbPath + " devices");

            System.out.println("**Uninstall");
            runShellCommand(adbPath + " uninstall com.readyatdawn.r15");

            System.out.println("**delete legacy data /sdcard/readyatdawn (old install location)");
            runShellCommand(adbPath + " shell \"rm -rf /sdcard/readyatdawn\"");

            System.out.println("**Install");
            runShellCommand(adbPath + " install -g \"" + pathToApkObb + "/" + apkfileName + "\"");

            System.out.println("**mkdir: " + dataDir + "/_local");
            runShellCommand(adbPath + " shell \"mkdir -p " + dataDir + "/_local\"");

            System.out.println("**Set permissions (pre-push)");
            runShellCommand(adbPath + " shell \"chmod -R 777 " + dataDir + "\"");

            System.out.println("**push zip to /data/local/tmp");
            progressLabel.setText("Pushing data files...");
            String pushOutput = runShellCommand(adbPath + " push \"" + pathToApkObb + "/" + obbfileName + "\" /data/local/tmp");

            boolean pushTransferredData = pushOutput.contains("bytes") && !pushOutput.contains("0 files pushed");
            int pushExitCode = parseExitCode(pushOutput);

            if (!pushTransferredData) {
                System.out.println("**FAILED: push did not transfer any data (exit code: " + pushExitCode + ")");
                ErrorDialog error = new ErrorDialog();
                error.errorDialog(parrentFrame, "Transfer Failed", "Failed to push data files to the device.", 0);
                return false;
            }

            progressLabel.setText("Verifying transfer...");

            int deviceStatus = checkQuestStatus();
            if (deviceStatus != 0) {
                System.out.println("**WARNING: Device disconnected after data push (push transferred but adb connection lost)");
                progressLabel.setText("Device disconnected - retrying...");

                runShellCommand(adbPath + " kill-server");
                runShellCommand(adbPath + " start-server");
                pause(2);

                deviceStatus = checkQuestStatus();
                if (deviceStatus != 0) {
                    ErrorDialog error = new ErrorDialog();
                    error.errorDialog(parrentFrame, "Device Disconnected",
                            "Device disconnected during data transfer and could not be reconnected.", 0);
                    return false;
                }

                progressLabel.setText("Device reconnected, continuing...");
            }

            boolean overallSuccess = true;

            System.out.println("**mv zip to target");
            if (!executeWithReconnect(adbPath,
                    adbPath + " shell \"mv /data/local/tmp/_data.zip " + dataDir + "/\"",
                    "mv", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**unzip");
            if (!executeWithReconnect(adbPath,
                    adbPath + " shell \"cd " + dataDir + "/; unzip _data.zip\"",
                    "unzip", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**rm zip");
            if (!executeWithReconnect(adbPath,
                    adbPath + " shell \"cd " + dataDir + "/; rm _data.zip\"",
                    "rm", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**Set permissions (post-unzip)");
            if (!executeWithReconnect(adbPath,
                    adbPath + " shell \"chmod -R 777 " + dataDir + "\"",
                    "chmod", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**Grant permissions");
            runShellCommand(adbPath + " shell appops set com.readyatdawn.r15 MANAGE_EXTERNAL_STORAGE allow");
            runShellCommand(adbPath + " shell pm grant com.readyatdawn.r15 android.permission.READ_EXTERNAL_STORAGE");
            runShellCommand(adbPath + " shell pm grant com.readyatdawn.r15 android.permission.WRITE_EXTERNAL_STORAGE");
            runShellCommand(adbPath + " shell pm grant com.readyatdawn.r15 android.permission.RECORD_AUDIO");

            //System.out.println("**adb reboot");
            //runShellCommand(adbPath + " reboot");

            System.out.println("**adb kill-server");
            runShellCommand(adbPath + " kill-server");

            return overallSuccess;
        }
        else if (deviceConnected == 1) {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(parrentFrame, "Allow your PC on the Quest",
                "<html><center>Your Quest is connected, but it hasn't allowed this PC yet.<br>"
                + "Put on your headset and tap&nbsp;<b>Allow</b>&nbsp;when the USB debugging prompt appears "
                + "(replug the cable if you don't see it).</center></html>", 3);
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





    /**
     * Prepares ADB and returns the Quest connection status.
     * @return 0 = connected &amp; authorized, 1 = connected but unauthorized, -1 = not detected.
     */
    public static int checkConnection() {
        prepareAdb();
        return checkQuestStatus();
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


    private static int parseExitCode(String output) {
        if (output == null) {
            return -1;
        }
        String[] lines = output.split("\\n");
        for (String line : lines) {
            if (line.startsWith("Process exited with code ")) {
                try {
                    String codeStr = line.substring("Process exited with code ".length()).trim();
                    return Integer.parseInt(codeStr);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return 0;
    }

    private static boolean executeWithReconnect(String adbPath, String command,
                                                 String commandDesc, SpecialLabel progressLabel) {
        int exitCode = runShellCommandWithExitCode(command);
        if (exitCode == 0) {
            return true;
        }

        int deviceStatus = checkQuestStatus();
        if (deviceStatus == 0) {
            System.out.println("**WARNING: " + commandDesc + " failed (exit code " + exitCode +
                    ") but device is connected — not retrying");
            return false;
        }

        System.out.println("**WARNING: Device disconnected during " + commandDesc + ", retrying...");
        progressLabel.setText("Device disconnected - retrying...");

        runShellCommand(adbPath + " kill-server");
        runShellCommand(adbPath + " start-server");
        pause(2);

        deviceStatus = checkQuestStatus();
        if (deviceStatus != 0) {
            System.out.println("**ERROR: Device still disconnected after reconnection during " + commandDesc);
            return false;
        }

        progressLabel.setText("Device reconnected, continuing...");

        exitCode = runShellCommandWithExitCode(command);
        if (exitCode != 0) {
            System.out.println("**ERROR: " + commandDesc + " failed after reconnection (exit code " + exitCode + ")");
            return false;
        }

        return true;
    }
}
