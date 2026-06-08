package bl00dy_c0d3_.echovr_installer;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * Handles Discord OAuth2 login flow using the system's default web browser.
 * Opens Discord's OAuth2 consent screen in the browser, captures the
 * callback via a localhost HTTP server, exchanges the authorization code
 * for a patch URL via the server API.
 */
public class DiscordOAuth2Flow {

    private static final String CLIENT_ID = "1326594571584409650";
    private static final String SERVER_URL = "https://files.echovr.de";
    private static final int CALLBACK_TIMEOUT_SECONDS = 300;

    private final String fileType;

    public DiscordOAuth2Flow(String fileType) {
        this.fileType = fileType;
    }

    /**
     * Starts the OAuth2 flow. Opens the system browser for Discord authorization,
     * captures the callback, exchanges the code, and returns the patch download URL.
     */
    public CompletableFuture<String> start(Consumer<String> onStatus) {
        CompletableFuture<String> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                if (onStatus != null) SwingUtilities.invokeLater(() -> onStatus.accept("Discord authorization opened in your browser."));

                // Start a temporary HTTP server on localhost for the OAuth2 callback.
                // Port 53124 must match what's registered in Discord Developer Portal.
                // Bind to 127.0.0.1 explicitly (not all interfaces) — a loopback-only listener is
                // exempt from Windows Firewall filtering, so this avoids the "allow access on public
                // and private networks" prompt. The redirect already targets 127.0.0.1.
                ServerSocket serverSocket = new ServerSocket(53124, 10, InetAddress.getByName("127.0.0.1"));
                String redirectUri = "http://127.0.0.1:53124/callback";

                String authUrl = "https://discord.com/api/oauth2/authorize?"
                        + "client_id=" + CLIENT_ID
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
                        + "&response_type=code"
                        + "&scope=identify%20guilds";

                // Open the system's default web browser for Discord authorization
                boolean browserOpened = openBrowser(authUrl);
                if (!browserOpened) {
                    future.completeExceptionally(new OAuth2Exception("no_browser",
                            "Could not open your browser automatically.\n"
                            + "Please open this URL manually:\n" + authUrl));
                    serverSocket.close();
                    return;
                }

                // Wait for the callback from Discord (blocking)
                String authCode = waitForCallback(serverSocket);
                serverSocket.close();

                if (authCode == null) {
                    future.completeExceptionally(new RuntimeException("OAuth2 authorization cancelled or timed out"));
                    return;
                }

                if (onStatus != null) SwingUtilities.invokeLater(() -> onStatus.accept("Generating your patch file..."));

                // Exchange the authorization code for a patch URL
                String patchUrl = exchangeCode(authCode);
                if (patchUrl != null) {
                    future.complete(patchUrl);
                } else {
                    future.completeExceptionally(new RuntimeException("Failed to get patch URL from server"));
                }

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, "OAuth2-Login-Thread").start();

        return future;
    }

    /**
     * Waits for the OAuth2 callback HTTP request on the temporary server.
     * Extracts the authorization code from the redirect URL.
     */
    private String waitForCallback(ServerSocket serverSocket) {
        try {
            serverSocket.setSoTimeout(CALLBACK_TIMEOUT_SECONDS * 1000);

            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream()));
                 OutputStream outputStream = clientSocket.getOutputStream()) {

                // Read the request line: GET /callback?code=XXXXX HTTP/1.1
                String requestLine = reader.readLine();
                if (requestLine == null) {
                    System.err.println("OAuth2: null request line");
                    return null;
                }

                // Consume remaining HTTP headers
                String headerLine;
                while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                    // intentionally empty — just consuming headers
                }

                // Parse the path from the request line
                String[] parts = requestLine.split(" ");
                String path = parts.length > 1 ? parts[1] : "";

                // Extract the authorization code from the query string
                String code = null;
                int codeIdx = path.indexOf("?code=");
                if (codeIdx >= 0) {
                    code = path.substring(codeIdx + 6);
                    int ampIdx = code.indexOf("&");
                    if (ampIdx > 0) {
                        code = code.substring(0, ampIdx);
                    }
                }

                // Send response to the browser
                String responseBody = "<html><body><h1>Authorization complete! You can close this window.</h1></body></html>";
                String httpResponse = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/html; charset=UTF-8\r\n"
                        + "Content-Length: " + responseBody.length() + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + responseBody;
                outputStream.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                return code;
            }

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("OAuth2 callback: timed out waiting for redirect");
            return null;
        } catch (Exception e) {
            System.err.println("OAuth2 callback error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Exchanges the OAuth2 authorization code for a patch URL via the server API.
     */
    private String exchangeCode(String authCode) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String json = "{\"code\":\"" + authCode + "\",\"type\":\"" + fileType + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/api/exchange"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        System.out.println("OAuth2: Server returned status=" + response.statusCode() + " body=" + body);

        if (response.statusCode() == 403 && body.contains("not_in_guild")) {
            throw new OAuth2Exception("not_in_guild",
                    "You must join the Echo VR Patcher server first.\nhttps://discord.gg/bMpsva6fmA");
        }

        if (response.statusCode() == 409 && body.contains("busy")) {
            throw new OAuth2Exception("busy",
                    "Bot is busy generating another file. Please try again in 30 seconds.");
        }

        if (response.statusCode() != 200) {
            throw new OAuth2Exception("server_error",
                    "Server returned error: " + body);
        }

        // Parse {"patchUrl": "..."} from the response
        return extractPatchUrl(body);
    }

    /**
     * Extracts the patch URL from the JSON response using simple string parsing.
     * Response format: {"patchUrl": "https://files.echovr.de/..."}
     */
    private static String extractPatchUrl(String json) {
        // Handle both "patchUrl":"..." and "patchUrl": "..." (with space)
        String keyWithSpace = "\"patchUrl\": \"";
        String keyNoSpace = "\"patchUrl\":\"";
        int start = json.indexOf(keyWithSpace);
        int keyLen;
        if (start >= 0) {
            keyLen = keyWithSpace.length();
        } else {
            start = json.indexOf(keyNoSpace);
            if (start < 0) return null;
            keyLen = keyNoSpace.length();
        }
        start += keyLen;
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    /**
     * Opens the given URL in the system browser with multiple fallback strategies.
     * Tries Desktop.browse first, then xdg-open (Linux), then CMD (Windows).
     */
    private static boolean openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    return true;
                }
            }
        } catch (Exception ignored) {}

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                return true;
            } else if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                return true;
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Custom exception for OAuth2 flow errors with a machine-readable error code.
     */
    public static class OAuth2Exception extends Exception {
        private final String errorCode;

        public OAuth2Exception(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
