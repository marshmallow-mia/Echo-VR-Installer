package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestWizardStateTest {

    private QuestWizardState state;

    @BeforeEach
    void setUp() {
        state = new QuestWizardState();
    }

    @Test
    void testDefaultApkFilename() {
        assertEquals("echo_quest_27-06-2026.001.apk", state.getApkFilename());
    }

    @Test
    void testDefaultAdbDeviceStatus() {
        assertEquals(-1, state.getAdbDeviceStatus());
    }

    @Test
    void testDefaultIsPatchedApk() {
        assertFalse(state.isPatchedApk());
    }

    @Test
    void testSetAndGetApkFilename() {
        state.setApkFilename("custom.apk");
        assertEquals("custom.apk", state.getApkFilename());
    }

    @Test
    void testSetAndGetAdbDeviceStatus() {
        state.setAdbDeviceStatus(3);
        assertEquals(3, state.getAdbDeviceStatus());
    }

    @Test
    void testSetAndGetIsPatchedApk() {
        state.setPatchedApk(true);
        assertTrue(state.isPatchedApk());

        state.setPatchedApk(false);
        assertFalse(state.isPatchedApk());
    }

    @Test
    void testPathNormalizationInherited() {
        state.setInstallPath("C:\\EchoVR\\");
        assertEquals("C:/EchoVR", state.getInstallPath());
    }
}
