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

            // Game data lives in the app-owned external media dir. Unlike /sdcard/readyatdawn,
            // this needs no storage permission, so it works on secondary Quest accounts.
            // It MUST be staged AFTER the APK is installed, because Android wipes
            // /sdcard/Android/media/<pkg> when the app is uninstalled.
            String dataDir = "/sdcard/Android/media/com.readyatdawn.r15/files";

            System.out.println("**adb kill-server");
            Adb.run("kill-server");

            System.out.println("**InstallerQuest ADB DEVICES (1st step)");
            Adb.run("devices");

            System.out.println("**Uninstall");
            Adb.run("uninstall com.readyatdawn.r15");

            System.out.println("**delete legacy data /sdcard/readyatdawn (old install location)");
            Adb.run("shell \"rm -rf /sdcard/readyatdawn\"");

            System.out.println("**Install");
            Adb.run("install -g \"" + pathToApkObb + "/" + apkfileName + "\"");

            System.out.println("**mkdir: " + dataDir + "/_local");
            Adb.run("shell \"mkdir -p " + dataDir + "/_local\"");

            System.out.println("**Set permissions (pre-push)");
            Adb.run("shell \"chmod -R 777 " + dataDir + "\"");

            System.out.println("**push zip to /data/local/tmp");
            progressLabel.setText("Pushing data files...");
            String pushOutput = Adb.run("push \"" + pathToApkObb + "/" + obbfileName + "\" /data/local/tmp");

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

                Adb.run("kill-server");
                Adb.run("start-server");
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
            if (!executeWithReconnect(
                    "shell \"mv /data/local/tmp/_data.zip " + dataDir + "/\"",
                    "mv", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**unzip");
            if (!executeWithReconnect(
                    "shell \"cd " + dataDir + "/; unzip _data.zip\"",
                    "unzip", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**rm zip");
            if (!executeWithReconnect(
                    "shell \"cd " + dataDir + "/; rm _data.zip\"",
                    "rm", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**Set permissions (post-unzip)");
            if (!executeWithReconnect(
                    "shell \"chmod -R 777 " + dataDir + "\"",
                    "chmod", progressLabel)) {
                overallSuccess = false;
            }

            System.out.println("**Grant permissions");
            Adb.run("shell appops set com.readyatdawn.r15 MANAGE_EXTERNAL_STORAGE allow");
            Adb.run("shell pm grant com.readyatdawn.r15 android.permission.READ_EXTERNAL_STORAGE");
            Adb.run("shell pm grant com.readyatdawn.r15 android.permission.WRITE_EXTERNAL_STORAGE");
            Adb.run("shell pm grant com.readyatdawn.r15 android.permission.RECORD_AUDIO");

            //System.out.println("**adb reboot");
            //Adb.run("reboot");

            System.out.println("**adb kill-server");
            Adb.run("kill-server");

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
        else if (deviceConnected == 2) {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(parrentFrame, "Several devices connected",
                "<html><center>" + multiDeviceMessage() + "</center></html>", 0);
            System.out.println("**Several usable devices are attached -- refusing to guess: "
                    + Adb.lastSelection().reason());
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
     * @return 0 = connected &amp; authorized, 1 = connected but unauthorized,
     *         2 = several usable devices and none selectable, -1 = not detected.
     */
    public static int checkConnection() {
        prepareAdb();
        // The user may have re-plugged a different headset since the last check, so never
        // answer this from a cached serial.
        Adb.clearTargetSerial("connection check");
        return checkQuestStatus();
    }

    /**
     * The message shown when adb reports several usable devices. Lists what it saw, so the
     * user can tell which cable to pull.
     */
    static String multiDeviceMessage() {
        StringBuilder sb = new StringBuilder(
                "More than one device is plugged in, so the installer can't tell<br>"
                + "which one is your Quest:<br><br>");
        for (AdbDevices.Device device : Adb.lastSelection().pickable()) {
            sb.append("&nbsp;&nbsp;\u2022&nbsp;").append(device.label()).append("<br>");
        }
        sb.append("<br>Unplug the others (phone, second headset, emulator) and try again.");
        return sb.toString();
    }

    /**
     * Reads the device table and decides which headset to talk to.
     *
     * <p>This used to scan {@code adb devices} itself and return on the <em>first</em> line
     * ending in {@code device}, so a second entry -- an emulator, a phone, an offline stub,
     * a second headset -- was never noticed: the installer reported "Quest connected" and
     * every later command then failed with "more than one device/emulator".
     * {@link AdbDevices} now enumerates the whole table, logs it, and picks a serial to pin.
     *
     * @return 0 = ready, 1 = unauthorized, 2 = several usable devices, -1 = none.
     */
    static int checkQuestStatus(){
        return AdbDevices.probe().statusCode();
    }


    static int parseExitCode(String output) {
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

    /**
     * @param argsTail the adb subcommand and its arguments, without the binary -- e.g.
     *                 {@code shell "cd /x; unzip a.zip"}. {@link Adb} adds the binary, the
     *                 {@code -s <serial>} pin and the trace.
     */
    static boolean executeWithReconnect(String argsTail,
                                                 String commandDesc, SpecialLabel progressLabel) {
        // Adb.runExit, not runShellCommandWithExitCode: the latter drains both streams into
        // nothing, so a failure here -- the likeliest place for "more than one device" to
        // surface -- used to leave only a bare exit code in the log.
        return executeWithReconnect(() -> Adb.runExit(argsTail), commandDesc, progressLabel);
    }

    /**
     * Runs {@code action} and, if it fails <em>because the device dropped</em>, restarts the
     * adb server and retries once. A failure with the device still connected is not retried.
     *
     * <p>The supplier form lets argv-based callers (see {@link Adb#exec}) reuse the same
     * reconnect handling as the command-string ones.
     */
    static boolean executeWithReconnect(java.util.function.IntSupplier action,
                                                 String commandDesc, SpecialLabel progressLabel) {
        int exitCode = action.getAsInt();
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

        // Unflagged by construction (Adb exempts the server subcommands), and each one drops
        // the cached serial, so the retry below resolves against the device table that exists
        // *after* the reconnect rather than reusing a stale one.
        Adb.run("kill-server");
        Adb.run("start-server");
        pause(2);

        deviceStatus = checkQuestStatus();
        if (deviceStatus != 0) {
            System.out.println("**ERROR: Device still disconnected after reconnection during " + commandDesc);
            return false;
        }

        progressLabel.setText("Device reconnected, continuing...");

        exitCode = action.getAsInt();
        if (exitCode != 0) {
            System.out.println("**ERROR: " + commandDesc + " failed after reconnection (exit code " + exitCode + ")");
            return false;
        }

        return true;
    }
}
