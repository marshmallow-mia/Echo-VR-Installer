package bl00dy_c0d3_.echovr_installer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class GetLogFilesFromQuest {
    static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static boolean isChrome = checkIfChromeOs();
    static String commandResult = "";

    public static void getLogFilesFromQuest(){
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
        prepareAdb();
        //runShellCommand(tempPath + "/platform-tools/adb.exe " + "pull /sdcard/r14logs r14logs");

        if(isWindows) {
            commandResult = runShellCommand(tempPath + "/platform-tools/adb.exe " + "pull /sdcard/r14logs/ r14logs/");
            commandResult = runShellCommand(tempPath + "/platform-tools/adb.exe " + "pull /sdcard/Android/data/com.readyatdawn.r15/files/_local/r14logs/ r14logs/");
        }
        else if(isChrome){
            commandResult = runShellCommand("adb " + "pull /sdcard/r14logs/ r14logs/");
            commandResult = runShellCommand("adb " + "pull /sdcard/Android/data/com.readyatdawn.r15/files/_local/r14logs/ r14logs/");
        }
        else if(mac){
            commandResult = runShellCommand(tempPath + "/platform-tools-mac/adb " + "pull /sdcard/r14logs/ r14logs/");
            commandResult = runShellCommand(tempPath + "/platform-tools-mac/adb " + "pull /sdcard/Android/data/com.readyatdawn.r15/files/_local/r14logs/ r14logs/");
                    }
        else {
            commandResult = runShellCommand("/lib64/ld-linux-x86-64.so.2 " + tempPath + "/platform-tools-linux/adb " + "pull /sdcard/r14logs/ r14logs/");
            commandResult = runShellCommand("/lib64/ld-linux-x86-64.so.2 " + tempPath + "/platform-tools-linux/adb " + "pull /sdcard/Android/data/com.readyatdawn.r15/files/_local/r14logs/ r14logs/");
        }
        System.out.println(commandResult);

    }





}
