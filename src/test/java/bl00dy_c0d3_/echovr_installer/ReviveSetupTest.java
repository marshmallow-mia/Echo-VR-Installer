package bl00dy_c0d3_.echovr_installer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ReviveSetupTest {

    private static final String EXE = "C:/EchoVR/ready-at-dawn-echo-arena/bin/win10/echovr.exe";

    private Path writeManifest(Path dir, String json) throws IOException {
        Path f = dir.resolve(ReviveSetup.VRMANIFEST);
        Files.writeString(f, json, StandardCharsets.UTF_8);
        return f;
    }

    private JsonArray readApps(Path dir) throws IOException {
        String content = Files.readString(dir.resolve(ReviveSetup.VRMANIFEST), StandardCharsets.UTF_8);
        return JsonParser.parseString(content).getAsJsonObject().getAsJsonArray("applications");
    }

    private long countEchoEntries(JsonArray apps) {
        long n = 0;
        for (var el : apps) {
            if (el.isJsonObject() && el.getAsJsonObject().has("app_key")
                && ReviveSetup.APP_KEY.equals(el.getAsJsonObject().get("app_key").getAsString())) {
                n++;
            }
        }
        return n;
    }

    // --- detectLibraryId ---

    @Test
    void detectsLibraryIdFromExistingEntry() {
        JsonArray apps = new JsonArray();
        JsonObject other = new JsonObject();
        other.addProperty("app_key", "revive.app.some-other-game");
        other.addProperty("arguments", "/app some-other-game /library 9988776655 \"x\" -nosymbollookup");
        apps.add(other);

        assertEquals("9988776655", ReviveSetup.detectLibraryId(apps));
    }

    @Test
    void detectLibraryIdIgnoresPlaceholderAndEmpty() {
        JsonArray apps = new JsonArray();
        JsonObject placeholder = new JsonObject();
        placeholder.addProperty("arguments", "/app x /library put-library-ID-here \"x\"");
        apps.add(placeholder);

        assertNull(ReviveSetup.detectLibraryId(apps));
        assertNull(ReviveSetup.detectLibraryId(new JsonArray()));
        assertNull(ReviveSetup.detectLibraryId(null));
    }

    // --- patchVrManifest ---

    @Test
    void addsEntryToPopulatedManifest(@TempDir Path dir) throws IOException {
        writeManifest(dir,
            "{\"applications\":[{\"app_key\":\"revive.app.other\","
                + "\"arguments\":\"/app other /library 12345 \\\"x\\\" -nosymbollookup\"}]}");

        ReviveSetup.VrManifestResult result = ReviveSetup.patchVrManifest(dir.toString(), EXE);

        assertEquals(ReviveSetup.VrManifestResult.ADDED, result);
        JsonArray apps = readApps(dir);
        assertEquals(2, apps.size());
        assertEquals(1, countEchoEntries(apps));

        // The added entry carries the detected library id and Revive injector binary.
        JsonObject echo = null;
        for (var el : apps) {
            if (ReviveSetup.APP_KEY.equals(el.getAsJsonObject().get("app_key").getAsString())) {
                echo = el.getAsJsonObject();
            }
        }
        assertNotNull(echo);
        assertTrue(echo.get("arguments").getAsString().contains("/library 12345"));
        assertEquals(ReviveSetup.REVIVE_INJECTOR, echo.get("binary_path_windows").getAsString());
        assertEquals(ReviveSetup.APP_ID,
            echo.getAsJsonObject("strings").getAsJsonObject("en_us").get("name").getAsString());
    }

    @Test
    void reapplyingIsIdempotentAndUpdates(@TempDir Path dir) throws IOException {
        writeManifest(dir,
            "{\"applications\":[{\"app_key\":\"revive.app.other\","
                + "\"arguments\":\"/app other /library 555 \\\"x\\\" -nosymbollookup\"}]}");

        assertEquals(ReviveSetup.VrManifestResult.ADDED, ReviveSetup.patchVrManifest(dir.toString(), EXE));
        assertEquals(ReviveSetup.VrManifestResult.UPDATED, ReviveSetup.patchVrManifest(dir.toString(), EXE));

        // No duplicate Echo entry on re-apply.
        assertEquals(1, countEchoEntries(readApps(dir)));
        assertEquals(2, readApps(dir).size());
    }

    @Test
    void emptyManifestWithoutLibraryIdReturnsFallback(@TempDir Path dir) throws IOException {
        writeManifest(dir, "{\"applications\":[]}");
        assertEquals(ReviveSetup.VrManifestResult.EMPTY_MANIFEST,
            ReviveSetup.patchVrManifest(dir.toString(), EXE));
    }

    @Test
    void missingManifestReturnsFallback(@TempDir Path dir) throws IOException {
        assertEquals(ReviveSetup.VrManifestResult.EMPTY_MANIFEST,
            ReviveSetup.patchVrManifest(dir.toString(), EXE));
    }

    // --- dashboard restore (guarded until URLs are wired) ---

    @Test
    void dashboardRestoreNotYetAvailable() {
        assertThrows(UnsupportedOperationException.class, ReviveSetup::restoreDashboardManifests);
    }
}
