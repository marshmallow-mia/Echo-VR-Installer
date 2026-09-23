package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless tests for the pure parts of {@link Adb}: where the {@code -s <serial>} flag goes,
 * which subcommands take one, how adb's failure messages are recognised, and how output is
 * bounded for the log.
 *
 * <p>The command builders take the serial as a parameter precisely so these assertions need
 * no device and never touch the static target cache.
 */
public class AdbTest {

    private static final String SERIAL = "1WMHH8150XX07M";
    private static final String BIN = "/tmp/platform-tools/adb";

    // --- which subcommands take a serial ---

    @Test
    void serverSubcommandsTakeNoSerial() {
        assertFalse(Adb.takesSerial("devices"));
        assertFalse(Adb.takesSerial("start-server"));
        assertFalse(Adb.takesSerial("kill-server"));
        assertFalse(Adb.takesSerial("version"));
        assertFalse(Adb.takesSerial("help"));
        assertFalse(Adb.takesSerial("connect"));
        assertFalse(Adb.takesSerial("disconnect"));
    }

    @Test
    void deviceSubcommandsTakeASerial() {
        assertTrue(Adb.takesSerial("install"));
        assertTrue(Adb.takesSerial("uninstall"));
        assertTrue(Adb.takesSerial("push"));
        assertTrue(Adb.takesSerial("pull"));
        assertTrue(Adb.takesSerial("shell"));
        assertTrue(Adb.takesSerial("reboot"));
    }

    @Test
    void anExistingGlobalFlagOrNothingIsLeftAlone() {
        assertFalse(Adb.takesSerial("-s"));
        assertFalse(Adb.takesSerial("-t"));
        assertFalse(Adb.takesSerial(null));
        assertFalse(Adb.takesSerial(""));
    }

    // --- argv form ---

    /** {@code -s} is a global option: it must precede the subcommand, never follow it. */
    @Test
    void serialIsInsertedBeforeTheSubcommand() {
        List<String> argv = Adb.buildArgv(SERIAL, BIN, "install", "-g", "/tmp/echo.apk");
        assertEquals(List.of(BIN, "-s", SERIAL, "install", "-g", "/tmp/echo.apk"), argv);
        assertEquals("-s", argv.get(1));
        assertEquals(SERIAL, argv.get(2));
        assertEquals("install", argv.get(3));
    }

    @Test
    void serialIsOmittedForServerSubcommands() {
        assertEquals(List.of(BIN, "devices", "-l"), Adb.buildArgv(SERIAL, BIN, "devices", "-l"));
        assertEquals(List.of(BIN, "kill-server"), Adb.buildArgv(SERIAL, BIN, "kill-server"));
        assertEquals(List.of(BIN, "start-server"), Adb.buildArgv(SERIAL, BIN, "start-server"));
    }

    /** With no serial resolved, the argv must be byte-for-byte what it was before pinning. */
    @Test
    void noSerialReproducesTheOriginalArgv() {
        assertEquals(List.of(BIN, "shell", "ls"), Adb.buildArgv(null, BIN, "shell", "ls"));
        assertEquals(List.of(BIN, "shell", "ls"), Adb.buildArgv("", BIN, "shell", "ls"));
    }

    @Test
    void shellScriptStaysASingleArgvElement() {
        List<String> argv = Adb.buildArgv(SERIAL, BIN, "shell", "cd /sdcard/x; unzip a.zip");
        assertEquals(5, argv.size());
        assertEquals("cd /sdcard/x; unzip a.zip", argv.get(4));
    }

    // --- command-string form ---

    @Test
    void commandLinePlacesTheSerialBeforeTheSubcommand() {
        assertEquals("\"C:/adb.exe\" -s " + SERIAL + " uninstall com.readyatdawn.r15",
                Adb.buildCommandLine(SERIAL, "\"C:/adb.exe\"", "uninstall com.readyatdawn.r15"));
    }

    @Test
    void commandLineOmitsTheSerialForServerSubcommands() {
        assertEquals("\"C:/adb.exe\" kill-server",
                Adb.buildCommandLine(SERIAL, "\"C:/adb.exe\"", "kill-server"));
        assertEquals("\"C:/adb.exe\" devices",
                Adb.buildCommandLine(SERIAL, "\"C:/adb.exe\"", "devices"));
    }

    @Test
    void commandLineWithoutASerialIsUnchanged() {
        assertEquals("\"C:/adb.exe\" install -g \"C:/x/echo.apk\"",
                Adb.buildCommandLine(null, "\"C:/adb.exe\"", "install -g \"C:/x/echo.apk\""));
    }

    /** A whitespace-bearing serial would be split by the Windows command-string path. */
    @Test
    void aWhitespaceSerialIsRefusedRatherThanInlined() {
        assertEquals("/adb shell ls", Adb.buildCommandLine("bad serial", "/adb", "shell ls"));
        assertEquals(List.of(BIN, "shell", "ls"), Adb.buildArgv("bad serial", BIN, "shell", "ls"));
    }

    // --- failure recognition ---

    @Test
    void adbDeviceErrorsAreRecognised() {
        assertEquals("more than one device",
                Adb.diagnose("adb: error: more than one device/emulator"));
        assertEquals("no devices/emulators found",
                Adb.diagnose("error: no devices/emulators found"));
        assertEquals("device offline", Adb.diagnose("error: device offline"));
        assertEquals("device unauthorized", Adb.diagnose("error: device unauthorized."));
        assertEquals("device '...' not found", Adb.diagnose("adb: device 'ABC123' not found"));
    }

    @Test
    void identityProblemsAreDistinguishedFromOtherFailures() {
        assertTrue(Adb.isIdentityProblem(Adb.diagnose("adb: error: more than one device/emulator")));
        assertTrue(Adb.isIdentityProblem(Adb.diagnose("adb: device 'ABC' not found")));
        assertFalse(Adb.isIdentityProblem(Adb.diagnose("adb: error: failed to stat remote object")));
        assertFalse(Adb.isIdentityProblem(null));
    }

    @Test
    void cleanOutputDiagnosesToNull() {
        assertNull(Adb.diagnose("Performing Streamed Install\nSuccess\n"));
        assertNull(Adb.diagnose(""));
        assertNull(Adb.diagnose(null));
    }

    // --- output bounding ---

    @Test
    void carriageReturnProgressIsSplitIntoLines() {
        String pushProgress = "[ 12%] /data/local/tmp/_data.zip\r[ 47%] /data/local/tmp/_data.zip"
                + "\r[100%] /data/local/tmp/_data.zip";
        assertEquals(3, Adb.normalize(pushProgress).split("\n").length);
    }

    @Test
    void oneEnormousLineIsTruncated() {
        String normalized = Adb.normalize("x".repeat(5000));
        assertTrue(normalized.length() < 600, "expected truncation, got " + normalized.length());
        assertTrue(normalized.endsWith("(line truncated)"));
    }

    @Test
    void shortOutputIsNotElided() {
        String output = "Performing Streamed Install\nSuccess";
        assertEquals(output, Adb.excerpt(output));
    }

    @Test
    void longOutputKeepsHeadAndTailAndCountsWhatItDropped() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) sb.append("line").append(i).append('\n');

        String[] lines = Adb.excerpt(sb.toString()).split("\n");
        assertEquals(80 + 1 + 20, lines.length);
        assertEquals("line0", lines[0]);
        assertEquals("line79", lines[79]);
        assertEquals("... 400 lines omitted ...", lines[80]);
        assertEquals("line480", lines[81]);
        assertEquals("line499", lines[lines.length - 1]);
    }

    @Test
    void excerptIsDeterministic() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) sb.append("l").append(i).append('\n');
        assertEquals(Adb.excerpt(sb.toString()), Adb.excerpt(sb.toString()));
    }
}
