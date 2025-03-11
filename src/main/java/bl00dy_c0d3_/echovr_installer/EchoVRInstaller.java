package bl00dy_c0d3_.echovr_installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import static java.lang.System.*;

public class EchoVRInstaller {

    // Programmstart:
    public static void main(String[] args) {
        try {
            File logFile;
            String osName = getProperty("os.name").toLowerCase();
            if (osName.contains("mac")) {
                // Use the Documents directory in the user's home folder
                String userHome = getProperty("user.home");
                File documentsDir = new File(userHome, "Documents");
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs();
                }

                // Create the log file in the Documents directory
                logFile = new File(documentsDir, "EchoVR_log.log");
            } else {
                // Create log file in the current working directory for other OS
                logFile = new File("log.log");
            }

            //if (logFile.exists()) {
            //    logFile.delete();
            //}
            if (!logFile.exists()) {
                logFile.createNewFile();
            }


            // Redirect stdout and stderr to log file
            PrintStream logStream = new PrintStream(new FileOutputStream(logFile, true));
            setOut(logStream);
            setErr(logStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start your main application logic
        new FrameMain();
    }
}
