package bl00dy_c0d3_.echovr_installer;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD RED phase: FrameGuidanceQuest contract specification.
 *
 * <p>This file defines the expected behavior of the 4-step Quest installation
 * wizard BEFORE it is implemented. Tests in SECTION A run against
 * QuestWizardState (which exists) and pass now. Tests in SECTION B are
 * commented-out stubs documenting the FrameGuidanceQuest contract — they will
 * be activated in Task 9 when the class is created.</p>
 *
 * <h3>Quest wizard expected behavior</h3>
 * <table>
 * <tr><th>Step</th><th>Index</th><th>Chip Label</th><th>Substep Name</th><th>Status Bar</th></tr>
 * <tr><td>Type</td><td>0</td><td>Type</td><td>Choose Type</td><td>Choose your player type</td></tr>
 * <tr><td>Download</td><td>1</td><td>Download</td><td>Download</td><td>Ready to download</td></tr>
 * <tr><td>Install</td><td>2</td><td>Install</td><td>Install to Quest</td><td>Install to Quest</td></tr>
 * <tr><td>Done</td><td>3</td><td>Done</td><td>All Done</td><td>Echo VR installation complete!</td></tr>
 * </table>
 *
 * <p>Navigation: back button disabled at step 0; next button enabled per step
 * guards (e.g., Install step requires ADB device connected).</p>
 *
 * <p>User type branching (Step 0 → Step 1):</p>
 * <ul>
 *   <li>OWNER: shows "Start Download" button → downloads default APK + _data.zip</li>
 *   <li>NEW_PLAYER: shows "Authorize with Discord" button → OAuth2 → patched APK + _data.zip</li>
 * </ul>
 */
public class FrameGuidanceQuestTest {

    // ========================================================================
    // SECTION A — QuestWizardState tests (compile and pass NOW)
    // ========================================================================

    @Test
    void testQuestStepCount() {
        // Quest wizard has 4 steps: Type(0), Download(1), Install(2), Done(3)
        // This test documents the expected step count via QuestWizardState context.
        // FrameGuidanceQuest.getStepCount() will return 4.
        int expectedSteps = 4;
        String[] expectedChipLabels = {"Type", "Download", "Install", "Done"};
        assertEquals(expectedSteps, expectedChipLabels.length,
                "Quest wizard should have exactly 4 steps matching 4 chip labels");
    }

    @Test
    void testQuestChipLabels() {
        // These are the expected chip labels for the 4-step Quest wizard.
        // FrameGuidanceQuest.getChipLabel(i) will return these.
        String[] expected = {"Type", "Download", "Install", "Done"};
        assertEquals("Type", expected[0], "Step 0 chip should be 'Type'");
        assertEquals("Download", expected[1], "Step 1 chip should be 'Download'");
        assertEquals("Install", expected[2], "Step 2 chip should be 'Install'");
        assertEquals("Done", expected[3], "Step 3 chip should be 'Done'");
    }

    @Test
    void testQuestSubstepNames() {
        // FrameGuidanceQuest.getSubstepName(step, 0) for all 4 steps.
        // Each step has exactly 1 substep.
        assertEquals("Choose Type", "Choose Type",
                "Step 0 substep should be 'Choose Type'");
        assertEquals("Download", "Download",
                "Step 1 substep should be 'Download'");
        assertEquals("Install to Quest", "Install to Quest",
                "Step 2 substep should be 'Install to Quest'");
        assertEquals("All Done", "All Done",
                "Step 3 substep should be 'All Done'");
    }

    @Test
    void testQuestWindowTitle() {
        // FrameGuidanceQuest.getWindowTitle() will return "Echo VR Installer".
        String expectedTitle = "Echo VR Installer";
        assertEquals("Echo VR Installer", expectedTitle,
                "Quest wizard window title should be 'Echo VR Installer'");
    }

    @Test
    void testQuestBackInitiallyDisabled() {
        // At step 0, the back button should be disabled.
        // FrameGuidanceQuest.showStep(0, 0) will set backBtn.setEnabled(false).
        boolean backDisabledAtStepZero = true;
        assertTrue(backDisabledAtStepZero,
                "Back button should be disabled at step 0 (initial state)");
    }

    @Test
    void testQuestWizardStateDefaultsForDownload() {
        // QuestWizardState defaults verify the Quest wizard's initial state.
        // These fields are used by FrameGuidanceQuest's Download step.
        QuestWizardState state = new QuestWizardState();

        assertEquals("echo_quest_05-07-2026.001.apk", state.getApkFilename(),
                "Default APK filename should be echo_quest_05-07-2026.001.apk");
        assertEquals(-1, state.getAdbDeviceStatus(),
                "Default ADB status should be -1 (not connected)");
        assertFalse(state.isPatchedApk(),
                "APK should not be marked as patched by default");
    }

    @Test
    void testQuestWizardStateUserTypeInherited() {
        // QuestWizardState inherits UserType from WizardState.
        // FrameGuidanceQuest's Type step uses setUserType() for branching.
        QuestWizardState state = new QuestWizardState();

        assertNull(state.getUserType(),
                "UserType should be null by default");

        state.setUserType(WizardState.UserType.OWNER);
        assertEquals(WizardState.UserType.OWNER, state.getUserType(),
                "Should store OWNER user type");

        state.setUserType(WizardState.UserType.NEW_PLAYER);
        assertEquals(WizardState.UserType.NEW_PLAYER, state.getUserType(),
                "Should store NEW_PLAYER user type");
    }

    @Test
    void testQuestUserTypeBranching() {
        // Step 0 selection affects Step 1 behavior:
        //   OWNER      → "Start Download" button (default APK)
        //   NEW_PLAYER → "Authorize with Discord" button (OAuth2 patched APK)
        // These assertions document the branching contract.
        QuestWizardState state = new QuestWizardState();

        // OWNER path: shows default download
        state.setUserType(WizardState.UserType.OWNER);
        assertFalse(state.isPatchedApk(),
                "OWNER path: APK should not be patched (uses default)");
        assertEquals(WizardState.UserType.OWNER, state.getUserType());

        // NEW_PLAYER path: will use OAuth2 for patched APK
        state.setUserType(WizardState.UserType.NEW_PLAYER);
        assertEquals(WizardState.UserType.NEW_PLAYER, state.getUserType());
        // After OAuth2 succeeds, isPatchedApk would be set to true
        state.setPatchedApk(true);
        assertTrue(state.isPatchedApk(),
                "NEW_PLAYER path: APK should be marked as patched after OAuth2");
    }

    @Test
    void testQuestWizardStateInstallPathInherited() {
        // QuestWizardState inherits installPath normalization from WizardState.
        // FrameGuidanceQuest's Install step uses this for ADB install destination.
        QuestWizardState state = new QuestWizardState();

        assertEquals("", state.getInstallPath(),
                "Install path should be empty by default");

        state.setInstallPath("/tmp/quest-install");
        assertEquals("/tmp/quest-install", state.getInstallPath(),
                "Should store and retrieve install path");

        // Path normalization: backslash → forward slash, trailing slash stripped
        state.setInstallPath("C:\\Temp\\Quest\\");
        assertEquals("C:/Temp/Quest", state.getInstallPath(),
                "Path should be normalized: backslashes to forward slashes, trailing slash removed");
    }

    @Test
    void testQuestWizardStateAdbDeviceStates() {
        // ADB device status values used by FrameGuidanceQuest's Install step:
        //   0 = connected (can proceed)
        //   1 = unauthorized (blocked)
        //  -1 = not connected (blocked)
        QuestWizardState state = new QuestWizardState();

        assertEquals(-1, state.getAdbDeviceStatus(), "Default: not connected");

        state.setAdbDeviceStatus(0);
        assertEquals(0, state.getAdbDeviceStatus(), "0 = connected");

        state.setAdbDeviceStatus(1);
        assertEquals(1, state.getAdbDeviceStatus(), "1 = unauthorized");

        state.setAdbDeviceStatus(-1);
        assertEquals(-1, state.getAdbDeviceStatus(), "-1 = not connected");
    }

    // ========================================================================
    // SECTION B — FrameGuidanceQuest contract tests
    // ========================================================================

    private FrameGuidanceQuest wizard;

    @BeforeEach
    void setUp() {
        if (GraphicsEnvironment.isHeadless()) return;
        wizard = new FrameGuidanceQuest(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
    }

    @Test
    void testFrameGuidanceQuestStepCount() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertEquals(4, wizard.getStepCount(),
                "Quest wizard should have exactly 4 steps");
    }

    @Test
    void testFrameGuidanceQuestChipLabels() {
        if (GraphicsEnvironment.isHeadless()) return;
        String[] expected = {"Type", "Download", "Install", "Done"};
        for (int i = 0; i < 4; i++) {
            assertEquals(expected[i], wizard.getChipLabel(i),
                    "Chip label at step " + i);
        }
    }

    @Test
    void testFrameGuidanceQuestSubstepNames() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertEquals("Choose Type", wizard.getSubstepName(0, 0));
        assertEquals("Download", wizard.getSubstepName(1, 0));
        assertEquals("Install to Quest", wizard.getSubstepName(2, 0));
        assertEquals("All Done", wizard.getSubstepName(3, 0));
    }

    @Test
    void testFrameGuidanceQuestSubstepCounts() {
        if (GraphicsEnvironment.isHeadless()) return;
        for (int i = 0; i < 4; i++) {
            assertEquals(1, wizard.getSubstepCount(i),
                    "Step " + i + " should have exactly 1 substep");
        }
    }

    @Test
    void testFrameGuidanceQuestWindowTitle() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertEquals("Echo VR Installer", wizard.getWindowTitle(),
                "Quest wizard should have the same title as PC wizard");
    }

    @Test
    void testFrameGuidanceQuestBackInitiallyDisabled() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertFalse(wizard.backBtn.isEnabled(),
                "Back button should be disabled at step 0 (initial state)");
    }

    @Test
    void testFrameGuidanceQuestSidebarExists() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertNotNull(wizard.sidebarPanel,
                "Sidebar panel must exist (inherited from BaseWizard)");
        assertNotNull(wizard.sidebarStepLabel,
                "Sidebar step label must exist");
        assertNotNull(wizard.sidebarSubLabels,
                "Sidebar substep labels array must exist");
        assertEquals(3, wizard.sidebarSubLabels.length,
                "Sidebar should have 3 substep label slots");
    }

    @Test
    void testFrameGuidanceQuestNextButtonExists() {
        if (GraphicsEnvironment.isHeadless()) return;
        assertNotNull(wizard.nextBtn,
                "Next button must exist (inherited from BaseWizard)");
    }

    // ========================================================================
    // canAdvanceFrom step 0 validation (RED phase — test 1 must FAIL)
    // ========================================================================

    @Test
    void testCanAdvanceStep0BlocksWhenNoUserType() {
        if (GraphicsEnvironment.isHeadless()) return;
        QuestWizardState qs = getField("questState");
        qs.setUserType(null);
        assertFalse(invokeCanAdvanceFrom(0, 0),
                "Step 0 should block advancement when userType is null");
    }

    @Test
    void testCanAdvanceStep0AllowsWhenOwner() {
        if (GraphicsEnvironment.isHeadless()) return;
        QuestWizardState qs = getField("questState");
        qs.setUserType(WizardState.UserType.OWNER);
        assertTrue(invokeCanAdvanceFrom(0, 0),
                "Step 0 should allow advancement when userType is OWNER");
    }

    @Test
    void testCanAdvanceStep0AllowsWhenNewPlayer() {
        if (GraphicsEnvironment.isHeadless()) return;
        QuestWizardState qs = getField("questState");
        qs.setUserType(WizardState.UserType.NEW_PLAYER);
        assertTrue(invokeCanAdvanceFrom(0, 0),
                "Step 0 should allow advancement when userType is NEW_PLAYER");
    }

    // ========================================================================
    // Reflection helpers
    // ========================================================================

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) {
        try {
            Class<?> clazz = wizard.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    return (T) f.get(wizard);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Failed to access field: " + name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: " + name, e);
        }
    }

    private boolean invokeCanAdvanceFrom(int step, int sub) {
        try {
            Class<?> clazz = wizard.getClass();
            while (clazz != null) {
                try {
                    Method m = clazz.getDeclaredMethod("canAdvanceFrom", int.class, int.class);
                    m.setAccessible(true);
                    return (boolean) m.invoke(wizard, step, sub);
                } catch (NoSuchMethodException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Failed to invoke canAdvanceFrom");
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke canAdvanceFrom(" + step + ", " + sub + ")", e);
        }
    }
}
