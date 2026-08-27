package bl00dy_c0d3_.echovr_installer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

// TODO: Remove in v0.9.0 — replaced by FrameGuidanceQuest
/**
 * @deprecated Replaced by {@link FrameGuidanceQuest}. Will be removed in a future version.
 */
@Deprecated
public class GetLogFilesFromQuest {
    static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    static boolean mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
    static boolean isChrome = checkIfChromeOs();
    static String commandResult = "";

    public static void getLogFilesFromQuest(){
        // NOTE: the pre-Adb Linux branch prefixed the loader (/lib64/ld-linux-x86-64.so.2);
        // Adb.path() does not, matching what InstallerQuest already does everywhere else.
        prepareAdb();
        commandResult = Adb.run("pull /sdcard/r14logs/ r14logs/");
        commandResult = Adb.run("pull /sdcard/Android/data/com.readyatdawn.r15/files/_local/r14logs/ r14logs/");
        System.out.println(commandResult);

    }





}
