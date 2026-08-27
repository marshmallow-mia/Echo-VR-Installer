package bl00dy_c0d3_.echovr_installer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Single home for locating and invoking the bundled {@code adb} binary.
 *
 * <p>Two invocation styles are offered, and the distinction matters:
 * <ul>
 *   <li>{@link #exec} / {@link #shellOut} build an argv and hand it straight to
 *       {@link ProcessBuilder}. Nothing re-parses the arguments, so paths containing
 *       spaces are safe on every platform. <b>Use these whenever the output is parsed.</b></li>
 *   <li>{@link #run} / {@link #runExit} concatenate into a command string routed through
 *       {@link Helpers#runShellCommand}, which on Windows goes through
 *       {@code Runtime.exec(String)} and whitespace-tokenizes. These exist so the
 *       pre-existing install path in {@link InstallerQuest} keeps behaving byte-for-byte
 *       as it did; prefer the argv forms in new code.</li>
 * </ul>
 *
 * <p>Note: no invocation passes {@code -s <serial>}, so a machine with two headsets
 * attached will get adb's "more than one device" error. That limitation predates this
 * class and applies to the whole installer.
 */
public final class Adb {

    private Adb() {}

    /** Android package name of Echo VR on Quest. */
    public static final String PACKAGE = Helpers.QUEST_PACKAGE;

    /** Cached -- checkIfChromeOs() touches the filesystem and logs on every call. */
    private static final boolean IS_CHROME = Helpers.checkIfChromeOs();

    private static Path tempPath() {
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    /** Raw, unquoted path to the adb binary -- for {@link ProcessBuilder} / argv use. */
    public static String binary() {
        Path temp = tempPath();
        if (Helpers.isWindows) return temp + "/platform-tools/adb.exe";
        if (IS_CHROME) return "adb";
        if (Helpers.mac) return temp + "/platform-tools-mac/adb";
        return temp + "/platform-tools-linux/adb";
    }

    /**
     * Adb path quoted for embedding in a command string. Byte-identical to the inline
     * strings this class replaced -- only Windows gets quotes, matching the old code.
     */
    public static String path() {
        if (Helpers.isWindows) return "\"" + binary() + "\"";
        return binary();
    }

    /** Extracts the bundled platform-tools if needed, then returns {@link #path()}. */
    public static String prepare() {
        Helpers.prepareAdb();
        return path();
    }

    /** Prepares adb and returns the connection status: 0 = ready, 1 = unauthorized, -1 = none. */
    public static int connectionStatus() {
        return InstallerQuest.checkConnection();
    }

    // --- argv forms (preferred: no re-parsing, safe with spaces) ---

    /** Runs {@code adb <args...>} and returns its combined output. */
    public static String exec(String... args) {
        return Helpers.runArgv(argv(args));
    }

    /** Runs {@code adb <args...>}, appending output to {@code out}; returns the exit code. */
    public static int execExit(StringBuilder out, String... args) {
        return Helpers.runArgvExit(argv(args), out);
    }

    /**
     * Runs a shell script on the device. The whole script is passed as a <em>single</em>
     * argv element, so only the device's {@code sh} ever splits it -- the host shell
     * never sees it.
     */
    public static String shellOut(String script) {
        return exec("shell", script);
    }

    /** As {@link #shellOut}, but returns the exit code. */
    public static int shellExit(StringBuilder out, String script) {
        return execExit(out, "shell", script);
    }

    private static List<String> argv(String... args) {
        List<String> argv = new ArrayList<>();
        argv.add(binary());
        argv.addAll(Arrays.asList(args));
        return argv;
    }

    // --- command-string forms (legacy behaviour of InstallerQuest) ---

    /** Runs {@code adb <argsTail>} through the shell and returns its output. */
    public static String run(String argsTail) {
        return Helpers.runShellCommand(path() + " " + argsTail);
    }

    /** Runs {@code adb <argsTail>} through the shell and returns its exit code. */
    public static int runExit(String argsTail) {
        return Helpers.runShellCommandWithExitCode(path() + " " + argsTail);
    }

    /**
     * Pushes a local file to an absolute remote <em>file</em> path.
     *
     * <p>Always pass the full destination filename, never a directory: local staging files
     * carry temp names, and pushing to a directory would land them under the wrong name.
     * The parent directory must already exist -- older adb builds create a plain file
     * named after a missing parent rather than the directory.
     *
     * @return true if adb reported a transfer and exited cleanly
     */
    public static boolean pushFile(Path local, String remoteFilePath) {
        StringBuilder out = new StringBuilder();
        int exit = execExit(out, "push", local.toString(), remoteFilePath);
        String output = out.toString();
        boolean transferred = output.contains("bytes") && !output.contains("0 files pushed");
        return exit == 0 && transferred;
    }

    /** Pulls a remote path to a local destination. @return true on a clean exit. */
    public static boolean pull(String remotePath, Path localDest) {
        return execExit(null, "pull", remotePath, localDest.toString()) == 0;
    }
}
