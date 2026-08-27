package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless tests for the Quest update gate. Everything here is device-free: the decision
 * table and the output parsers are pure, which is why they were factored out of the adb
 * plumbing in the first place.
 */
public class QuestUpdateServiceTest {

    private static final String BASE_SHA = "0a7fa5f9cfc173013e152a75fac2ded7ca4f66b8d8530f598c0c2530b5cf0973";
    private static final String PATCHED_SHA = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String OLD_BASE_SHA = "2222222222222222222222222222222222222222222222222222222222222222";

    private static UpdateManifest manifest() {
        return UpdateManifest.parse(
                "# BASE_APK: echo_quest_27-08-2026.001.apk " + BASE_SHA + "\n"
              + "# Target: /sdcard/Android/media/com.readyatdawn.r15\n"
              + "add  asset_patches/a/b  " + "c".repeat(64) + "\n",
                "https://files.echovr.de/updates/quest/update.manifest");
    }

    private static QuestUpdateService.Marker marker(String baseSha, String installedSha, boolean patched) {
        QuestUpdateService.Marker m = new QuestUpdateService.Marker();
        m.baseApk = "echo_quest_27-08-2026.001.apk";
        m.baseSha256 = baseSha;
        m.installedSha256 = installedSha;
        m.patched = patched;
        return m;
    }

    // --- decision table ---

    @Test
    void testRow1_notInstalled() {
        QuestUpdateService.Status s = QuestUpdateService.decide(manifest(), null, false, null);
        assertEquals(QuestUpdateService.VersionCheck.NOT_INSTALLED, s.result);
        assertFalse(s.isOk());
    }

    @Test
    void testRow2_markerMatchesStockInstall() {
        QuestUpdateService.Status s = QuestUpdateService.decide(
                manifest(), marker(BASE_SHA, BASE_SHA, false), true, BASE_SHA);
        assertTrue(s.isOk(), "a stock install matching the manifest base may update");
        assertFalse(s.shouldSelfHealMarker, "an existing marker needs no back-fill");
    }

    @Test
    void testRow2_markerMatchesPatchedInstall() {
        // The whole reason the marker exists: a repacked Discord APK can never hash to the
        // manifest's BASE_APK, but it is still built from that base.
        QuestUpdateService.Status s = QuestUpdateService.decide(
                manifest(), marker(BASE_SHA, PATCHED_SHA, true), true, PATCHED_SHA);
        assertTrue(s.isOk(), "a patched install of the current base may update");
    }

    @Test
    void testRow3_apkReplacedSinceInstall() {
        QuestUpdateService.Status s = QuestUpdateService.decide(
                manifest(), marker(BASE_SHA, PATCHED_SHA, true), true, "d".repeat(64));
        assertEquals(QuestUpdateService.VersionCheck.MISMATCH, s.result,
                "an APK swapped in behind the installer must not be updated");
        assertTrue(s.detail.contains("replaced"));
    }

    @Test
    void testRow4_markerFromOlderBase() {
        QuestUpdateService.Status s = QuestUpdateService.decide(
                manifest(), marker(OLD_BASE_SHA, OLD_BASE_SHA, false), true, OLD_BASE_SHA);
        assertEquals(QuestUpdateService.VersionCheck.MISMATCH, s.result);
        assertTrue(s.detail.contains("older"), "the user is told their version is out of date");
    }

    @Test
    void testRow5_noMarkerButStockBaseSelfHeals() {
        QuestUpdateService.Status s = QuestUpdateService.decide(manifest(), null, true, BASE_SHA);
        assertTrue(s.isOk(), "a stock base APK is recognisable without a marker");
        assertTrue(s.shouldSelfHealMarker, "the missing marker should be back-filled");
    }

    @Test
    void testRow6_noMarkerAndUnknownApk() {
        QuestUpdateService.Status s = QuestUpdateService.decide(manifest(), null, true, PATCHED_SHA);
        assertEquals(QuestUpdateService.VersionCheck.MISMATCH, s.result,
                "a patched APK with no provenance cannot be updated");
    }

    @Test
    void testRow6_noMarkerAndUnknownHash() {
        QuestUpdateService.Status s = QuestUpdateService.decide(manifest(), null, true, null);
        assertEquals(QuestUpdateService.VersionCheck.MISMATCH, s.result,
                "an unhashable APK with no marker cannot be updated");
    }

    @Test
    void testRow8_manifestError() {
        QuestUpdateService.Status s = QuestUpdateService.decide(null, null, true, BASE_SHA);
        assertEquals(QuestUpdateService.VersionCheck.MANIFEST_ERROR, s.result);
    }

    @Test
    void testHashComparisonIsCaseInsensitive() {
        QuestUpdateService.Status s = QuestUpdateService.decide(
                manifest(), marker(BASE_SHA.toUpperCase(), BASE_SHA.toUpperCase(), false),
                true, BASE_SHA);
        assertTrue(s.isOk(), "hex hashes must compare case-insensitively");
    }

    @Test
    void testManifestWithoutBaseApkHeaderDoesNotGate() {
        UpdateManifest noHeader = UpdateManifest.parse(
                "# Target: /sdcard/Android/media/com.readyatdawn.r15\ndel  a\n",
                "https://files.echovr.de/updates/quest/update.manifest");
        QuestUpdateService.Status s = QuestUpdateService.decide(noHeader, null, true, PATCHED_SHA);
        assertTrue(s.isOk(), "with no BASE_APK header there is nothing to gate on");
    }

    // --- marker ---

    @Test
    void testMarkerRoundTrip() {
        QuestUpdateService.Marker original = marker(BASE_SHA, PATCHED_SHA, true);
        original.installedAt = "2026-08-27T21:04:00Z";
        original.installerVersion = "Echo VR Installer v0.9.4b-006";

        QuestUpdateService.Marker parsed = QuestUpdateService.Marker.parse(original.serialize());

        assertNotNull(parsed);
        assertEquals(original.baseApk, parsed.baseApk);
        assertEquals(original.baseSha256, parsed.baseSha256);
        assertEquals(original.installedSha256, parsed.installedSha256);
        assertTrue(parsed.patched);
        assertEquals(original.installedAt, parsed.installedAt);
        assertEquals(original.installerVersion, parsed.installerVersion);
    }

    @Test
    void testMarkerParseToleratesNoise() {
        // adb/cat noise and Helpers' "Process exited with code N" line must not break parsing.
        String text = "cat: /sdcard/...: No such file\n"
                    + "Process exited with code 0\n"
                    + "# a comment\n"
                    + "base_sha256=" + BASE_SHA + "\n"
                    + "garbage without an equals sign\n"
                    + "installed_sha256=" + PATCHED_SHA + "\n"
                    + "unknown_key=whatever\n";
        QuestUpdateService.Marker m = QuestUpdateService.Marker.parse(text);

        assertNotNull(m);
        assertEquals(BASE_SHA, m.baseSha256);
        assertEquals(PATCHED_SHA, m.installedSha256);
    }

    @Test
    void testMarkerParseReturnsNullWhenAbsent() {
        assertNull(QuestUpdateService.Marker.parse(null));
        assertNull(QuestUpdateService.Marker.parse(""));
        assertNull(QuestUpdateService.Marker.parse("cat: no such file\n"),
                "a missing marker file must not parse into an empty marker");
    }

    // --- device output parsing ---

    @Test
    void testFirstHashToken() {
        assertEquals(BASE_SHA,
                QuestUpdateService.firstHashToken(BASE_SHA + "  /data/app/x/base.apk"));
        assertEquals(BASE_SHA,
                QuestUpdateService.firstHashToken(BASE_SHA.toUpperCase() + "  /data/app/x/base.apk"),
                "hex is normalised to lowercase");
        assertNull(QuestUpdateService.firstHashToken("sha256sum: not found"));
        assertNull(QuestUpdateService.firstHashToken("/system/bin/sh: sha256sum: inaccessible"));
        assertNull(QuestUpdateService.firstHashToken(""));
        assertNull(QuestUpdateService.firstHashToken(null));
    }

    @Test
    void testParseHashListing() {
        String output = "aaaa: skipped noise\n"
                      + BASE_SHA + "  asset_patches/489bb35d53ca50e9/2dfe2e7610506f03\n"
                      + PATCHED_SHA + "  asset_patches/e2efe7289d5985b8/3fa6d25bc24a7b01\n"
                      + "sha256sum: asset_patches/missing: No such file or directory\n";

        Map<String, String> hashes = QuestUpdateService.parseHashListing(output);

        assertEquals(2, hashes.size(), "only well-formed hash lines are kept");
        assertEquals(BASE_SHA, hashes.get("asset_patches/489bb35d53ca50e9/2dfe2e7610506f03"));
        assertEquals(PATCHED_SHA, hashes.get("asset_patches/e2efe7289d5985b8/3fa6d25bc24a7b01"));
        assertNull(hashes.get("asset_patches/missing"), "missing files yield no entry, so they get pushed");
    }

    @Test
    void testParseHashListingHandlesEmptyOutput() {
        assertTrue(QuestUpdateService.parseHashListing(null).isEmpty());
        assertTrue(QuestUpdateService.parseHashListing("").isEmpty());
    }

    @Test
    void testMarkerPathIsUnderTheManifestTargetRoot() {
        assertEquals("/sdcard/Android/media/com.readyatdawn.r15/.echo_installer_version",
                QuestUpdateService.MARKER_PATH,
                "the marker must sit at the target root so Android wipes it on uninstall");
    }
}
