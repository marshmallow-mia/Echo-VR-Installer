# OAuth2 Discord Auth Flow — Implementation Plan

## Problem

The installer opens a raw Discord login page in a JavaFX WebView. Cookies/session don't persist across restarts because `com.sun.webkit.network.CookieManager.setStoragePath()` doesn't exist. Users must manually log in, navigate Discord, click a reaction button, copy a URL, and paste it back into the installer.

## Solution

Replace the manual Discord login + reaction-click flow with a **Discord OAuth2 authorization flow**. The installer opens Discord's OAuth2 consent screen, intercepts the callback with an authorization code, sends it to the existing bot server, which exchanges it for a token, verifies guild membership, generates the patch file, and returns a direct download URL.

**This eliminates the cookie persistence bug entirely** — no session to persist. The flow is stateless from the installer's perspective.

**Guild enforcement**: The server checks that the user is a member of the **Echo VR Patcher** server (`guild_id: 1193648519647608902`, invite `https://discord.gg/bMpsva6fmA`). Users who haven't joined get a clear message with the invite link.

---

## Architecture

```
┌── Java Installer ──────────────────────────────────────────────┐
│                                                                 │
│  WebView → https://discord.com/api/oauth2/authorize?            │
│    client_id=XXX&redirect_uri=echovr-installer://oauth2/cb     │
│    &response_type=code&scope=identify+guilds                    │
│                                                                 │
│  User logs in → clicks Authorize → Discord redirects            │
│                                                                 │
│  locationProperty() listener intercepts the redirect URL:       │
│    echovr-installer://oauth2/cb?code=XXXXXXXXX                  │
│                                                                 │
│  Extract ?code= from URL. POST to server endpoint.              │
│                                                                 │
│  Receive { patchUrl } → download file to install dir            │
└─────────────────────────────────────────────────────────────────┘
         │                            ▲
         │ POST /api/exchange         │ { patchUrl: "https://..." }
         │ { code, type }             │
         ▼                            │
┌── nginx reverse proxy ──────────────────────────────────────────┐
│  location /api/ { proxy_pass http://127.0.0.1:8765; }           │
│  location /dlls/ { alias /var/www/EchoClientHosting/dlls/; }    │
│  location /apks/ { alias /var/www/EchoClientHosting/apks/; }    │
└─────────────────────────────────────────────────────────────────┘
         │                            ▲
         ▼                            │
┌── Python Bot Process (new HTTP thread) ─────────────────────────┐
│                                                                 │
│  POST /api/exchange:                                             │
│  1. Receive { code, type }                                       │
│  2. Exchange code → Discord access token (POST discord.com)      │
│  3. Use token → GET /users/@me/guilds → verify guild membership │
│  4. Get user's Discord ID                                        │
│  5. Run existing create_dll() or create_apk() logic              │
│  6. Copy output file to /var/www/EchoClientHosting/{dlls,apks}/ │
│  7. Return { patchUrl: "https://files.echovr.de/..." }          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 1: Server-Side Changes (Python Bot on files.echo)

### 1.1 Dependencies

Add to the bot environment:
```
aiohttp  # async HTTP server that won't block the bot's asyncio loop
```

### 1.2 New HTTP Server Module

Create `/root/EchoSignUp/oauth2_server.py`:

```python
import aiohttp
from aiohttp import web
import discord
import os

HTTP_PORT = 8765
CLIENT_ID = os.getenv("DISCORD_CLIENT_ID")
CLIENT_SECRET = os.getenv("DISCORD_CLIENT_SECRET")
GUILD_ID = os.getenv("DISCORD_GUILD_ID", "1193648519647608902")  # Echo VR Patcher
REDIRECT_URI = "echovr-installer://oauth2/cb"

async def handle_exchange(request):
    """POST /api/exchange - OAuth2 code exchange + file generation"""
    try:
        body = await request.json()
        code = body.get("code")
        file_type = body.get("type", "dll")  # "dll" or "apk"
        
        if not code:
            return web.json_response({"error": "Missing code"}, status=400)
        
        # Step 1: Exchange code for access token
        async with aiohttp.ClientSession() as session:
            token_data = {
                "client_id": CLIENT_ID,
                "client_secret": CLIENT_SECRET,
                "grant_type": "authorization_code",
                "code": code,
                "redirect_uri": REDIRECT_URI,
            }
            async with session.post(
                "https://discord.com/api/oauth2/token",
                data=token_data,
                headers={"Content-Type": "application/x-www-form-urlencoded"}
            ) as resp:
                if resp.status != 200:
                    return web.json_response({"error": "Invalid code"}, status=400)
                token_json = await resp.json()
                access_token = token_json["access_token"]
        
        # Step 2: Get user identity and verify guild membership
        async with aiohttp.ClientSession() as session:
            async with session.get(
                "https://discord.com/api/users/@me/guilds",
                headers={"Authorization": f"Bearer {access_token}"}
            ) as resp:
                if resp.status != 200:
                    return web.json_response({"error": "Failed to verify identity"}, status=400)
                guilds = await resp.json()
                
                # Check if user is in the target guild
                guild_ids = [g["id"] for g in guilds]
                if str(GUILD_ID) not in guild_ids:
                    return web.json_response({
                        "error": "not_in_guild",
                        "message": "You must join the Echo VR Patcher server first",
                        "invite": "https://discord.gg/bMpsva6fmA"
                    }, status=403)
        
        # Step 3: Get user info
        async with aiohttp.ClientSession() as session:
            async with session.get(
                "https://discord.com/api/users/@me",
                headers={"Authorization": f"Bearer {access_token}"}
            ) as resp:
                user_info = await resp.json()
                discord_user_id = user_info["id"]
        
        # Step 4: Generate file (reuse existing bot logic via shared functions)
        # TODO: Call into EchoSignUp.py's create_dll/create_apk
        # Since those are async discord.py functions, they need the bot instance
        # Pass discord_user_id and file_type
        # Copy output to web directory
        
        # Step 5: Return download URL
        # patch_url = f"https://files.echovr.de/{file_type}s/{timestamp}/pnsovr.dll"
        return web.json_response({"patchUrl": patch_url})
        
    except Exception as e:
        return web.json_response({"error": str(e)}, status=500)

def start_server():
    app = web.Application()
    app.router.add_post("/api/exchange", handle_exchange)
    web.run_app(app, host="127.0.0.1", port=HTTP_PORT)
```

### 1.3 Bot Integration

Modify `EchoSignUp.py` to:

1. Start the HTTP server in a background thread when the bot starts (in `on_ready()`)
2. Create shared async functions that the HTTP endpoint can call:
   - `generate_for_user(discord_user_id, file_type)` → calls `create_dll()` or `create_apk()` → copies file to web dir → returns URL
3. Add a `temp_state["patch_url"]` to pass the generated URL back from `create_dll()`/`create_apk()`

### 1.4 File Delivery Changes

**For DLL**: Currently `create_dll()` saves to `temp/pnsovr.dll` and sends via Discord DM. Add:
```python
# After patch_dll() saves the file:
timestamp = datetime.now().strftime("%Y-%m-%d-%H-%M-%S-%f")[:-3]
dll_dir = f'/var/www/EchoClientHosting/dlls/{timestamp}'
os.makedirs(dll_dir, exist_ok=True)
shutil.copy(f'temp/pnsovr.dll', f'{dll_dir}/pnsovr.dll')
patch_url = f'https://files.echovr.de/dlls/{timestamp}/pnsovr.dll'
```

**For APK**: Already copies to web dir and generates URL. Just need to return the URL programmatically.

### 1.5 nginx Configuration

On the server, add to the Apache2/nginx config:

```nginx
# Reverse proxy for OAuth2 exchange endpoint
location /api/ {
    proxy_pass http://127.0.0.1:8765;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}

# Serve generated DLL files (similar to existing APK serving)
location /dlls/ {
    alias /var/www/EchoClientHosting/dlls/;
}
```

### 1.6 Discord Developer Portal

- Use the existing bot application (same app as the Echo VR Patcher bot)
- Add OAuth2 redirect URL: `echovr-installer://oauth2/cb`
- Note the Client ID (will be embedded in the installer)
- Note the Client Secret (will be set as env var on the server)
- The bot must have **Server Members Intent** enabled (already enabled per `EchoSignUp.py` line 34: `intents.members = True`)
- Required OAuth2 scopes: `identify` + `guilds` (to verify guild membership and get user info)

---

## Part 2: Installer-Side Changes (Java/JavaFX)

### 2.1 New Class: `OAuth2CallbackServer` (optional, for localhost fallback)

Create `OAuth2CallbackServer.java` — a temporary lightweight HTTP server on localhost:
- Listens on a random available port
- Has a single endpoint `/callback`
- When Discord redirects with the auth code, catches it
- Shuts down after receiving the code
- Returns the auth code to the caller

**Only needed if the custom scheme approach doesn't work in JavaFX WebView.**

### 2.2 New Class: `DiscordOAuth2Flow.java`

Create `DiscordOAuth2Flow.java` — handles the OAuth2 flow:

```java
public class DiscordOAuth2Flow {
    private static final String CLIENT_ID = "1326594571584409650"; // from bot code
    private static final String REDIRECT_URI = "echovr-installer://oauth2/cb";
    private static final String AUTH_URL = 
        "https://discord.com/api/oauth2/authorize?" +
        "client_id=" + CLIENT_ID +
        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, UTF_8) +
        "&response_type=code" +
        "&scope=identify%20guilds";
    private static final String SERVER_URL = "https://files.echovr.de";
    
    private DiscordWebView webView;
    private String authCode;
    
    /**
     * Opens the OAuth2 consent screen in the WebView.
     * Intercepts the redirect to capture the authorization code.
     */
    public CompletableFuture<String> startOAuth2(String fileType) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        webView = new DiscordWebView();
        
        // Listen for redirects in the WebView
        webView.getWebEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && newUrl.startsWith("echovr-installer://")) {
                // Extract the authorization code from the redirect URL
                String code = extractCode(newUrl);
                if (code != null && !code.isEmpty()) {
                    authCode = code;
                    webView.close();
                    
                    // Exchange the code on the server
                    exchangeCode(code, fileType, future);
                }
            }
        });
        
        webView.navigateTo(AUTH_URL);
        return future;
    }
    
    private String extractCode(String url) {
        // Parse ?code=XXX from the redirect URL
        Pattern pattern = Pattern.compile("[?&]code=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private void exchangeCode(String code, String fileType, 
                              CompletableFuture<String> future) {
        HttpClient client = HttpClient.newHttpClient();
        String json = String.format("{\"code\":\"%s\",\"type\":\"%s\"}", 
                                     code, fileType);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_URL + "/api/exchange"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(response -> {
                // Parse { "patchUrl": "..." }
                // Complete the future with the URL
                JsonObject jsonResponse = ...; // parse
                String patchUrl = jsonResponse.get("patchUrl");
                future.complete(patchUrl);
            })
            .exceptionally(e -> {
                future.completeExceptionally(e);
                return null;
            });
    }
}
```

### 2.3 Modify `DiscordWebView.java`

Add support for navigation interception:

1. Add `getWebEngine()` public method (already exists)
2. Add ability to close the window after OAuth2 button
3. Keep the existing sidebar with instructions updated for OAuth2 flow:
   - Step 1: Log in to Discord
   - Step 2: Click "Authorize" to grant access
   - Step 3: Wait for file generation

### 2.4 Modify `FrameGuidance.java`

Replace the "Open Discord" button handler:

**Current**: Creates `DiscordWebView` and navigates to `https://discord.com/login`
**New**: Creates `DiscordOAuth2Flow` with appropriate file type (dll/apk), waits for the patch URL, then downloads the file.

Flow:
1. User clicks "Start Patching" → opens OAuth2 consent screen
2. User authorizes → code exchange happens in background
3. Loading indicator shows "Generating your patch file..."
4. Server returns URL → installer downloads and extracts to install dir
5. Success → proceed to next step

### 2.5 Add HTTP Client for File Download

Download the patch file from the returned URL using `java.net.http.HttpClient`:

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(patchUrl))
    .GET()
    .build();

// Stream download with progress reporting
client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(destPath))
    .thenAccept(response -> {
        // File downloaded successfully
    });
```

---

## Part 3: Error Handling

| Scenario | Server Response | Installer Action |
|---|---|---|
| Invalid/expired auth code | 400 `{"error": "Invalid code"}` | Show "Authorization failed. Please try again." |
| User not in guild | 403 `{"error": "not_in_guild", "invite": "https://discord.gg/bMpsva6fmA"}` | Show "Please join the Echo VR Patcher server first" with invite link `https://discord.gg/bMpsva6fmA` |
| File generation failed | 500 `{"error": "..."}` | Show "File generation failed. Contact support." |
| Network error (timeout) | — | Show "Connection to server failed. Check your internet." with retry button |
| User closes WebView | — | Cancel flow, return to previous step |

---

## Part 4: Files to Create/Modify

### Server (SSH into files.echo)

| File | Action |
|---|---|
| `/root/EchoSignUp/oauth2_server.py` | **Create** — aiohttp HTTP server with `/api/exchange` endpoint |
| `/root/EchoSignUp/EchoSignUp.py` | **Modify** — Add HTTP server startup in `on_ready()`, add shared generate function |
| `/etc/nginx/sites-available/...` | **Modify** — Add reverse proxy + /dlls/ location |
| `.env` or bot service file | **Modify** — Add `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_GUILD_ID` |

### Installer (this repo)

| File | Action |
|---|---|
| `src/main/java/.../DiscordOAuth2Flow.java` | **Create** — OAuth2 authorize + code exchange orchestration |
| `src/main/java/.../DiscordWebView.java` | **Modify** — Update sidebar instructions for OAuth2, improve navigation interception |
| `src/main/java/.../FrameGuidance.java` | **Modify** — Wire OAuth2 flow instead of raw Discord login |
| `build.gradle` | **Maybe** — No changes needed (Java 17 has HttpClient built-in) |

---

## Task Dependency Graph

```
Server: oauth2_server.py ───┐
                            ├──> Server: Modify EchoSignUp.py ──> Server: nginx config
                            │                                      │
Server: DLL file delivery ──┘                                      │
                                                                    │
Discord Developer Portal (independent) ────────────────────────────┤
                                                                    │
                                                                    ▼
                                        Server: verify all + restart bot
                                                  │
              ┌───────────────────────────────────┘
              ▼
  Installer: DiscordOAuth2Flow.java ──> Installer: DiscordWebView.java sidebar
              │                                              │
              └──────────────────────────────────────────────┘
                        │
                        ▼
              Installer: FrameGuidance.java wiring
                        │
                        ▼
              End-to-end test
                        │
                        ▼
              Cleanup DiscordNavigator references
```

**Parallel waves:**
- **Wave 1** (fully parallel): Tasks 1, 3, 5 (no interdependencies)
- **Wave 2**: Task 2 (depends on task 1), Task 4 (depends on task 2)
- **Wave 3** (parallel): Tasks 6, 7 (no interdependencies, no server dependency for coding)
- **Wave 4**: Task 8 (depends on tasks 6, 7)
- **Wave 5**: Task 9 (depends on task 8 and server side)
- **Wave 6**: Task 10 (depends on task 8)

## Category + Skills Recommendations

| Task | Category | Skills | Rationale |
|------|----------|--------|-----------|
| 1. oauth2_server.py | `quick` | `[]` | Single-file Python HTTP server, straightforward |
| 2. EchoSignUp.py integration | `deep` | `[]` | Requires understanding existing async bot code and modifying it without breaking reaction flow |
| 3. DLL file delivery | `quick` | `[]` | Simple file copy + timestamp logic |
| 4. nginx config | `quick` | `[]` | Standard reverse proxy config |
| 5. Discord Developer Portal | — | — | Manual configuration step |
| 6. DiscordOAuth2Flow.java | `unspecified-high` | `[]` | New Java class with HTTP client, URL parsing, CompletableFuture orchestration |
| 7. DiscordWebView.java sidebar | `quick` | `[]` | Trivial label text change |
| 8. FrameGuidance.java wiring | `unspecified-high` | `[]` | Modifying existing wizard flow, UI state management, error handling dialogs |
| 9. End-to-end test | `deep` | `[]` | Requires both server and installer running, manual OAuth2 flow verification |
| 10. Cleanup | `quick` | `[]` | Remove unused imports/references |

## Implementation Order with QA Scenarios

### Task 1 — Server: Create `oauth2_server.py`

**Action**: Create `/root/EchoSignUp/oauth2_server.py` with aiohttp HTTP server, POST `/api/exchange` endpoint.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `ssh files.echo "python3 -c 'from oauth2_server import start_server; print(\"import OK\")'"` | SSH into server, run import check | Prints "import OK" |
| `ssh files.echo "timeout 3 python3 -c 'import oauth2_server; oauth2_server.start_server()' 2>&1"` | Start the server for 3 seconds | Server starts, prints "Running on http://127.0.0.1:8765" |
| `curl -X POST http://127.0.0.1:8765/api/exchange -H "Content-Type: application/json" -d '{"code":"invalid","type":"dll"}'` | With server running, send bad code | Returns 400 `{"error": "Invalid code"}` |

### Task 2 — Server: Modify `EchoSignUp.py`

**Action**: Start HTTP server in `on_ready()` background thread. Add `generate_for_user()` shared function.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `ssh files.echo "grep -n 'oauth2_server' /root/EchoSignUp/EchoSignUp.py"` | Check bot imports the server | Shows import line(s) |
| `ssh files.echo "grep -n 'start_server' /root/EchoSignUp/EchoSignUp.py"` | Check server starts in on_ready | Shows start_server() call inside on_ready |
| `ssh files.echo "grep -n 'generate_for_user' /root/EchoSignUp/EchoSignUp.py"` | Check shared function exists | Shows function definition |
| `ssh files.echo "python3 -c 'from EchoSignUp import generate_for_user; print(\"OK\")'"` | Verify module loads without error | Prints "OK" |
| Restart bot + `curl http://127.0.0.1:8765/api/exchange ...` | After bot restart, hit endpoint | Returns HTTP response (not connection refused) |

### Task 3 — Server: Add DLL file delivery

**Action**: In `create_dll()`, after `patch_dll()` saves the file, copy to `/var/www/EchoClientHosting/dlls/{timestamp}/pnsovr.dll`.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `ssh files.echo "grep -n 'EchoClientHosting/dlls' /root/EchoSignUp/EchoSignUp.py"` | Check DLL copy code exists | Shows line with dll directory path |
| `ssh files.echo "grep -n 'shutil.copy' /root/EchoSignUp/EchoSignUp.py"` | Check copy call exists | Shows shutil.copy() for DLL |
| `ssh files.echo "ls -la /var/www/EchoClientHosting/dlls/ 2>&1"` | Check dlls directory exists | Shows directory listing (may be empty) |
| Manual: React to patcher message with a test user | Trigger normal flow | DLL file appears in `/var/www/EchoClientHosting/dlls/` with timestamp dir |

### Task 4 — Server: Configure nginx reverse proxy

**Action**: Add `/api/` reverse proxy to 127.0.0.1:8765 and `/dlls/` static alias.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `ssh files.echo "nginx -t 2>&1"` | Test nginx config | "syntax is ok" / "test is successful" |
| `ssh files.echo "systemctl reload nginx 2>&1"` | Reload nginx | No error (exit 0) |
| `curl -X POST https://files.echovr.de/api/exchange ...` | From external machine (or local), hit API endpoint | Returns JSON response (not 502/404) |
| `curl -I https://files.echovr.de/dlls/` | Verify /dlls/ is served | Returns 200 or 403 (if dir listing disabled) — not 404 |

### Task 5 — Server: Register OAuth2 redirect URI in Discord Developer Portal

**Action**: Go to Discord Developer Portal → OAuth2 → Add redirect: `echovr-installer://oauth2/cb`. Note Client ID, set Client Secret as env var.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| Browser | Open https://discord.com/developers/applications → select app → OAuth2 → General | Redirect URL `echovr-installer://oauth2/cb` listed |
| `ssh files.echo "echo \$DISCORD_CLIENT_ID"` | Check env vars | Shows non-empty numeric client ID |
| `ssh files.echo "echo \$DISCORD_CLIENT_SECRET"` | Check env vars (should not be echoed in logs) | Shows non-empty string |
| `ssh files.echo "echo \$DISCORD_GUILD_ID"` | Check guild ID | Shows numeric guild ID |

### Task 6 — Installer: Create `DiscordOAuth2Flow.java`

**Action**: Create new Java class in `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordOAuth2Flow.java` with OAuth2 URL construction, redirect interception, code extraction, HTTP POST to server, JSON response parsing.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `lsp_diagnostics` on new file | Run diagnostics | Zero errors |
| `./gradlew compileJava` | Build | BUILD SUCCESSFUL |
| Unit test (manual): Inspect URL | Check `AUTH_URL` constant | Opens to Discord OAuth2 authorize page with correct client_id, redirect_uri, scope |
| Unit test: `extractCode()` | Pass URL with `?code=abc123` | Returns `"abc123"` |
| Unit test: `extractCode()` | Pass URL without code | Returns `null` |

### Task 7 — Installer: Modify `DiscordWebView.java` sidebar

**Action**: Update sidebar labels (step1-step4) for OAuth2 flow.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `lsp_diagnostics` on `DiscordWebView.java` | Run diagnostics | Zero errors |
| `./gradlew compileJava` | Build | BUILD SUCCESSFUL |

### Task 8 — Installer: Wire OAuth2 into `FrameGuidance.java`

**Action**: Replace `new DiscordWebView().navigateTo("https://discord.com/login")` with `DiscordOAuth2Flow` flow. Handle patch URL response → download file → proceed to next step.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `lsp_diagnostics` on `FrameGuidance.java` | Run diagnostics | Zero errors |
| `./gradlew compileJava` | Build | BUILD SUCCESSFUL |

### Task 9 — Test: Full end-to-end flow

**Action**: Run the installer with both server and bot running. Go through the full OAuth2 + file generation + download flow.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| Run installer | Launch app, click Install → select user type → get to patcher step | Discord OAuth2 consent screen opens in WebView |
| Log into Discord in WebView | Enter credentials, click Authorize | WebView closes, loading indicator shows |
| Check server logs | `ssh files.echo "tail -20 /root/EchoSignUp/logfile.log"` | Log shows OAuth2 exchange + file generation |
| Check file appears | `ssh files.echo "ls /var/www/EchoClientHosting/dlls/*/pnsovr.dll"` | File exists with timestamp |
| Check installer | Wait for download to complete | File downloaded to install directory |
| Repeat | Close and reopen installer, go through flow again | Works consistently (stateless — no cookie dependency) |

### Task 10 — Cleanup: Remove unused references

**Action**: Check if `DiscordNavigator` is still referenced anywhere. Remove if no longer needed.

**QA**:
| Tool | Steps | Expected |
|------|-------|----------|
| `grep -r "DiscordNavigator" src/` | Search for references | No references found (or only in test file) |
| `./gradlew compileJava` | Build | BUILD SUCCESSFUL |
