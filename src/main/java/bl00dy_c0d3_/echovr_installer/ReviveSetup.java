package bl00dy_c0d3_.echovr_installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Backend for the PC wizard "Steam Patch (Revive)" stage.
 *
 * <p>UI-agnostic: each operation either returns a result enum or throws so the
 * caller can drive the live checklist and surface errors. Covers the post-install
 * Revive setup documented in {@code steampatch.md}: locating Revive, creating the
 * Revive-injector desktop shortcut, patching {@code revive.vrmanifest}, restoring the
 * Meta Horizon dashboard manifests, and installing the correct game artwork.
 */
public class ReviveSetup {

    // Revive
    public static final String DEFAULT_REVIVE_DIR = "C:\\Program Files\\Revive";
    public static final String REVIVE_INJECTOR = "ReviveInjector.exe";
    public static final String VRMANIFEST = "revive.vrmanifest";

    // Echo identifiers / Meta Horizon layout
    public static final String APP_KEY = "revive.app.ready-at-dawn-echo-arena";
    public static final String APP_ID = "ready-at-dawn-echo-arena";
    public static final String META_HORIZON_DIR = "C:\\Program Files\\Meta Horizon";
    public static final String STORE_ASSETS_DIR =
        META_HORIZON_DIR + "\\CoreData\\Software\\StoreAssets\\" + APP_ID + "_assets";
    public static final String IMAGE_PATH =
        "C:/Program Files/Meta Horizon/CoreData/Software/StoreAssets/" + APP_ID
            + "_assets/cover_landscape_image_large.png";

    // Download sources
    public static final String ARTWORK_ZIP_URL =
        "https://files.echovr.de/stuff/patches/" + APP_ID + "_assets.zip";

    private static final Pattern LIBRARY_ID = Pattern.compile("/library\\s+(\\S+)");

    private static void log(String msg) { System.out.println("[ReviveSetup] " + msg); }
    private static void logErr(String msg) { System.err.println("[ReviveSetup] " + msg); }

    /** Outcome of {@link #patchVrManifest}. */
    public enum VrManifestResult {
        /** Entry inserted into an existing manifest with a detected library id. */
        ADDED,
        /** Existing Echo entry was refreshed. */
        UPDATED,
        /** No library id could be detected (manifest empty/missing) — caller shows fallback guidance. */
        EMPTY_MANIFEST
    }

    /**
     * Locates an installed Revive directory.
     * <ol>
     *   <li>Reads the Windows uninstall registry for a "Revive" entry's InstallLocation.</li>
     *   <li>Falls back to {@link #DEFAULT_REVIVE_DIR}.</li>
     * </ol>
     * In every case the candidate is verified by confirming {@link #REVIVE_INJECTOR} exists,
     * since the registry can be stale.
     *
     * @return the verified Revive directory, or {@code null} if Revive is not installed.
     */
    public static String findReviveDir() {
        if (!Helpers.isWindows) { log("findReviveDir: not Windows, Revive unavailable"); return null; }

        log("findReviveDir: querying uninstall registry for Revive...");
        String fromRegistry = queryReviveInstallLocation();
        if (fromRegistry != null) {
            log("findReviveDir: registry InstallLocation = '" + fromRegistry + "'");
            String verified = verifyReviveDir(fromRegistry);
            if (verified != null) {
                log("findReviveDir: verified Revive at '" + verified + "' (" + REVIVE_INJECTOR + " present)");
                return verified;
            }
            logErr("findReviveDir: registry path did not contain " + REVIVE_INJECTOR + ", trying default");
        } else {
            log("findReviveDir: no Revive entry in registry, trying default '" + DEFAULT_REVIVE_DIR + "'");
        }
        String fallback = verifyReviveDir(DEFAULT_REVIVE_DIR);
        if (fallback != null) {
            log("findReviveDir: verified Revive at default '" + fallback + "'");
        } else {
            logErr("findReviveDir: Revive not found (registry + default both missing " + REVIVE_INJECTOR + ")");
        }
        return fallback;
    }

    public static boolean isReviveInstalled() {
        return findReviveDir() != null;
    }

    /** Likely Revive dashboard/overlay executables, in preference order. */
    public static final String[] REVIVE_DASHBOARD_EXES = {"ReviveDashboard.exe", "ReviveOverlay.exe", "Revive.exe"};

    /** True if any Revive dashboard/overlay process is currently running. */
    public static boolean isReviveRunning() {
        if (!Helpers.isWindows) return false;
        for (String name : REVIVE_DASHBOARD_EXES) {
            try {
                String out = Helpers.runShellCommandWithOutput("tasklist /FI \"IMAGENAME eq " + name + "\" /NH");
                if (out != null && out.toLowerCase().contains(name.toLowerCase())) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /** Terminates any running Revive dashboard/overlay processes (best effort). */
    public static void stopRevive() {
        if (!Helpers.isWindows) return;
        for (String name : REVIVE_DASHBOARD_EXES) {
            try {
                Helpers.runShellCommandWithExitCode("taskkill /F /IM " + name);
                log("stopRevive: taskkill " + name);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Launches the Revive dashboard/overlay (non-elevated, non-blocking) so Revive/SteamVR can run
     * and populate {@code revive.vrmanifest}. Best-effort: returns false if no launcher is found.
     */
    public static boolean startRevive(String reviveDir) {
        if (!Helpers.isWindows || reviveDir == null) return false;
        File dir = new File(reviveDir);
        File exe = null;
        for (String name : REVIVE_DASHBOARD_EXES) {
            File f = new File(dir, name);
            if (f.isFile()) { exe = f; break; }
        }
        if (exe == null) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String n = f.getName().toLowerCase();
                    if (f.isFile() && n.endsWith(".exe") && n.contains("revive")
                        && (n.contains("overlay") || n.contains("dashboard"))) { exe = f; break; }
                }
            }
        }
        if (exe == null) {
            logErr("startRevive: no Revive dashboard/overlay exe found in " + reviveDir);
            return false;
        }
        try {
            log("startRevive: launching " + exe.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(exe.getAbsolutePath());
            pb.directory(dir);
            pb.start(); // non-blocking; Revive keeps running in the background
            return true;
        } catch (Exception e) {
            logErr("startRevive: failed to launch: " + e);
            return false;
        }
    }

    /** Reads the library ID directly from {@code revive.vrmanifest}, or null if absent/empty. */
    public static String detectLibraryIdFromManifest(String reviveDir) {
        File manifest = new File(reviveDir, VRMANIFEST);
        if (!manifest.isFile()) return null;
        try {
            String content = Files.readString(manifest.toPath(), StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(content);
            return parsed.isJsonObject() ? detectLibraryId(parsed.getAsJsonObject().getAsJsonArray("applications")) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String queryReviveInstallLocation() {
        String cmd = "powershell -Command \""
            + "Get-ItemProperty "
            + "'HKLM:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*',"
            + "'HKLM:\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*' "
            + "-ErrorAction SilentlyContinue | "
            + "Where-Object { $_.DisplayName -like '*Revive*' } | "
            + "Select-Object -ExpandProperty InstallLocation -First 1\"";
        try {
            String out = Helpers.runShellCommandWithOutput(cmd);
            if (out == null) return null;
            String dir = out.trim();
            return dir.isEmpty() ? null : dir;
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the directory (trailing separators trimmed) if it contains ReviveInjector.exe, else null. */
    private static String verifyReviveDir(String dir) {
        if (dir == null || dir.isEmpty()) return null;
        String trimmed = dir.replaceAll("[\\\\/]+$", "");
        File injector = new File(trimmed, REVIVE_INJECTOR);
        return injector.isFile() ? trimmed : null;
    }

    /**
     * Creates a desktop shortcut launching Echo VR through the Revive injector — the
     * "can't press any buttons in-game" fix from the megathread.
     */
    public static void createInjectorShortcut(String reviveDir, String exePath) {
        String injector = new File(reviveDir, REVIVE_INJECTOR).getAbsolutePath();
        String args = String.format("\"%s\" -nosymbollookup /app %s",
            new File(exePath).getAbsolutePath(), APP_ID);
        File existing = new File(System.getProperty("user.home"), "Desktop/Echo VR (Revive).lnk");
        log("createInjectorShortcut: desktop shortcut 'Echo VR (Revive)' "
            + (existing.exists() ? "already exists -> OVERWRITING" : "does not exist -> CREATING"));
        log("createInjectorShortcut: target=" + injector);
        log("createInjectorShortcut: arguments=" + args);
        log("createInjectorShortcut: workingDir=" + reviveDir);
        Helpers.createShortcut("Echo VR (Revive)", injector, args, reviveDir, injector);
        log("createInjectorShortcut: done");
    }

    /**
     * Inserts or refreshes the Echo entry in {@code revive.vrmanifest}.
     *
     * @return {@link VrManifestResult#EMPTY_MANIFEST} when no library id can be detected
     *         (so the caller shows the "download a free Meta app and restart SteamVR" fallback),
     *         otherwise {@link VrManifestResult#ADDED} / {@link VrManifestResult#UPDATED}.
     */
    public static VrManifestResult patchVrManifest(String reviveDir, String exePath) throws IOException {
        File manifest = new File(reviveDir, VRMANIFEST);
        log("patchVrManifest: target = " + manifest.getAbsolutePath());
        if (!manifest.isFile()) {
            logErr("patchVrManifest: " + VRMANIFEST + " does not exist -> EMPTY_MANIFEST (cannot patch)");
            return VrManifestResult.EMPTY_MANIFEST;
        }

        JsonObject root;
        try {
            String content = Files.readString(manifest.toPath(), StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(content);
            root = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            throw new IOException("Could not read " + VRMANIFEST + ": " + e.getMessage(), e);
        }

        JsonArray apps = root.getAsJsonArray("applications");
        if (apps == null) {
            log("patchVrManifest: no 'applications' array present -> creating one");
            apps = new JsonArray();
            root.add("applications", apps);
        }
        log("patchVrManifest: manifest currently has " + apps.size() + " application entr(ies)");

        String libraryId = detectLibraryId(apps);
        if (libraryId == null) {
            logErr("patchVrManifest: could not detect a library ID from existing entries -> EMPTY_MANIFEST");
            return VrManifestResult.EMPTY_MANIFEST;
        }
        log("patchVrManifest: detected library ID = " + libraryId);

        JsonObject existing = findEntry(apps, APP_KEY);
        boolean isUpdate = existing != null;
        if (isUpdate) {
            String oldArgs = existing.has("arguments") && !existing.get("arguments").isJsonNull()
                ? existing.get("arguments").getAsString() : "(none)";
            log("patchVrManifest: existing '" + APP_KEY + "' entry found -> OVERWRITING");
            log("patchVrManifest:   old arguments = " + oldArgs);
        } else {
            log("patchVrManifest: no '" + APP_KEY + "' entry -> ADDING new entry");
        }

        JsonObject entry = isUpdate ? existing : new JsonObject();
        fillEchoEntry(entry, libraryId, exePath);
        if (!isUpdate) apps.add(entry);
        log("patchVrManifest:   new arguments = " + entry.get("arguments").getAsString());
        log("patchVrManifest:   image_path    = " + entry.get("image_path").getAsString());

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String json = gson.toJson(root);
        Files.writeString(manifest.toPath(), json, StandardCharsets.UTF_8);
        log("patchVrManifest: wrote " + json.getBytes(StandardCharsets.UTF_8).length + " bytes; result = "
            + (isUpdate ? "UPDATED" : "ADDED"));
        return isUpdate ? VrManifestResult.UPDATED : VrManifestResult.ADDED;
    }

    /** Extracts the shared library id from the first existing entry's {@code arguments} ({@code /library <id>}). */
    public static String detectLibraryId(JsonArray apps) {
        if (apps == null) return null;
        for (JsonElement el : apps) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("arguments") || obj.get("arguments").isJsonNull()) continue;
            Matcher m = LIBRARY_ID.matcher(obj.get("arguments").getAsString());
            if (m.find()) {
                String id = m.group(1);
                if (!id.equalsIgnoreCase("put-library-ID-here")) return id;
            }
        }
        return null;
    }

    private static JsonObject findEntry(JsonArray apps, String appKey) {
        for (JsonElement el : apps) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("app_key") && appKey.equals(obj.get("app_key").getAsString())) {
                    return obj;
                }
            }
        }
        return null;
    }

    private static void fillEchoEntry(JsonObject entry, String libraryId, String exePath) {
        entry.addProperty("action_manifest_path", "Input/action_manifest.json");
        entry.addProperty("app_key", APP_KEY);
        entry.addProperty("arguments", String.format(
            "/app %s /library %s \"Software\\ready-at-dawn-echo-arena\\bin\\win10\\echovr.exe\" -nosymbollookup",
            APP_ID, libraryId));
        entry.addProperty("binary_path_windows", REVIVE_INJECTOR);
        entry.addProperty("image_path", IMAGE_PATH);
        entry.addProperty("launch_type", "binary");
        JsonObject strings = new JsonObject();
        JsonObject enUs = new JsonObject();
        enUs.addProperty("name", APP_ID);
        strings.add("en_us", enUs);
        entry.add("strings", strings);
    }

    /**
     * Downloads the correct game artwork and extracts it into the Meta Horizon StoreAssets folder,
     * creating the target folder if missing.
     */
    public static void installArtwork() throws IOException {
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"), "revive");
        Files.createDirectories(tmp);
        Path zip = tmp.resolve(APP_ID + "_assets.zip");
        log("installArtwork: downloading " + ARTWORK_ZIP_URL);
        log("installArtwork:   -> " + zip);
        Helpers.downloadFile(ARTWORK_ZIP_URL, zip.toString());
        log("installArtwork: downloaded " + zip.toFile().length() + " bytes");

        File dest = new File(STORE_ASSETS_DIR);
        if (dest.exists()) {
            log("installArtwork: target folder exists, files will be overwritten: " + STORE_ASSETS_DIR);
        } else {
            log("installArtwork: target folder missing -> CREATING " + STORE_ASSETS_DIR);
            if (!dest.mkdirs()) throw new IOException("Could not create artwork folder: " + STORE_ASSETS_DIR);
        }

        // Log each entry and whether it overwrites an existing file.
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            int files = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                files++;
                boolean overwrite = new File(dest, entry.getName()).exists();
                log("installArtwork:   " + (overwrite ? "OVERWRITE" : "ADD     ") + " " + entry.getName());
            }
            log("installArtwork: extracting " + files + " file(s) into " + STORE_ASSETS_DIR);
        }
        UnzipFile.unzip(zip.toString(), STORE_ASSETS_DIR);
        log("installArtwork: done");
    }

    /**
     * Restores the Meta Horizon dashboard manifests (.json/.mini) so Echo appears in the Revive
     * Dashboard. Only relevant for players who had Echo on PC before the shutdown.
     *
     * @throws UnsupportedOperationException until the hosted manifest URLs are wired in.
     */
    public static void restoreDashboardManifests() {
        logErr("restoreDashboardManifests: not yet available (no hosted .json/.mini URLs); target would be "
            + META_HORIZON_DIR + "\\Manifests");
        throw new UnsupportedOperationException(
            "Dashboard manifest restore is not available yet. For now, place the .json and .mini files "
                + "manually into " + META_HORIZON_DIR + "\\Manifests, then relaunch Revive.");
    }
}
