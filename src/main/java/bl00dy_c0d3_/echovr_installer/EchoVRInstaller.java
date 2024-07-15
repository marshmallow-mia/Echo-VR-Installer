package bl00dy_c0d3_.echovr_installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class EchoVRInstaller {
    // Programmstart:
    public static void main(String[] args) {
        try {
            // Create log file in the current working directory
            File logFile = new File("log.log");
            logFile.delete();
            logFile.createNewFile();

            // Redirect stdout and stderr to log file
            PrintStream logStream = new PrintStream(new FileOutputStream(logFile, true));
            System.setOut(logStream);
            System.setErr(logStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start your main application logic
        new FrameMain();
    }
}
