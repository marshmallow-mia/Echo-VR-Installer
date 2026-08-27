package bl00dy_c0d3_.echovr_installer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Quest counterpart to {@link UpdateService}: applies the same manifest format over adb
 * instead of the local filesystem.
 *
 * <p>Unlike PC, a Quest update is only safe against the exact APK the manifest was built
 * for, so every run is gated by {@link #checkVersion}. Because Discord-personalized APKs
 * are repacked -- their SHA-256 can never equal the manifest's {@code BASE_APK} hash -- a
 * marker file written at install time records <em>which base version</em> an install
 * corresponds to. The marker lives on the headset rather than the PC, so an update works
 * from any machine. See {@link #decide} for the full decision table.
 */
public class QuestUpdateService {

    /** Where the on-device base-version marker lives, at the manifest's target root. */
    public static final String MARKER_PATH =
            "/sdcard/Android/media/" + Helpers.QUEST_PACKAGE + "/.echo_installer_version";

    private static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-fA-F]{64}$");

    /** Cap on one batched {@code sha256sum} invocation, in paths and in characters. */
    private static final int HASH_BATCH_PATHS = 50;
    private static final int HASH_BATCH_CHARS = 3000;

    private static volatile boolean cancelRequested = false;

    public static void cancelUpdate() {
        cancelRequested = true;
    }

    /** Outcome of the pre-flight version check. */
    public enum VersionCheck { OK, NOT_INSTALLED, MISMATCH, NO_DEVICE, MANIFEST_ERROR }

    /** Result of {@link #checkVersion} / {@link #decide}. */
    public static final class Status {
        public final VersionCheck result;
        /** Human-readable reason, shown verbatim in the mismatch dialog. */
        public final String detail;
        public final String installedSha;
        public final Marker marker;
        public final UpdateManifest manifest;
        /** True when {@link #decide} determined a missing marker should be back-filled. */
        public final boolean shouldSelfHealMarker;

        Status(VersionCheck result, String detail, String installedSha,
               Marker marker, UpdateManifest manifest, boolean shouldSelfHealMarker) {
            this.result = result;
            this.detail = detail;
            this.installedSha = installedSha;
            this.marker = marker;
            this.manifest = manifest;
            this.shouldSelfHealMarker = shouldSelfHealMarker;
        }

        public boolean isOk() { return result == VersionCheck.OK; }
    }

    /**
     * On-device record of which base version an install came from.
     *
     * <p>Android wipes {@code /sdcard/Android/media/<pkg>} when the app is uninstalled, so
     * a marker can never outlive its install -- exactly the semantic we want.
     */
    public static final class Marker {
        public String baseApk;
        /** SHA-256 of the base APK the install was built from. */
        public String baseSha256;
        /** SHA-256 of the APK actually installed; differs from {@link #baseSha256} when patched. */
        public String installedSha256;
        public boolean patched;
        public String installedAt;
        public String installerVersion;

        /** Tolerant parser: any line without '=' is ignored, so adb/cat noise is harmless. */
        public static Marker parse(String text) {
            if (text == null || text.isEmpty()) return null;
            Marker m = new Marker();
            boolean sawAny = false;
            for (String rawLine : text.split("\n")) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                switch (key) {
                    case "base_apk":          m.baseApk = value; sawAny = true; break;
                    case "base_sha256":       m.baseSha256 = value; sawAny = true; break;
                    case "installed_sha256":  m.installedSha256 = value; sawAny = true; break;
                    case "patched":           m.patched = Boolean.parseBoolean(value); sawAny = true; break;
                    case "installed_at":      m.installedAt = value; break;
                    case "installer_version": m.installerVersion = value; break;
                    default: break;
                }
            }
            return sawAny ? m : null;
        }

        public String serialize() {
            return "version=1\n"
                 + "base_apk=" + nullToEmpty(baseApk) + "\n"
                 + "base_sha256=" + nullToEmpty(baseSha256) + "\n"
                 + "installed_sha256=" + nullToEmpty(installedSha256) + "\n"
                 + "patched=" + patched + "\n"
                 + "installed_at=" + nullToEmpty(installedAt) + "\n"
                 + "installer_version=" + nullToEmpty(installerVersion) + "\n";
        }

        private static String nullToEmpty(String s) { return s == null ? "" : s; }
    }


    // ------------------------------------------------------------------
    // Device queries
    // ------------------------------------------------------------------

    /**
     * Absolute path of the installed base APK, or null if the package isn't installed.
     *
     * <p>Detection keys off the presence of a {@code package:} line rather than the exit
     * code: builds disagree on whether "not installed" exits 0-with-empty-stdout or 1.
     */
    public static String installedApkPath() {
        String out = Adb.exec("shell", "pm", "path", Helpers.QUEST_PACKAGE);
        if (out == null) return null;
        for (String line : out.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package:")) {
                String path = trimmed.substring("package:".length()).trim();
                if (!path.isEmpty()) return path;
            }
        }
        return null;
    }

    public static boolean isPackageInstalled() {
        return installedApkPath() != null;
    }

    /**
     * SHA-256 of the installed APK, hashed on the device. Falls back to pulling the ~96 MB
     * APK and hashing locally only when {@code sha256sum} is unusable.
     *
     * @return lowercase hex, or null if it could not be determined
     */
    public static String installedApkSha() {
        String apkPath = installedApkPath();
        if (apkPath == null) return null;

        String hash = firstHashToken(Adb.exec("shell", "sha256sum", apkPath));
        if (hash != null) return hash;

        System.out.println("QuestUpdateService: on-device sha256sum unusable, pulling APK to hash locally");
        Path temp = null;
        try {
            temp = Files.createTempFile("echo_quest_apk_", ".apk");
            // adb pull refuses to overwrite nothing, but createTempFile already made the file.
            Files.deleteIfExists(temp);
            if (!Adb.pull(apkPath, temp) || !Files.exists(temp)) return null;
            return Helpers.sha256Hex(temp);
        } catch (IOException e) {
            System.err.println("QuestUpdateService: could not hash installed APK: " + e.getMessage());
            return null;
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
            }
        }
    }

    /** First whitespace-delimited token of {@code output}, if it is a 64-char hex hash. */
    static String firstHashToken(String output) {
        if (output == null) return null;
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String token = trimmed.split("\\s+")[0];
            if (SHA256_HEX.matcher(token).matches()) return token.toLowerCase();
        }
        return null;
    }

    /**
     * Parses batched {@code sha256sum} output into path -&gt; hash. Lines for missing files
     * (and any {@code cat}/{@code sha256sum} error noise) are skipped.
     */
    static Map<String, String> parseHashListing(String output) {
        Map<String, String> hashes = new HashMap<>();
        if (output == null) return hashes;
        for (String rawLine : output.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            if (parts.length < 2) continue;
            if (!SHA256_HEX.matcher(parts[0]).matches()) continue;
            hashes.put(parts[1].trim(), parts[0].toLowerCase());
        }
        return hashes;
    }


    // ------------------------------------------------------------------
    // Marker
    // ------------------------------------------------------------------

    public static Marker readMarker() {
        return Marker.parse(Adb.exec("shell", "cat", MARKER_PATH));
    }

    /**
     * Writes the marker by pushing a locally-built file. Deliberately not
     * {@code adb shell "echo ... > file"}: shell redirection through the Windows
     * {@code Runtime.exec} path is fragile, while push is the transfer already proven by
     * the install flow.
     */
    public static boolean writeMarker(Marker marker) {
        Path temp = null;
        try {
            temp = Files.createTempFile("echo_marker_", ".txt");
            Files.writeString(temp, marker.serialize());
            Adb.shellOut("mkdir -p /sdcard/Android/media/" + Helpers.QUEST_PACKAGE);
            boolean ok = Adb.pushFile(temp, MARKER_PATH);
            System.out.println("QuestUpdateService: marker write " + (ok ? "ok" : "FAILED"));
            return ok;
        } catch (IOException e) {
            System.err.println("QuestUpdateService: could not write marker: " + e.getMessage());
            return false;
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
            }
        }
    }

    /** Convenience used by the install wizard right after a successful {@code adb install}. */
    public static boolean writeMarkerAfterInstall(String baseApk, String baseSha,
                                                  String installedSha, boolean patched) {
        Marker m = new Marker();
        m.baseApk = baseApk;
        m.baseSha256 = baseSha;
        m.installedSha256 = installedSha;
        m.patched = patched;
        m.installedAt = Instant.now().toString();
        m.installerVersion = Helpers.VERSION_TITLE;
        return writeMarker(m);
    }


    // ------------------------------------------------------------------
    // Version gate
    // ------------------------------------------------------------------

    /**
     * Decides whether an update may proceed. Pure -- no device access -- so the whole
     * table is unit-testable.
     *
     * <table>
     *   <caption>Decision table</caption>
     *   <tr><th>Installed</th><th>Marker</th><th>Condition</th><th>Result</th></tr>
     *   <tr><td>no</td><td>-</td><td>-</td><td>NOT_INSTALLED</td></tr>
     *   <tr><td>yes</td><td>yes</td><td>base matches and installed hash agrees</td><td>OK</td></tr>
     *   <tr><td>yes</td><td>yes</td><td>base matches, installed hash differs</td><td>MISMATCH</td></tr>
     *   <tr><td>yes</td><td>yes</td><td>base differs</td><td>MISMATCH</td></tr>
     *   <tr><td>yes</td><td>no</td><td>installed hash == manifest base</td><td>OK + self-heal</td></tr>
     *   <tr><td>yes</td><td>no</td><td>otherwise</td><td>MISMATCH</td></tr>
     * </table>
     */
    static Status decide(UpdateManifest manifest, Marker marker,
                         boolean installed, String installedSha) {
        if (manifest == null) {
            return new Status(VersionCheck.MANIFEST_ERROR,
                    "The update manifest could not be downloaded.", installedSha, marker, null, false);
        }
        if (!installed) {
            return new Status(VersionCheck.NOT_INSTALLED,
                    "Echo VR is not installed on your Quest.", installedSha, marker, manifest, false);
        }

        String manifestBase = manifest.baseApkSha();
        if (manifestBase == null) {
            // No BASE_APK header: nothing to gate on, so allow the update through.
            return new Status(VersionCheck.OK, "", installedSha, marker, manifest, false);
        }

        if (marker != null && marker.baseSha256 != null) {
            if (!manifestBase.equalsIgnoreCase(marker.baseSha256)) {
                return new Status(VersionCheck.MISMATCH,
                        "The Echo VR version on your Quest is older than this update"
                        + (marker.baseApk != null ? " (installed from " + marker.baseApk + ")." : "."),
                        installedSha, marker, manifest, false);
            }
            if (installedSha != null && marker.installedSha256 != null
                    && !installedSha.equalsIgnoreCase(marker.installedSha256)) {
                return new Status(VersionCheck.MISMATCH,
                        "The Echo VR app on your Quest was replaced since it was installed.",
                        installedSha, marker, manifest, false);
            }
            return new Status(VersionCheck.OK, "", installedSha, marker, manifest, false);
        }

        // No marker: only a stock base APK can be recognised, and we back-fill the marker.
        if (installedSha != null && manifestBase.equalsIgnoreCase(installedSha)) {
            return new Status(VersionCheck.OK, "", installedSha, marker, manifest, true);
        }
        return new Status(VersionCheck.MISMATCH,
                "The Echo VR version installed on your Quest could not be matched to this update.",
                installedSha, marker, manifest, false);
    }

    /**
     * Runs the pre-flight on a background thread and hands the {@link Status} back on the EDT.
     * Self-heals a missing marker when the installed APK is the stock base build.
     */
    public static void checkVersion(String manifestUrl, JLabel progressLabel,
                                    JDialog parent, Consumer<Status> onResult) {
        new Thread(() -> {
            Status status;
            try {
                setLabel(progressLabel, "Checking your Quest...");
                Helpers.prepareAdb();
                if (InstallerQuest.checkQuestStatus() != 0) {
                    status = new Status(VersionCheck.NO_DEVICE,
                            "Your Quest is no longer connected.", null, null, null, false);
                } else {
                    setLabel(progressLabel, "Checking for updates...");
                    UpdateManifest manifest = UpdateManifest.fetchQuiet(manifestUrl);

                    setLabel(progressLabel, "Checking your Echo VR version...");
                    String apkPath = installedApkPath();
                    String installedSha = apkPath == null ? null : installedApkSha();
                    Marker marker = apkPath == null ? null : readMarker();

                    status = decide(manifest, marker, apkPath != null, installedSha);

                    if (status.isOk() && status.shouldSelfHealMarker) {
                        System.out.println("QuestUpdateService: no marker found, back-filling from base APK");
                        writeMarkerAfterInstall(manifest.baseApkName(), manifest.baseApkSha(),
                                installedSha, false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                status = new Status(VersionCheck.MANIFEST_ERROR,
                        "Could not check for updates:\n" + e.getMessage(), null, null, null, false);
            }
            final Status result = status;
            SwingUtilities.invokeLater(() -> onResult.accept(result));
        }).start();
    }


    // ------------------------------------------------------------------
    // Apply
    // ------------------------------------------------------------------

    /**
     * Applies {@code manifest} to the headset. Mirrors {@link UpdateService#applyUpdates}:
     * {@code del} entries first, then {@code add} entries whose on-device hash differs.
     * Runs on its own thread; all UI updates are marshalled to the EDT.
     */
    public static void applyUpdates(UpdateManifest manifest, SpecialLabel progressLabel,
                                    JDialog frame, JFrame frameMain, Runnable onComplete) {
        applyUpdates(manifest, progressLabel, frame, frameMain, onComplete, null);
    }

    /**
     * As above, with {@code onFailure} run on the EDT when the update aborts. Callers that
     * disable their UI while updating need this -- otherwise an abort leaves the wizard
     * stuck mid-step with no way forward.
     */
    public static void applyUpdates(UpdateManifest manifest, SpecialLabel progressLabel,
                                    JDialog frame, JFrame frameMain,
                                    Runnable onComplete, Runnable onFailure) {
        cancelRequested = false;
        new Thread(() -> {
            try {
                String root = manifest.targetRoot();
                if (root == null) {
                    abortUpdate(frame, "Update Failed",
                            "The Quest update manifest is missing its target location.\nUpdate aborted.", onFailure);
                    return;
                }
                String adbPath = Adb.prepare();

                List<UpdateManifest.Entry> dels = manifest.dels();
                List<UpdateManifest.Entry> adds = manifest.adds();
                int total = dels.size() + adds.size();
                int current = 0;

                for (UpdateManifest.Entry e : dels) {
                    if (cancelRequested) { runOnEdt(onFailure); return; }
                    current++;
                    setLabel(progressLabel, "Updating " + current + "/" + total + ": deleting " + e.path + "...");
                    System.out.println("QuestUpdateService: deleting " + e.path);
                    // rm -f exits 0 for a missing file; a leftover file is not worth aborting over.
                    if (!InstallerQuest.executeWithReconnect(adbPath,
                            () -> Adb.shellExit(null, "rm -rf " + root + "/" + e.path),
                            "rm " + e.path, progressLabel)) {
                        System.out.println("QuestUpdateService: could not delete " + e.path + ", continuing");
                    }
                }

                Map<String, String> remoteHashes = remoteHashes(root, adds, progressLabel);

                String lastParent = null;
                for (UpdateManifest.Entry e : adds) {
                    if (cancelRequested) { runOnEdt(onFailure); return; }
                    current++;
                    String remote = root + "/" + e.path;

                    String existing = remoteHashes.get(e.path);
                    if (existing != null && existing.equalsIgnoreCase(e.sha256)) {
                        setLabel(progressLabel, "Updating " + current + "/" + total + ": " + e.path + " (up to date)");
                        System.out.println("QuestUpdateService: skipping " + e.path + " (already up to date)");
                        continue;
                    }

                    setLabel(progressLabel, "Updating " + current + "/" + total + ": " + e.path + "...");

                    Path tempFile = Files.createTempFile("echo_quest_update_", ".tmp");
                    try {
                        String fileUrl = manifest.urlFor(e);
                        System.out.println("QuestUpdateService: downloading " + fileUrl);
                        Helpers.downloadFileSimple(fileUrl, tempFile);

                        if (!Helpers.sha256Matches(tempFile, e.sha256)) {
                            abortUpdate(frame, "Hash mismatch for " + e.path,
                                "The downloaded file " + e.path + " does not match the expected SHA-256 checksum.\nUpdate aborted.", onFailure);
                            return;
                        }

                        // The live manifest groups entries by directory, so this collapses
                        // many mkdirs into one per directory.
                        int slash = remote.lastIndexOf('/');
                        String parent = slash > 0 ? remote.substring(0, slash) : root;
                        if (!parent.equals(lastParent)) {
                            Adb.shellOut("mkdir -p " + parent);
                            lastParent = parent;
                        }

                        System.out.println("QuestUpdateService: pushing " + remote);
                        final Path push = tempFile;
                        if (!InstallerQuest.executeWithReconnect(adbPath,
                                () -> Adb.pushFile(push, remote) ? 0 : 1,
                                "push " + e.path, progressLabel)) {
                            abortUpdate(frame, "Transfer Failed",
                                "Failed to copy " + e.path + " to your Quest.\nUpdate aborted.", onFailure);
                            return;
                        }
                    } finally {
                        try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
                    }
                }

                // /sdcard is a synthesized FUSE mount, so chmod may be a no-op or outright
                // fail depending on the build. Never fatal.
                int chmod = Adb.shellExit(null, "chmod -R 777 " + root);
                if (chmod != 0) {
                    System.out.println("QuestUpdateService: chmod on " + root + " returned " + chmod + " (ignored)");
                }

                System.out.println("QuestUpdateService: update complete");
                SwingUtilities.invokeLater(() -> {
                    progressLabel.setText("Update applied!");
                    if (onComplete != null) onComplete.run();
                });

            } catch (Exception e) {
                System.err.println("QuestUpdateService: update failed: " + e.getMessage());
                e.printStackTrace();
                abortUpdate(frame, "Update Failed",
                        "An error occurred during update:\n" + e.getMessage() + "\n\nUpdate aborted.", onFailure);
            }
        }).start();
    }

    /**
     * Hashes the manifest's target files on the device in as few adb round-trips as
     * possible. When {@code sha256sum} is unavailable an empty map is returned, which
     * simply disables skipping -- pulling every file to hash it locally would cost more
     * than re-pushing them.
     */
    private static Map<String, String> remoteHashes(String root, List<UpdateManifest.Entry> adds,
                                                     SpecialLabel progressLabel) {
        Map<String, String> hashes = new HashMap<>();
        if (adds.isEmpty()) return hashes;

        if (firstHashToken(Adb.exec("shell", "sha256sum", "/system/build.prop")) == null) {
            System.out.println("QuestUpdateService: sha256sum unavailable on device, pushing all files");
            return hashes;
        }

        setLabel(progressLabel, "Checking existing files...");
        Set<String> seen = new HashSet<>();
        List<String> batch = new ArrayList<>();
        int batchChars = 0;
        for (UpdateManifest.Entry e : adds) {
            if (!seen.add(e.path)) continue;
            if (!batch.isEmpty()
                    && (batch.size() >= HASH_BATCH_PATHS || batchChars + e.path.length() > HASH_BATCH_CHARS)) {
                hashes.putAll(hashBatch(root, batch));
                batch.clear();
                batchChars = 0;
            }
            batch.add(e.path);
            batchChars += e.path.length() + 1;
        }
        if (!batch.isEmpty()) hashes.putAll(hashBatch(root, batch));
        return hashes;
    }

    /**
     * One batched hash call. The script is a single argv element, so the host shell never
     * splits it; {@link UpdateManifest}'s path validation guarantees the device's {@code sh}
     * sees no metacharacters either.
     */
    private static Map<String, String> hashBatch(String root, List<String> paths) {
        String script = "cd " + root + " && sha256sum " + String.join(" ", paths) + " 2>/dev/null";
        return parseHashListing(Adb.shellOut(script));
    }

    private static void setLabel(JLabel label, String text) {
        if (label != null) SwingUtilities.invokeLater(() -> label.setText(text));
    }

    private static void runOnEdt(Runnable r) {
        if (r != null) SwingUtilities.invokeLater(r);
    }

    private static void abortUpdate(JDialog frame, String title, String message, Runnable onFailure) {
        SwingUtilities.invokeLater(() -> {
            new ErrorDialog().errorDialog(frame, title, message, 0);
            if (onFailure != null) onFailure.run();
        });
    }
}
