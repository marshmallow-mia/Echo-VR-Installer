package bl00dy_c0d3_.echovr_installer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static bl00dy_c0d3_.echovr_installer.Helpers.*;

public class GetLogFilesFromQuest {

    public static void getLogFilesFromQuest(){
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
        prepareAdb();
        runShellCommand(tempPath + "/platform-tools/adb.exe " + "pull /sdcard/r14logs r14logs");


    }

}
