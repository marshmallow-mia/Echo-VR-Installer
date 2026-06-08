package bl00dy_c0d3_.echovr_installer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * The privileged ("admin") side of the elevation broker. The same application executable is
 * relaunched elevated with {@code --admin-helper <portFile> <tokenFile> <parentPid>}; this class
 * then becomes a small request/response server that performs admin-only operations on behalf of
 * the (non-elevated) main process.
 *
 * <p>Security model:
 * <ul>
 *   <li>Listens on {@code 127.0.0.1} only and accepts a <b>single</b> connection.</li>
 *   <li>The first line from the client must equal the shared one-time token (delivered to this
 *       process via {@code tokenFile}); otherwise the connection is dropped and the helper exits.</li>
 *   <li>Only a <b>fixed vocabulary</b> of named operations is honoured — never an arbitrary
 *       command string — so the socket cannot be turned into a general "run anything as admin" hole.</li>
 *   <li>Exits as soon as the client disconnects or the parent process dies.</li>
 * </ul>
 */
public final class AdminHelper {

    private AdminHelper() {}

    public static void main(String[] args) {
        redirectLog();
        if (args.length < 4) {
            System.err.println("[AdminHelper] usage: --admin-helper <portFile> <tokenFile> <parentPid>");
            System.exit(2);
        }
        Path portFile = Paths.get(args[1]);
        Path tokenFile = Paths.get(args[2]);
        long parentPid = parseLong(args[3], -1);
        System.out.println("[AdminHelper] starting (parentPid=" + parentPid + ")");

        String expectedToken;
        try {
            expectedToken = Files.readString(tokenFile, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            System.err.println("[AdminHelper] cannot read token file: " + e);
            System.exit(3);
            return;
        }

        watchParent(parentPid);

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(30_000); // give the client 30s to connect
            int port = server.getLocalPort();
            // Publish the chosen port so the client knows where to connect.
            Files.writeString(portFile, Integer.toString(port), StandardCharsets.UTF_8);
            System.out.println("[AdminHelper] listening on 127.0.0.1:" + port);

            try (Socket client = server.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

                String clientToken = in.readLine();
                if (clientToken == null || !clientToken.equals(expectedToken)) {
                    System.err.println("[AdminHelper] token mismatch — dropping connection");
                    return;
                }
                deleteQuietly(tokenFile);
                reply(out, "OK");
                System.out.println("[AdminHelper] client authenticated; ready");

                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\t", -1);
                    String op = parts[0];
                    if ("SHUTDOWN".equals(op)) { System.out.println("[AdminHelper] shutdown requested"); break; }
                    handle(op, parts, out);
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminHelper] fatal: " + e);
            e.printStackTrace();
        } finally {
            deleteQuietly(portFile);
            System.out.println("[AdminHelper] exiting");
            System.exit(0);
        }
    }

    private static void handle(String op, String[] a, BufferedWriter out) throws Exception {
        System.out.println("[AdminHelper] op: " + op);
        try {
            switch (op) {
                case "PING" -> reply(out, "OK");
                case "PATCH_VRMANIFEST" -> {
                    ReviveSetup.VrManifestResult r = ReviveSetup.patchVrManifest(a[1], a[2]);
                    reply(out, "OK\t" + r.name());
                }
                case "INSTALL_ARTWORK" -> {
                    ReviveSetup.installArtwork();
                    reply(out, "OK");
                }
                case "RESTORE_DASHBOARD" -> {
                    try {
                        ReviveSetup.restoreDashboardManifests();
                        reply(out, "OK");
                    } catch (UnsupportedOperationException u) {
                        reply(out, "ERR\t" + u.getMessage());
                    }
                }
                case "CREATE_SHORTCUT" -> {
                    ReviveSetup.createInjectorShortcut(a[1], a[2]);
                    reply(out, "OK");
                }
                case "RUN_INSTALLER" -> reply(out, "OK\t" + runInstaller(a));
                case "KILL_REVIVE" -> { ReviveSetup.stopRevive(); reply(out, "OK"); }
                default -> reply(out, "ERR\tunknown op: " + op);
            }
        } catch (Exception e) {
            System.err.println("[AdminHelper] op " + op + " failed: " + e);
            reply(out, "ERR\t" + safe(e.getMessage()));
        }
    }

    private static int runInstaller(String[] a) throws Exception {
        // a[0] = "RUN_INSTALLER", a[1] = path, a[2..] = args
        java.util.List<String> cmd = new java.util.ArrayList<>();
        for (int i = 1; i < a.length; i++) cmd.add(a[i]);
        System.out.println("[AdminHelper] running installer (elevated): " + cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = r.readLine()) != null) System.out.println("INSTALLER: " + l);
        }
        return p.waitFor();
    }

    private static void reply(BufferedWriter out, String line) throws Exception {
        out.write(line);
        out.write('\n');
        out.flush();
    }

    private static void watchParent(long pid) {
        if (pid <= 0) return;
        Thread t = new Thread(() -> {
            while (true) {
                Optional<ProcessHandle> ph = ProcessHandle.of(pid);
                if (ph.isEmpty() || !ph.get().isAlive()) {
                    System.out.println("[AdminHelper] parent " + pid + " gone — exiting");
                    System.exit(0);
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void redirectLog() {
        try {
            File log = new File(System.getProperty("java.io.tmpdir"), "evr-admin-helper.log");
            PrintStream ps = new PrintStream(new FileOutputStream(log, true));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception ignored) {}
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private static String safe(String s) { return s == null ? "" : s.replace('\n', ' ').replace('\t', ' '); }

    private static void deleteQuietly(Path p) {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
    }
}
