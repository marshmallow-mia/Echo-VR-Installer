package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless tests for the shared manifest parser. The path and target validation here is
 * security-load-bearing: Quest manifest paths end up interpolated into {@code adb shell}
 * scripts, including {@code rm -rf}.
 */
public class UpdateManifestTest {

    private static final String QUEST_URL = "https://files.echovr.de/updates/quest/update.manifest";

    private static final String PC_URL = "https://files.echovr.de/updates/update.manifest";

    /** Verbatim shape of the live Quest manifest, double space after "Target:" included. */
    private static final String QUEST_MANIFEST =
            "# Echo VR Quest update manifest -- 2026-08-27\n"
          + "# Base URL: https://files.echovr.de/updates/quest\n"
          + "# BASE_APK: echo_quest_27-08-2026.001.apk 0a7fa5f9cfc173013e152a75fac2ded7ca4f66b8d8530f598c0c2530b5cf0973\n"
          + "# Target:  /sdcard/Android/media/com.readyatdawn.r15\n"
          + "\n"
          + "add  asset_patches/489bb35d53ca50e9/2dfe2e7610506f03  d5f216f7df4194345187abd6ffb91de4c463e983fbac1e08173177302db2cf37\n"
          + "add  asset_patches/489bb35d53ca50e9/3fa6d25bc24a7b01  afe542b412b9570d8483969f4fa1b6205f51ca5aac294ccbd3266be2855caf75\n"
          + "add  asset_patches/e2efe7289d5985b8/2dfe2e7610506f03  d7786295dac4db88440f9c65764785801678f81c7c88668844387229fd103092\n";

    @Test
    void testParsesQuestHeaders() {
        UpdateManifest m = UpdateManifest.parse(QUEST_MANIFEST, QUEST_URL);

        assertEquals("echo_quest_27-08-2026.001.apk", m.baseApkName(),
                "BASE_APK header must yield the APK filename");
        assertEquals("0a7fa5f9cfc173013e152a75fac2ded7ca4f66b8d8530f598c0c2530b5cf0973", m.baseApkSha(),
                "BASE_APK header must yield the APK hash");
        assertEquals("/sdcard/Android/media/com.readyatdawn.r15", m.targetRoot(),
                "Target header must parse despite the double space after the colon");
    }

    @Test
    void testParsesQuestEntries() {
        UpdateManifest m = UpdateManifest.parse(QUEST_MANIFEST, QUEST_URL);

        List<UpdateManifest.Entry> adds = m.adds();
        assertEquals(3, adds.size(), "manifest has three add entries");
        assertTrue(m.dels().isEmpty(), "manifest has no del entries");
        assertEquals("asset_patches/489bb35d53ca50e9/2dfe2e7610506f03", adds.get(0).path);
        assertEquals("d5f216f7df4194345187abd6ffb91de4c463e983fbac1e08173177302db2cf37", adds.get(0).sha256);
        assertTrue(adds.get(0).isAdd());
        assertFalse(adds.get(0).isDel());
    }

    @Test
    void testBaseUrlAndUrlFor() {
        UpdateManifest m = UpdateManifest.parse(QUEST_MANIFEST, QUEST_URL);

        assertEquals("https://files.echovr.de/updates/quest", m.baseUrl(),
                "base URL is the manifest URL up to the last slash");
        assertEquals("https://files.echovr.de/updates/quest/asset_patches/489bb35d53ca50e9/2dfe2e7610506f03",
                m.urlFor(m.adds().get(0)));
    }

    @Test
    void testPcManifestHasNoQuestHeaders() {
        String pc = "# Echo VR update manifest\n"
                  + "add  dbgcore.dll  a7aab2291aefd918b634966f20d922b4268be34c46f56ac684223f0cd58c40af\n"
                  + "del  plugins/old.dll\n";
        UpdateManifest m = UpdateManifest.parse(pc, "https://files.echovr.de/updates/update.manifest");

        assertNull(m.baseApkName(), "PC manifest carries no BASE_APK header");
        assertNull(m.baseApkSha());
        assertNull(m.targetRoot(), "PC manifest carries no Target header");
        assertEquals(1, m.adds().size());
        assertEquals(1, m.dels().size());
        assertNull(m.dels().get(0).sha256, "del entries carry no hash");
    }

    @Test
    void testRejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse("add  ../../etc/passwd  " + "a".repeat(64) + "\n", QUEST_URL),
                "relative traversal must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse("add  /etc/passwd  " + "a".repeat(64) + "\n", QUEST_URL),
                "absolute paths must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse("del  a/../../b\n", QUEST_URL),
                "traversal in the middle of a path must be rejected");
    }

    @Test
    void testAcceptsPlusInPaths() {
        // The live PC manifest ships the MinGW runtime trio, one of which carries '+'.
        // Rejecting it aborted the whole update: "Unsafe path in manifest: libstdc++-6.dll".
        String hash = "a".repeat(64);
        UpdateManifest m = UpdateManifest.parse(
                "add  libgcc_s_seh-1.dll  " + hash + "\n"
              + "add  libstdc++-6.dll  " + hash + "\n"
              + "add  libwinpthread-1.dll  " + hash + "\n", PC_URL);

        List<UpdateManifest.Entry> adds = m.adds();
        assertEquals(3, adds.size(), "all three MinGW runtime entries must parse");
        assertEquals("libstdc++-6.dll", adds.get(1).path,
                "'+' must survive parsing verbatim -- it is what the download URL is built from");
        assertEquals("https://files.echovr.de/updates/libstdc++-6.dll", m.urlFor(adds.get(1)),
                "'+' is literal in a URL path segment, so the URL needs no escaping");
    }

    @Test
    void testStillRejectsLeadingPlus() {
        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse("del  +weird\n", QUEST_URL),
                "'+' is only allowed after the first character");
    }

    @Test
    void testRejectsShellMetacharactersInPaths() {
        // These would otherwise reach the device's sh via the batched sha256sum script.
        String[] hostile = {
            "a;rm -rf /sdcard",
            "a`id`b",
            "a$(id)b",
            "a|b",
            "a&b",
            "a*b",
            "a\"b",
            "a'b",
            "..\\windows",
        };
        for (String path : hostile) {
            assertThrows(IllegalArgumentException.class,
                    () -> UpdateManifest.parse("del  " + path + "\n", QUEST_URL),
                    "must reject hostile path: " + path);
        }
    }

    @Test
    void testRejectsUnsafeTargetRoot() {
        String manifest = "# Target: /sdcard\ndel  a\n";
        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse(manifest, QUEST_URL),
                "a target root outside the app media dir must be rejected -- it reaches rm -rf");

        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse("# Target: /sdcard/Android/media/com.evil.app\ndel  a\n", QUEST_URL),
                "a target root for another package must be rejected");
    }

    @Test
    void testRejectsAddWithoutHash() {
        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse("add  dbgcore.dll\n", QUEST_URL),
                "an add entry without a SHA-256 must be rejected");
    }

    @Test
    void testRejectsUnknownAction() {
        assertThrows(IllegalArgumentException.class,
                () -> UpdateManifest.parse("move  a  b\n", QUEST_URL));
    }

    @Test
    void testIgnoresBlankLinesAndComments() {
        String manifest = "\n   \n# just a comment\n#\nadd  a.bin  " + "b".repeat(64) + "\n\n";
        UpdateManifest m = UpdateManifest.parse(manifest, QUEST_URL);
        assertEquals(1, m.entries().size());
    }
}
