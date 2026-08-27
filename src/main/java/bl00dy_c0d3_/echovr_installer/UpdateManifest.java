package bl00dy_c0d3_.echovr_installer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed update manifest, shared by the PC and Quest update paths.
 *
 * <p>Body grammar (whitespace-separated; {@code #} comments and blank lines ignored):
 * <pre>
 *   add  path/to/file.dll  sha256hex
 *   del  path/to/old.dll
 * </pre>
 *
 * <p>The Quest manifest additionally carries two header comments that the PC one does
 * not. They live in {@code #} lines so the file stays readable by the plain body parser:
 * <pre>
 *   # BASE_APK: echo_quest_27-08-2026.001.apk 0a7fa5f9...
 *   # Target:  /sdcard/Android/media/com.readyatdawn.r15
 * </pre>
 *
 * <p>Manifest paths are attacker-influenced input that ends up interpolated into
 * {@code adb shell} scripts (including {@code rm -rf}) and into local filesystem paths,
 * so both the entry paths and the target root are validated strictly here. This is the
 * single choke point -- do not re-implement parsing elsewhere.
 */
public final class UpdateManifest {

    /**
     * Only unreserved path characters. Blocks whitespace, quotes, $ ` ; &amp; | * and friends.
     *
     * <p>{@code +} is allowed after the first character because the PC manifest ships
     * {@code libstdc++-6.dll} (the MinGW runtime trio, alongside {@code libgcc_s_seh-1.dll}
     * and {@code libwinpthread-1.dll}). It is neither a shell metacharacter nor a glob
     * character, so it is inert on the {@code adb shell} path this guards, and it is literal
     * in a URL path segment. Note {@code -} must stay last in the class to stay literal.
     */
    private static final Pattern SAFE_PATH = Pattern.compile("^[A-Za-z0-9._][A-Za-z0-9._/+-]*$");

    /** The Quest target must be an Echo VR app media dir -- nothing else may reach {@code rm -rf}. */
    private static final Pattern SAFE_TARGET =
            Pattern.compile("^/sdcard/Android/media/com\\.readyatdawn\\.[A-Za-z0-9]+$");

    private static final Pattern BASE_APK_HEADER =
            Pattern.compile("^#\\s*BASE_APK:\\s*(\\S+)\\s+([0-9a-fA-F]{64})\\s*$");

    private static final Pattern TARGET_HEADER =
            Pattern.compile("^#\\s*Target:\\s*(\\S+)\\s*$");

    /** A single manifest line. {@link #sha256} is null for {@code del} entries. */
    public static final class Entry {
        public final String action;
        public final String path;
        public final String sha256;

        Entry(String action, String path, String sha256) {
            this.action = action;
            this.path = path;
            this.sha256 = sha256;
        }

        public boolean isAdd() { return "add".equals(action); }

        public boolean isDel() { return "del".equals(action); }

        @Override
        public String toString() { return action + " " + path + (sha256 != null ? " " + sha256 : ""); }
    }

    private final List<Entry> entries;
    private final String baseUrl;
    private final String baseApkName;
    private final String baseApkSha;
    private final String targetRoot;

    private UpdateManifest(List<Entry> entries, String baseUrl,
                           String baseApkName, String baseApkSha, String targetRoot) {
        this.entries = Collections.unmodifiableList(entries);
        this.baseUrl = baseUrl;
        this.baseApkName = baseApkName;
        this.baseApkSha = baseApkSha;
        this.targetRoot = targetRoot;
    }

    /** Downloads and parses. Throws on network failure or a malformed/unsafe manifest. */
    public static UpdateManifest fetch(String manifestUrl) throws IOException {
        return parse(Helpers.downloadText(manifestUrl), manifestUrl);
    }

    /**
     * Best-effort {@link #fetch} for callers that have a working fallback (e.g. the install
     * wizard keeping its built-in APK name when offline).
     *
     * @return null if the manifest could not be fetched or parsed
     */
    public static UpdateManifest fetchQuiet(String manifestUrl) {
        try {
            return fetch(manifestUrl);
        } catch (Exception e) {
            System.out.println("UpdateManifest: could not fetch " + manifestUrl + " -- " + e.getMessage());
            return null;
        }
    }

    public static UpdateManifest parse(String content, String manifestUrl) {
        List<Entry> entries = new ArrayList<>();
        String baseApkName = null;
        String baseApkSha = null;
        String targetRoot = null;

        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Headers are comments, so they must be matched before the '#' skip below.
            if (trimmed.startsWith("#")) {
                Matcher apk = BASE_APK_HEADER.matcher(trimmed);
                if (apk.matches()) {
                    baseApkName = apk.group(1);
                    baseApkSha = apk.group(2);
                    continue;
                }
                Matcher target = TARGET_HEADER.matcher(trimmed);
                if (target.matches()) {
                    targetRoot = target.group(1);
                }
                continue;
            }

            String[] tokens = trimmed.split("\\s+");
            if (tokens.length < 2) continue;

            String action = tokens[0];
            String path = tokens[1];
            if (!"add".equals(action) && !"del".equals(action)) {
                throw new IllegalArgumentException("Unknown manifest action: " + action);
            }
            if (!SAFE_PATH.matcher(path).matches() || path.contains("..")) {
                throw new IllegalArgumentException("Unsafe path in manifest: " + path);
            }
            if ("add".equals(action)) {
                if (tokens.length < 3) {
                    throw new IllegalArgumentException("Missing SHA-256 for manifest entry: " + path);
                }
                entries.add(new Entry(action, path, tokens[2]));
            } else {
                entries.add(new Entry(action, path, null));
            }
        }

        if (targetRoot != null && !SAFE_TARGET.matcher(targetRoot).matches()) {
            throw new IllegalArgumentException("Unsafe target root in manifest: " + targetRoot);
        }

        String baseUrl = manifestUrl == null || manifestUrl.lastIndexOf('/') < 0
                ? "" : manifestUrl.substring(0, manifestUrl.lastIndexOf('/'));

        return new UpdateManifest(entries, baseUrl, baseApkName, baseApkSha, targetRoot);
    }

    public List<Entry> entries() { return entries; }

    public List<Entry> adds() { return filter(true); }

    public List<Entry> dels() { return filter(false); }

    private List<Entry> filter(boolean adds) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.isAdd() == adds) out.add(e);
        }
        return out;
    }

    /** Manifest URL up to (excluding) the last '/'; individual files resolve against it. */
    public String baseUrl() { return baseUrl; }

    public String urlFor(Entry e) { return baseUrl + "/" + e.path; }

    /** APK filename from the {@code # BASE_APK:} header, or null if absent. */
    public String baseApkName() { return baseApkName; }

    /** SHA-256 of the base APK this manifest was built against, or null if absent. */
    public String baseApkSha() { return baseApkSha; }

    /** On-device root the entry paths are relative to, or null if absent (PC manifests). */
    public String targetRoot() { return targetRoot; }
}
