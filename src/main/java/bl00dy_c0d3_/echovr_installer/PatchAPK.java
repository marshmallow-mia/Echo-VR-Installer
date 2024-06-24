package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class PatchAPK {
    static Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
    Path targetPath;



    public int patchAPK(String pathToApkObb, String apkfileName, String configPath, SpecialLabel progressLabel, JDialog parrentFrame){

        String dir = tempPath + "/uber/";

        if (configPath.startsWith("Optional")){
            ErrorDialog errorDialog = new ErrorDialog();
            errorDialog.errorDialog(parrentFrame, "Path to config.json incorrect", "The path to the config.json is incorrect. Set above", 0);
            return -1;
        }

        File file = new File(dir);
        if (!file.exists()){
            file.mkdirs();
        }
        //TODO read filelist from the folder instead of that
        targetPath = Paths.get(tempPath + "/uber/uber-apk-signer-1.3.0.jar" );
        System.out.println(targetPath + "");

        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("uber-apk-signer-1.3.0.jar");
            Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //TODO ^

        //COPY THE APK BEFORE CHANGING IT
        Path sourcePath = Paths.get(pathToApkObb + "/" + apkfileName);
        Path destinationPath = Paths.get(pathToApkObb + "/changedConfig.apk");
        try {
            // Copy the file from source to destination
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully.");
        } catch (IOException e) {
            // Handle the exception
            System.err.println("Failed to copy file: " + e.getMessage());
        }


        // CHANGE THE CONFIG.JSON
        Map<String, String> zipProperties = new HashMap<>();
        zipProperties.put("create", "false");
        zipProperties.put("encoding", "UTF-8");
        URI zipDisk = URI.create("jar:file:/" + pathToApkObb.replace("\\", "/") + "/changedConfig.apk");
        System.out.println(zipDisk);

        // Create ZIP file system
        try (FileSystem zipFs = FileSystems.newFileSystem(zipDisk, zipProperties)) {
            Path zipFilePath = zipFs.getPath("assets/_local/config.json");
            Path addNewFile = Paths.get(configPath);
            Files.createDirectories(zipFilePath.getParent());
            Files.copy(addNewFile, zipFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



        //SIGN THE APK
        patchAPK(pathToApkObb);
        return 0;

    }

    //Patch the APK
    private void patchAPK(String pathToApkObb){
        Process process = null;
        try {
            System.out.println("java" + "-jar" +  tempPath + "/uber/uber-apk-signer-1.3.0.jar" + "--apks" + pathToApkObb + "/changedConfig.apk");
            process = new ProcessBuilder("java", "-jar", tempPath + "/uber/uber-apk-signer-1.3.0.jar", "--apks", pathToApkObb + "/changedConfig.apk").start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // StringBuilder to accumulate the output
        StringBuilder stdErrResult = new StringBuilder();

        // Read the output from the process's input stream
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String stdout;
            while ((stdout = stdInput.readLine()) != null) {
                stdErrResult.append(stdout).append("\n"); // Append each line and a newline character
            }
            System.out.println(stdErrResult + "");

        }
        catch (IOException e){
            e.printStackTrace();
        }
        //TODO ^
    }

}
