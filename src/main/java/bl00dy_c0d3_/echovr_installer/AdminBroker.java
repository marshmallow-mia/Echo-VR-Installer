package bl00dy_c0d3_.echovr_installer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Client side of the elevation broker. Operations that need administrator rights are first
 * attempted in-process; if they fail, the broker asks the user for consent and launches the same
 * executable elevated ({@link AdminHelper}). Once a helper is running it is <b>reused</b> for all
 * later operations — only the first elevation shows a UAC prompt.
 *
 * <p>Talks to the helper over an authenticated {@code 127.0.0.1} socket using a fixed, line-based
 * protocol. See {@link AdminHelper} for the trust model.
 */
public final class AdminBroker {

    private static final AdminBroker INSTANCE = new AdminBroker();
    public static AdminBroker get() { return INSTANCE; }

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private AdminBroker() {}

    // ---- High-level, try-in-process-then-elevate operations used by the wizard ----

    public static ReviveSetup.VrManifestResult patchVrManifest(Component parent, String reviveDir, String exe) throws IOException {
        try {
            return ReviveSetup.patchVrManifest(reviveDir, exe);
        } catch (IOException e) {
            System.out.println("[AdminBroker] patchVrManifest failed in-process (" + e + ") -> trying elevated helper");
            if (!get().ensure(parent)) throw e;
            String r = get().exec("PATCH_VRMANIFEST\t" + reviveDir + "\t" + exe);
            return ReviveSetup.VrManifestResult.valueOf(r);
        }
    }

    public static void installArtwork(Component parent) throws IOException {
        try {
            ReviveSetup.installArtwork();
        } catch (IOException e) {
            System.out.println("[AdminBroker] installArtwork failed in-process (" + e + ") -> trying elevated helper");
            if (!get().ensure(parent)) throw e;
            get().exec("INSTALL_ARTWORK");
        }
    }

    /**
     * Runs an installer that may require elevation. Tries in-process first; if launching fails
     * (e.g. {@code CreateProcess error=740, requires elevation}), runs it through the elevated helper.
     *
     * @return the installer's exit code.
     */
    public static int runInstaller(Component parent, String path, String... args) throws IOException {
        try {
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(path);
            for (String a : args) cmd.add(a);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) System.out.println("INSTALLER: " + l);
            }
            return p.waitFor();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while running installer", ie);
        } catch (IOException e) {
            System.out.println("[AdminBroker] installer launch failed in-process (" + e + ") -> trying elevated helper");
            if (!get().ensure(parent)) throw e;
            StringBuilder line = new StringBuilder("RUN_INSTALLER\t").append(path);
            for (String a : args) line.append('\t').append(a);
            String r = get().exec(line.toString());
            try { return Integer.parseInt(r.trim()); } catch (NumberFormatException nfe) { return 0; }
        }
    }

    /** Kills any running Revive process — via the elevated helper if connected (Revive started by
     *  the elevated installer is itself elevated), otherwise a best-effort local taskkill. */
    public static void stopRevive(Component parent) {
        AdminBroker b = get();
        if (b.isConnected()) {
            try { b.exec("KILL_REVIVE"); return; } catch (IOException ignored) {}
        }
        ReviveSetup.stopRevive();
    }

    public static void createInjectorShortcut(Component parent, String reviveDir, String exe) {
        // Writes to the user's Desktop — normally no admin needed; only escalate if a helper is
        // already running (cheap) or the direct call clearly couldn't write.
        ReviveSetup.createInjectorShortcut(reviveDir, exe);
    }

    // ---- Connection lifecycle ----

    /** True if a helper connection is alive (verified with a PING). */
    public synchronized boolean isConnected() {
        if (socket == null || socket.isClosed() || !socket.isConnected()) return false;
        try {
            String r = exec("PING");
            return "".equals(r) || r != null;
        } catch (IOException e) {
            closeQuietly();
            return false;
        }
    }

    /**
     * Ensures an elevated helper is available, launching it (with user consent + one UAC prompt)
     * if necessary. Reuses an existing helper.
     *
     * @return true if a helper is connected and ready, false if unavailable or the user declined.
     */
    public synchronized boolean ensure(Component parent) {
        if (isConnected()) return true;
        if (!Helpers.isWindows) {
            System.out.println("[AdminBroker] elevation not supported on this OS");
            return false;
        }
        if (!askConsent(parent)) {
            System.out.println("[AdminBroker] user declined elevation");
            return false;
        }
        try {
            return launchHelper();
        } catch (Exception e) {
            System.err.println("[AdminBroker] failed to launch helper: " + e);
            e.printStackTrace();
            return false;
        }
    }

    private boolean askConsent(Component parent) {
        final boolean[] yes = {false};
        Runnable r = () -> {
            int c = JOptionPane.showConfirmDialog(parent,
                "<html>This step needs administrator rights to write into protected folders<br>"
                    + "(installing Echo into the Meta folder and applying Revive patches).<br><br>"
                    + "Start the privileged helper now? Windows will ask you to confirm.</html>",
                "Administrator rights required", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            yes[0] = (c == JOptionPane.YES_OPTION);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try { SwingUtilities.invokeAndWait(r); } catch (Exception e) { return false; }
        }
        return yes[0];
    }

    private boolean launchHelper() throws Exception {
        String exe = ProcessHandle.current().info().command().orElse(null);
        if (exe == null || exe.toLowerCase().endsWith("java.exe") || exe.toLowerCase().endsWith("javaw.exe")) {
            System.err.println("[AdminBroker] elevation requires the packaged application (current launcher: " + exe + ")");
            return false;
        }

        String token = UUID.randomUUID().toString();
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        String stamp = Long.toHexString(System.nanoTime());
        Path tokenFile = tmp.resolve("evr-helper-" + stamp + ".token");
        Path portFile = tmp.resolve("evr-helper-" + stamp + ".port");
        Files.deleteIfExists(portFile);
        Files.writeString(tokenFile, token, StandardCharsets.UTF_8);
        long pid = ProcessHandle.current().pid();

        String ps = "Start-Process -FilePath '" + exe.replace("'", "''") + "' "
            + "-ArgumentList '--admin-helper','" + portFile.toString().replace("'", "''") + "',"
            + "'" + tokenFile.toString().replace("'", "''") + "','" + pid + "' -Verb RunAs";
        System.out.println("[AdminBroker] launching elevated helper: " + exe);
        int code = Helpers.runShellCommandWithExitCode("powershell -Command \"" + ps + "\"");
        if (code != 0) {
            System.err.println("[AdminBroker] Start-Process RunAs returned " + code + " (UAC declined?)");
            cleanup(tokenFile, portFile);
            return false;
        }

        // Wait for the helper to publish its port.
        int port = -1;
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(portFile)) {
                try {
                    String s = Files.readString(portFile, StandardCharsets.UTF_8).trim();
                    if (!s.isEmpty()) { port = Integer.parseInt(s); break; }
                } catch (Exception ignored) {}
            }
            Thread.sleep(200);
        }
        if (port <= 0) {
            System.err.println("[AdminBroker] helper did not publish a port in time");
            cleanup(tokenFile, portFile);
            return false;
        }

        // Connect + authenticate.
        Socket s = new Socket(InetAddress.getLoopbackAddress(), port);
        BufferedReader bin = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter bout = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
        bout.write(token);
        bout.write('\n');
        bout.flush();
        String hello = bin.readLine();
        if (!"OK".equals(hello)) {
            System.err.println("[AdminBroker] handshake failed: " + hello);
            try { s.close(); } catch (IOException ignored) {}
            cleanup(tokenFile, portFile);
            return false;
        }
        this.socket = s;
        this.in = bin;
        this.out = bout;
        cleanup(tokenFile, portFile);
        System.out.println("[AdminBroker] elevated helper connected on port " + port);
        return true;
    }

    /** Sends one operation line and returns the OK payload, or throws with the helper's error. */
    public synchronized String exec(String line) throws IOException {
        if (socket == null || in == null || out == null) throw new IOException("helper not connected");
        out.write(line);
        out.write('\n');
        out.flush();
        String resp = in.readLine();
        if (resp == null) { closeQuietly(); throw new IOException("helper disconnected"); }
        if (resp.equals("OK")) return "";
        if (resp.startsWith("OK\t")) return resp.substring(3);
        if (resp.startsWith("ERR\t")) throw new IOException(resp.substring(4));
        throw new IOException("unexpected helper response: " + resp);
    }

    public synchronized void shutdown() {
        if (socket != null) {
            try { out.write("SHUTDOWN\n"); out.flush(); } catch (Exception ignored) {}
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; in = null; out = null;
    }

    private static void cleanup(Path... files) {
        for (Path f : files) { try { Files.deleteIfExists(f); } catch (Exception ignored) {} }
    }
}
