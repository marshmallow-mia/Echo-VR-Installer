package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manifest-based update engine for the PC install.
 *
 * <p>Processes all {@code del} entries first, then downloads and SHA-256-verifies each
 * {@code add} entry into {@code installBinPath}. Files whose on-disk hash already matches
 * the manifest are skipped. See {@link UpdateManifest} for the manifest format.
 *
 * <p>The Quest equivalent is {@link QuestUpdateService}, which applies the same manifest
 * shape over adb instead of the local filesystem.
 */
public class UpdateService {

    private static volatile boolean cancelRequested = false;

    public static void cancelUpdate() {
        cancelRequested = true;
    }

    public static void applyUpdates(String manifestUrl, String installBinPath,
                                     JLabel progressLabel, JDialog frame, JFrame frameMain,
                                     Runnable onComplete) {
        cancelRequested = false;
        new Thread(() -> {
            try {
                System.out.println("UpdateService: downloading manifest " + manifestUrl);
                UpdateManifest manifest = UpdateManifest.fetch(manifestUrl);
                List<UpdateManifest.Entry> delEntries = manifest.dels();
                List<UpdateManifest.Entry> addEntries = manifest.adds();

                int total = delEntries.size() + addEntries.size();
                int current = 0;

                for (UpdateManifest.Entry e : delEntries) {
                    if (cancelRequested) return;
                    current++;
                    final int cur = current;
                    SwingUtilities.invokeLater(() ->
                        progressLabel.setText("Updating " + cur + "/" + total + ": deleting " + e.path + "..."));
                    System.out.println("UpdateService: deleting " + e.path);
                    Path target = Paths.get(installBinPath, e.path);
                    Files.deleteIfExists(target);
                }

                for (UpdateManifest.Entry e : addEntries) {
                    if (cancelRequested) return;
                    current++;
                    final int cur = current;
                    SwingUtilities.invokeLater(() ->
                        progressLabel.setText("Updating " + cur + "/" + total + ": " + e.path + "..."));
                    System.out.println("UpdateService: processing " + e.path);

                    Path target = Paths.get(installBinPath, e.path);

                    if (Files.exists(target)) {
                        try {
                            if (Helpers.sha256Matches(target, e.sha256)) {
                                System.out.println("UpdateService: skipping " + e.path + " (already up to date)");
                                continue;
                            }
                        } catch (Exception ex) {
                            System.out.println("UpdateService: couldn't verify existing " + e.path + ", re-downloading");
                        }
                    }

                    String fileUrl = manifest.urlFor(e);
                    Path tempFile = Files.createTempFile("echo_update_", ".tmp");
                    try {
                        System.out.println("UpdateService: downloading " + fileUrl);
                        Helpers.downloadFileSimple(fileUrl, tempFile);

                        if (!Helpers.sha256Matches(tempFile, e.sha256)) {
                            abortUpdate(frame, "Hash mismatch for " + e.path,
                                "The downloaded file " + e.path + " does not match the expected SHA-256 checksum.\nUpdate aborted.");
                            return;
                        }

                        Files.createDirectories(target.getParent());
                        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("UpdateService: placed " + e.path);
                    } finally {
                        try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
                    }
                }

                System.out.println("UpdateService: update complete");
                SwingUtilities.invokeLater(() -> {
                    progressLabel.setText("Update applied!");
                    if (onComplete != null) onComplete.run();
                });

            } catch (Exception e) {
                System.err.println("UpdateService: update failed: " + e.getMessage());
                e.printStackTrace();
                String title = (e instanceof IOException) ? "Please close Echo VR first" : "Update Failed";
                String message = (e instanceof IOException)
                    ? "Please close Echo VR before updating.\n\n" + e.getMessage()
                    : "An error occurred during update:\n" + e.getMessage() + "\n\nUpdate aborted.";
                abortUpdate(frame, title, message);
            }
        }).start();
    }

    private static void abortUpdate(JDialog frame, String title, String message) {
        SwingUtilities.invokeLater(() -> {
            new ErrorDialog().errorDialog(frame, title, message, 0);
        });
    }
}
