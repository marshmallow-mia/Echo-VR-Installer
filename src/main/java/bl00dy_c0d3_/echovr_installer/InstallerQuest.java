package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

//This Class will uninstall echo, install echo and copy obb
public class InstallerQuest {
    static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    String[] fileList;
    String[] fileList2;
    String[] fileList3;
    Path targetPath;
    Path targetPath2;
    Path targetPath3;
    String folder;
    String libcName;
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));

    public void installAPK(String pathToApkObb, String apkfileName, String obbfileName, SpecialLabel progressLabel, JDialog parrentFrame)  {
        InstallerQuest outFrame = this;
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
                    InputStream stream = getClass().getClassLoader().getResourceAsStream("platform-tools/" + fileList[a]);
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
                InputStream stream2 = getClass().getClassLoader().getResourceAsStream(folder + "/lib64/" + libcName);
                Files.copy(stream2, targetPath2, StandardCopyOption.REPLACE_EXISTING);


                fileList3 = new String[]{"adb", "fastboot", "make_f2fs_casefold", "mke2fs.conf", "source.properties", "etc1tool", "hprof-conv", "make_f2fs", "mke2fs", "NOTICE.txt", "sqlite3"};
                //TODO read filelist from the folder instead of that
                for (int b = 0; b < fileList3.length; b++) {
                    targetPath3 = Paths.get(tempPath + "/" + folder + "/" + fileList3[b]);
                    InputStream stream3 = getClass().getClassLoader().getResourceAsStream(folder + "/" + fileList3[b]);
                    Files.copy(stream3, targetPath3, StandardCopyOption.REPLACE_EXISTING);
                    stream2.close();
                }
                runShellCommand("chmod -R +x " + tempPath + "/" + folder + "/", 1);
            } catch (Exception e) {
                e.printStackTrace();

            }
            //TODO ^
        }


        // Check if any device is connected
        int deviceConnected = checkQuestStatus();
        if (deviceConnected == 0) {

            File apkFile = new File(pathToApkObb + "/" + apkfileName);
            File obbFile = new File(pathToApkObb + "/" + obbfileName);
            if(!apkFile.exists() ||  !obbFile.exists()) {
                System.out.println("APK or OBB FILE NOT FOUND");
                ErrorDialog error = new ErrorDialog();
                error.errorDialog(parrentFrame, "File not found", "APK or OBB FILE NOT FOUND. PLEASE DOWNLOAD IT ABOVE!", 0);

                return;
            }




            if(isWindows) {
                runShellCommand(tempPath + "/platform-tools/adb.exe " + "uninstall com.readyatdawn.r15", 1);
                runShellCommand(tempPath + "/platform-tools/adb.exe " + "install " + pathToApkObb + "/" + apkfileName, 2);
                runShellCommand(tempPath + "/platform-tools/adb.exe " + "shell \"mkdir /storage/self/primary/Android/obb/com.readyatdawn.r15\"", 3);
                runShellCommand(tempPath + "/platform-tools/adb.exe " + "push " + pathToApkObb + "/" + obbfileName + " \"/storage/self/primary/Android/obb/com.readyatdawn.r15/\"", 4);
            }
            else{
                runShellCommand(tempPath + "/" +  folder + "/adb " + "uninstall com.readyatdawn.r15", 1);
                runShellCommand(tempPath + "/" +  folder + "/adb " + "install " + pathToApkObb + "/" + apkfileName, 2);
                runShellCommand(tempPath + "/" +  folder + "/adb " + "shell " + "mkdir /storage/self/primary/Android/obb/com.readyatdawn.r15", 3);
                runShellCommand(tempPath + "/" +  folder + "/adb " + "push " + pathToApkObb + "/" + obbfileName + " /storage/self/primary/Android/obb/com.readyatdawn.r15/", 4);


            }


        }
        else if (deviceConnected == 1) {
            ErrorDialog error = new ErrorDialog();
            error.errorDialog(parrentFrame, "Not authorized", "<html>You need to allow this PC to use adb on your Quest. <br>Replug the cable and check inside your Quest. You should get asked if you allow the connection.</html>", 0);
            System.out.println("Device is unauthorized!");
        }
        else if (deviceConnected == -1) {
            // Create an instance of ErrorDialog
            ErrorDialog errorDialog = new ErrorDialog();

            // Show the error dialog
            errorDialog.errorDialog(parrentFrame, "No Device detected", "<html>Either your Quest is not connected, or you don't have the Developer Mode enabled</html>", 1);

            System.out.println("No device is connected.");
        }

    }


    //0 = connected, 1 = unauthorized, -1 not connected
    // Method to check if any device is connected based on the adb devices output
    private static int checkQuestStatus() {

        // Start the process
        Process process = null;
        try {
            if(isWindows) {
                process = new ProcessBuilder(tempPath + "/platform-tools/adb.exe", "devices").start();
            }
            else if(mac){
                process = new ProcessBuilder(tempPath + "/platform-tools-mac/adb", "devices").start();
            }
            else{
                process = new ProcessBuilder(tempPath + "/platform-tools-linux/adb", "devices").start();
            }

        } catch (IOException e) {
            //e.printStackTrace();
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

    private void runShellCommand(String shellCommand, int step){
        try {
            if (isWindows) {

                Process process = Runtime.getRuntime().exec(shellCommand);
                process.waitFor();
                System.out.println("DONE");
            } else {
                Process process = Runtime.getRuntime().exec(shellCommand);
                process.waitFor();
                System.out.println("DONE");
            }

        }
        catch (Exception e){

        }
    }



}
