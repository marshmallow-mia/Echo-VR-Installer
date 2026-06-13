package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Manifest-based update engine.
 *
 * <p>Downloads a manifest file at {@code manifestUrl}, processes all {@code del} entries
 * first, then downloads and SHA-256-verifies each {@code add} entry. The base URL for
 * individual files is derived from the manifest URL (everything before the last '/').
 *
 * <p>Manifest format (whitespace-separated, {@code #} comments and blank lines supported):
 * <pre>
 *   add  path/to/file.dll  sha256hex
 *   del  path/to/old.dll
 * </pre>
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
                String manifestContent = downloadText(manifestUrl);
                List<ManifestEntry> entries = parseManifest(manifestContent);
                String baseUrl = manifestUrl.substring(0, manifestUrl.lastIndexOf('/'));

                List<ManifestEntry> delEntries = new ArrayList<>();
                List<ManifestEntry> addEntries = new ArrayList<>();
                for (ManifestEntry e : entries) {
                    if ("del".equals(e.action)) delEntries.add(e);
                    else if ("add".equals(e.action)) addEntries.add(e);
                }

                int total = delEntries.size() + addEntries.size();
                int current = 0;

                for (ManifestEntry e : delEntries) {
                    if (cancelRequested) return;
                    current++;
                    final int cur = current;
                    SwingUtilities.invokeLater(() ->
                        progressLabel.setText("Updating " + cur + "/" + total + ": deleting " + e.path + "..."));
                    System.out.println("UpdateService: deleting " + e.path);
                    Path target = Paths.get(installBinPath, e.path);
                    Files.deleteIfExists(target);
                }

                for (ManifestEntry e : addEntries) {
                    if (cancelRequested) return;
                    current++;
                    final int cur = current;
                    SwingUtilities.invokeLater(() ->
                        progressLabel.setText("Updating " + cur + "/" + total + ": " + e.path + "..."));
                    System.out.println("UpdateService: processing " + e.path);

                    Path target = Paths.get(installBinPath, e.path);

                    if (Files.exists(target)) {
                        try {
                            if (sha256Matches(target, e.sha256)) {
                                System.out.println("UpdateService: skipping " + e.path + " (already up to date)");
                                continue;
                            }
                        } catch (Exception ex) {
                            System.out.println("UpdateService: couldn't verify existing " + e.path + ", re-downloading");
                        }
                    }

                    String fileUrl = baseUrl + "/" + e.path;
                    Path tempFile = Files.createTempFile("echo_update_", ".tmp");
                    try {
                        System.out.println("UpdateService: downloading " + fileUrl);
                        downloadFileSimple(fileUrl, tempFile);

                        if (!sha256Matches(tempFile, e.sha256)) {
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

    private static List<ManifestEntry> parseManifest(String content) {
        List<ManifestEntry> entries = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            String[] tokens = trimmed.split("\\s+");
            if (tokens.length < 2) continue;

            String action = tokens[0];
            String path = tokens[1];

            if (path.contains("..") || path.startsWith("/")) {
                System.err.println("UpdateService: path traversal detected in manifest: " + path);
                throw new RuntimeException("Path traversal detected in manifest: " + path);
            }

            if ("add".equals(action)) {
                if (tokens.length < 3) {
                    throw new RuntimeException("Missing SHA-256 for add entry: " + path);
                }
                String sha256 = tokens[2];
                entries.add(new ManifestEntry(action, path, sha256));
            } else if ("del".equals(action)) {
                entries.add(new ManifestEntry(action, path, null));
            }
        }
        return entries;
    }

    private static String downloadText(String url) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new URL(url).openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static void downloadFileSimple(String url, Path destination) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean sha256Matches(Path file, String expectedHex) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    digest.update(buf, 0, n);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(expectedHex);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }

    private static void abortUpdate(JDialog frame, String title, String message) {
        SwingUtilities.invokeLater(() -> {
            new ErrorDialog().errorDialog(frame, title, message, 0);
        });
    }

    private static class ManifestEntry {
        final String action;
        final String path;
        final String sha256;

        ManifestEntry(String action, String path, String sha256) {
            this.action = action;
            this.path = path;
            this.sha256 = sha256;
        }
    }
}
