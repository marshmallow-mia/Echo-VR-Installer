package bl00dy_c0d3_.echovr_installer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single home for locating, targeting, invoking and logging the bundled {@code adb} binary.
 *
 * <p>Every invocation goes through one chokepoint here, which does three things no caller
 * has to remember:
 * <ul>
 *   <li><b>Traces the call.</b> Command, exit code, duration and (bounded) output land in
 *       the log under {@code **adb[n]} markers. The id is there because the install, the
 *       update service and the wizard's connection check all run on different background
 *       threads and their lines interleave.</li>
 *   <li><b>Pins the device.</b> A serial is resolved once from {@code adb devices -l} and
 *       passed as {@code -s <serial>} on every subcommand that accepts one. Without this a
 *       machine with a headset <em>and</em> anything else attached -- an emulator, a phone,
 *       a second headset -- fails every command with "more than one device/emulator".</li>
 *   <li><b>Names failures.</b> Output is scanned for adb's device-identity errors, which
 *       previously vanished into a discarded stream and left only a bare exit code.</li>
 * </ul>
 *
 * <p>Two invocation styles remain, and the distinction still matters:
 * <ul>
 *   <li>{@link #exec} / {@link #shellOut} build an argv and hand it straight to
 *       {@link ProcessBuilder}. Nothing re-parses the arguments, so paths containing
 *       spaces are safe on every platform. <b>Use these whenever the output is parsed.</b></li>
 *   <li>{@link #run} / {@link #runExit} concatenate into a command string routed through
 *       {@link Helpers#runShellCommand}, which on Windows goes through
 *       {@code Runtime.exec(String)} and whitespace-tokenizes. {@link InstallerQuest}'s
 *       install path uses these; prefer the argv forms in new code.</li>
 * </ul>
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
     * Adb path quoted for embedding in a command string. Only Windows gets quotes, matching
     * the inline strings this class replaced.
     *
     * <p>Caveat worth knowing: on Windows the command string goes through
     * {@code Runtime.exec(String)}, which splits with {@code StringTokenizer} and does
     * <em>not</em> honour quotes. This only works because {@code java.io.tmpdir} contains no
     * spaces in practice -- which is why {@link #logEnvironmentOnce()} puts the resolved
     * path in the log rather than leaving it to be guessed at.
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

    /** Prepares adb and returns the connection status: 0 = ready, 1 = unauthorized, 2 = several, -1 = none. */
    public static int connectionStatus() {
        return InstallerQuest.checkConnection();
    }

    // ------------------------------------------------------------------
    // target device
    // ------------------------------------------------------------------

    /** Subcommands that address the server rather than a device; {@code -s} is invalid or pointless. */
    private static final Set<String> NO_SERIAL = Set.of(
            "devices", "start-server", "kill-server", "version", "help", "connect", "disconnect");

    private static final Object LOCK = new Object();
    private static volatile String targetSerial;
    private static volatile boolean targetResolved;
    private static boolean resolving;
    private static volatile boolean diagnosing;

    /**
     * True when {@code -s <serial>} belongs before this subcommand.
     *
     * <p>Pure and unit-tested: a wrong answer here either corrupts a server command or
     * silently leaves an install unpinned.
     */
    static boolean takesSerial(String subcommand) {
        if (subcommand == null || subcommand.isEmpty()) return false;
        if (subcommand.startsWith("-")) return false;   // already a global flag; don't touch
        return !NO_SERIAL.contains(subcommand);
    }

    /**
     * The serial every device-addressed command is pinned to, resolving it on first use.
     *
     * <p>Returns null when nothing could be selected, in which case commands are built
     * exactly as they were before this class learned about serials.
     */
    static String targetSerial() {
        if (targetResolved) return targetSerial;
        synchronized (LOCK) {
            if (targetResolved) return targetSerial;
            if (resolving) return null;     // insurance: probe() only runs exempt subcommands
            resolving = true;
            try {
                AdbDevices.probe();             // adoptSelection() stores the result
            } finally {
                resolving = false;
            }
            return targetSerial;
        }
    }

    /**
     * Takes the serial a fresh probe just decided on.
     *
     * <p>Called by {@link AdbDevices#probe()} so that a probe run for its own sake -- the
     * wizard's connection check, the update service's gate -- also satisfies the lazy
     * resolution below. Without this the next device command would probe all over again.
     */
    static void adoptSelection(AdbDevices.Selection selection) {
        synchronized (LOCK) {
            targetSerial = selection.serial();
            targetResolved = true;
        }
    }

    /** Pins a serial chosen by the user, overriding the heuristic until the next invalidation. */
    public static void setTargetSerial(String serial) {
        synchronized (LOCK) {
            targetSerial = serial;
            targetResolved = true;
        }
        System.out.println("**adb: target serial set to " + serial + " (chosen by the user)");
    }

    /** Drops the cached serial so the next device command re-resolves it. */
    public static void clearTargetSerial(String why) {
        boolean had;
        synchronized (LOCK) {
            had = targetResolved;
            targetSerial = null;
            targetResolved = false;
        }
        if (had) System.out.println("**adb: target serial cleared -- " + why);
    }

    /** The last device table and the decision made from it. Never null. */
    public static AdbDevices.Selection lastSelection() {
        return AdbDevices.last();
    }

    /**
     * The device the pinned serial refers to, looked up in the last probe. For display only:
     * it reads the cache and never triggers a resolution, so it is safe to call from the EDT.
     *
     * @return the device, or null when nothing is pinned or it was not in the last table
     */
    public static AdbDevices.Device targetDevice() {
        String serial = targetSerial;
        if (serial == null) return AdbDevices.last().chosen();
        for (AdbDevices.Device device : AdbDevices.last().all()) {
            if (device.serial().equals(serial)) return device;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // command construction (pure -- unit tested)
    // ------------------------------------------------------------------

    /**
     * Builds the argv for {@code adb <args...>}, inserting {@code -s <serial>} before the
     * subcommand when one applies. {@code -s} is a global option, so position matters.
     */
    static List<String> buildArgv(String serial, String binary, String... args) {
        List<String> argv = new ArrayList<>();
        argv.add(binary);
        if (args.length > 0 && takesSerial(args[0]) && usableSerial(serial)) {
            argv.add("-s");
            argv.add(serial);
        }
        argv.addAll(Arrays.asList(args));
        return argv;
    }

    /** As {@link #buildArgv}, for the command-string form. */
    static String buildCommandLine(String serial, String adbPath, String argsTail) {
        String tail = argsTail == null ? "" : argsTail.trim();
        String subcommand = tail.isEmpty() ? null : tail.split("\\s+", 2)[0];
        if (takesSerial(subcommand) && usableSerial(serial)) {
            return adbPath + " -s " + serial + " " + tail;
        }
        return adbPath + " " + tail;
    }

    /**
     * Serials are USB serial numbers, {@code emulator-NNNN} or {@code host:port} -- never
     * whitespace-bearing. A serial that broke that rule would be split apart by the Windows
     * command-string path, so refuse it rather than emit a corrupt command line.
     */
    private static boolean usableSerial(String serial) {
        if (serial == null || serial.isEmpty()) return false;
        for (int i = 0; i < serial.length(); i++) {
            if (Character.isWhitespace(serial.charAt(i))) {
                System.out.println("**ADB-PROBLEM: refusing to inline a serial containing "
                        + "whitespace: '" + serial + "'");
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // output handling (pure -- unit tested)
    // ------------------------------------------------------------------

    private static final int MAX_LINES = 120;
    private static final int HEAD_LINES = 80;
    private static final int TAIL_LINES = 20;
    private static final int MAX_LINE_CHARS = 500;

    /** Newline-normalized, per-line truncated output. {@code adb push} progress is one
     *  enormous {@code \r}-laden line that {@code readLine} never splits. */
    static String normalize(String output) {
        if (output == null || output.isEmpty()) return "";
        String[] lines = output.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line.length() > MAX_LINE_CHARS
                    ? line.substring(0, MAX_LINE_CHARS) + " ...(line truncated)"
                    : line);
        }
        return sb.toString();
    }

    /**
     * Bounds successful output for the log, which is appended to forever and never rotated.
     * Failures are never elided -- those are the lines worth having.
     */
    static String excerpt(String output) {
        String normalized = normalize(output);
        if (normalized.isEmpty()) return "";
        String[] lines = normalized.split("\n");
        if (lines.length <= MAX_LINES) return normalized;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HEAD_LINES; i++) sb.append(lines[i]).append('\n');
        sb.append("... ").append(lines.length - HEAD_LINES - TAIL_LINES).append(" lines omitted ...\n");
        for (int i = lines.length - TAIL_LINES; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append('\n');
        }
        return sb.toString();
    }

    /** Device-identity failures: the target is wrong or gone, so the cached serial is stale. */
    private static final String[] IDENTITY_MARKERS = {
            "more than one device", "no devices/emulators found", "device offline",
            "device unauthorized"
    };

    /** Other adb failures worth a line in the log, but not a re-probe. */
    private static final String[] OTHER_MARKERS = {
            "adb: error:", "no permissions", "protocol fault", "error: closed",
            "cannot connect to daemon"
    };

    /** @return the matched problem marker, or null when the output looks clean. */
    static String diagnose(String output) {
        if (output == null || output.isEmpty()) return null;
        String lower = output.toLowerCase(Locale.ROOT);
        for (String marker : IDENTITY_MARKERS) {
            if (lower.contains(marker)) return marker;
        }
        if (lower.contains("device '") && lower.contains("' not found")) {
            return "device '...' not found";
        }
        for (String marker : OTHER_MARKERS) {
            if (lower.contains(marker)) return marker;
        }
        return null;
    }

    /** True when the problem means the pinned serial can no longer be trusted. */
    static boolean isIdentityProblem(String marker) {
        if (marker == null) return false;
        if (marker.equals("device '...' not found")) return true;
        for (String identity : IDENTITY_MARKERS) {
            if (identity.equals(marker)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // the invocation chokepoint
    // ------------------------------------------------------------------

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static volatile boolean environmentLogged;

    /** Records the resolved binary path once, so a broken temp path shows up in the log. */
    private static void logEnvironmentOnce() {
        if (environmentLogged) return;
        synchronized (LOCK) {
            if (environmentLogged) return;
            environmentLogged = true;
        }
        System.out.println("**adb: binary " + binary());
    }

    /** Runs {@code adb <args...>} through the trace. @return the exit code. */
    private static int invokeArgv(StringBuilder out, String... args) {
        logEnvironmentOnce();
        int id = SEQ.incrementAndGet();
        String subcommand = args.length > 0 ? args[0] : null;
        String serial = takesSerial(subcommand) ? targetSerial() : null;
        List<String> argv = buildArgv(serial, binary(), args);

        System.out.println("**adb[" + id + "] > argv: " + String.join(" ", argv));

        StringBuilder buffer = new StringBuilder();
        long start = System.nanoTime();
        int exit = Helpers.runArgvExit(argv, buffer);
        long millis = (System.nanoTime() - start) / 1_000_000L;

        String output = buffer.toString();
        report(id, exit, millis, output);
        if (out != null) out.append(output);
        afterRun(id, subcommand, output);
        return exit;
    }

    /** Runs {@code adb <argsTail>} through the shell, and through the trace. */
    private static String invokeCommand(String argsTail, int[] exitOut) {
        logEnvironmentOnce();
        int id = SEQ.incrementAndGet();
        String tail = argsTail == null ? "" : argsTail.trim();
        String subcommand = tail.isEmpty() ? null : tail.split("\\s+", 2)[0];
        String serial = takesSerial(subcommand) ? targetSerial() : null;
        String command = buildCommandLine(serial, path(), tail);

        System.out.println("**adb[" + id + "] > cmd: " + command);

        long start = System.nanoTime();
        // echo = false: this method does its own structured logging, and the public
        // runShellCommand would otherwise print the command and the raw output again.
        String output = Helpers.runShellCommand(command, false);
        long millis = (System.nanoTime() - start) / 1_000_000L;

        int exit = InstallerQuest.parseExitCode(output);
        // The "<" line already reports the exit code; the synthetic marker Helpers prepends
        // is only there for parseExitCode, so keep it out of the trace.
        report(id, exit, millis, stripExitMarker(output));
        if (exitOut != null) exitOut[0] = exit;
        afterRun(id, subcommand, output);
        return output;
    }

    private static void report(int id, int exit, long millis, String output) {
        System.out.println("**adb[" + id + "] < exit=" + exit + " in " + formatMillis(millis));
        // A failure is never elided: those are the lines that explain what went wrong.
        String body = exit == 0 ? excerpt(output) : normalize(output);
        if (body.isEmpty()) return;
        for (String line : body.split("\n")) {
            if (!line.isBlank()) System.out.println("**adb[" + id + "] | " + line);
        }
    }

    /** Drops {@link Helpers#runShellCommand}'s leading "Process exited with code N" line. */
    static String stripExitMarker(String output) {
        if (output == null) return "";
        String marker = "Process exited with code ";
        if (!output.startsWith(marker)) return output;
        int newline = output.indexOf('\n');
        return newline < 0 ? "" : output.substring(newline + 1);
    }

    private static String formatMillis(long millis) {
        if (millis < 1000) return millis + "ms";
        return String.format(Locale.ROOT, "%.1fs", millis / 1000.0);
    }

    private static void afterRun(int id, String subcommand, String output) {
        // Restarting the server reshuffles transports; a serial cached across it is stale.
        // Detected here, at the same place the -s exemption is decided, so it cannot be
        // forgotten by a caller.
        if (!resolving && ("kill-server".equals(subcommand) || "start-server".equals(subcommand))) {
            clearTargetSerial("adb " + subcommand);
        }

        String problem = diagnose(output);
        if (problem != null) reportProblem(id, problem);
    }

    /**
     * The block this whole change exists to produce: when adb refuses because of which
     * device it is talking to, say so unmistakably and show the device table.
     */
    private static void reportProblem(int id, String problem) {
        System.out.println("**ADB-PROBLEM: " + problem);
        System.out.println("**ADB-PROBLEM: raised by adb[" + id + "]");
        System.out.println("**ADB-PROBLEM: target serial was "
                + (targetSerial == null ? "<none>" : targetSerial));

        AdbDevices.Selection previous = AdbDevices.last();
        System.out.println("**ADB-PROBLEM: devices at the last probe (" + previous.reason() + "):");
        if (previous.all().isEmpty()) {
            System.out.println("**ADB-PROBLEM:   (none)");
        } else {
            for (AdbDevices.Device device : previous.all()) {
                System.out.println("**ADB-PROBLEM:   " + device.describe());
            }
        }

        if (!isIdentityProblem(problem)) return;

        boolean hadTarget = targetSerial != null;
        clearTargetSerial("device-identity error: " + problem);
        // Only worth re-reading when a pinned serial may have gone stale. With nothing
        // pinned, the last probe already says what is attached, and re-probing after every
        // failing command would triple the log for no new information.
        if (!hadTarget) return;
        if (diagnosing) return;     // a probe's own output must not re-enter here
        diagnosing = true;
        try {
            System.out.println("**ADB-PROBLEM: re-reading the device table now");
            AdbDevices.probe();
        } finally {
            diagnosing = false;
        }
    }

    // --- argv forms (preferred: no re-parsing, safe with spaces) ---

    /** Runs {@code adb <args...>} and returns its combined output. */
    public static String exec(String... args) {
        StringBuilder out = new StringBuilder();
        invokeArgv(out, args);
        return out.toString();
    }

    /** Runs {@code adb <args...>}, appending output to {@code out}; returns the exit code. */
    public static int execExit(StringBuilder out, String... args) {
        return invokeArgv(out, args);
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

    // --- command-string forms (the install path in InstallerQuest) ---

    /**
     * Runs {@code adb <argsTail>} through the shell and returns its output.
     *
     * <p>The returned string is {@link Helpers#runShellCommand}'s verbatim, including the
     * leading {@code "Process exited with code N"} marker -- {@link InstallerQuest} scrapes
     * that back out with {@code parseExitCode} and checks the push with
     * {@code contains("bytes")}.
     */
    public static String run(String argsTail) {
        return invokeCommand(argsTail, null);
    }

    /** Runs {@code adb <argsTail>} through the shell and returns its exit code. */
    public static int runExit(String argsTail) {
        int[] exit = new int[] {-1};
        invokeCommand(argsTail, exit);
        return exit[0];
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
