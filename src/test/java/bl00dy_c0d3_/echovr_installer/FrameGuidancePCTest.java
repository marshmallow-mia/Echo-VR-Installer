package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.jupiter.api.Assertions.*;

public class FrameGuidancePCTest {

    private FrameGuidancePC guidance;

    @BeforeEach
    void setUp() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        guidance = new FrameGuidancePC(null) {
            @Override
            public void setVisible(boolean b) {
                // Prevent the window from ever appearing — tests run UI-invisibly
                super.setVisible(false);
            }
        };
    }

    @Test
    void testStepCount() {
        if (GraphicsEnvironment.isHeadless()) return;
        for (int step = 0; step < 6; step++) {
            int count = invokeGetSubstepCount(step);
            assertTrue(count >= 1 && count <= 2,
                    "Step " + step + " should have 1 or 2 substeps, got " + count);
        }
    }

    @Test
    void testChipLabels() {
        if (GraphicsEnvironment.isHeadless()) return;

        assertNotNull(guidance);
        JLabel sidebarStepLabel = getField("sidebarStepLabel");
        assertEquals("Step 1", sidebarStepLabel.getText());

        String[] expectedStepLabels = {"Step 1", "Step 2", "Step 3", "Step 4", "Step 5", "Step 6"};
        for (int i = 0; i < 6; i++) {
            invokeShowStep(i, 0);
            pause(50);
            assertEquals(expectedStepLabels[i], sidebarStepLabel.getText());
        }
    }

    @Test
    void testSubstepCounts() {
        if (GraphicsEnvironment.isHeadless()) return;

        assertEquals(1, invokeGetSubstepCount(0));
        assertEquals(1, invokeGetSubstepCount(1));
        assertEquals(1, invokeGetSubstepCount(2));
        assertEquals(1, invokeGetSubstepCount(3));
        assertEquals(1, invokeGetSubstepCount(5));

        assertEquals(1, invokeGetSubstepCount(4));

        PCWizardState ws = getField("wizardState");
        ws.setPlayStyle(PCWizardState.PlayStyle.STEAMVR);
        assertEquals(2, invokeGetSubstepCount(4));
    }

    @Test
    void testSubstepNames() {
        if (GraphicsEnvironment.isHeadless()) return;

        assertEquals("Choose Type", invokeGetSubstepName(0, 0));
        assertEquals("How do you play?", invokeGetSubstepName(1, 0));
        assertEquals("Choose Path", invokeGetSubstepName(2, 0));
        assertEquals("Download", invokeGetSubstepName(3, 0));
        assertEquals("All Done", invokeGetSubstepName(5, 0));
    }

    @Test
    void testSubstepNamesStep4() {
        if (GraphicsEnvironment.isHeadless()) return;

        PCWizardState ws = getField("wizardState");

        assertEquals("Optional Patches", invokeGetSubstepName(4, 0));

        ws.setUserType(WizardState.UserType.NEW_PLAYER);
        assertEquals("Authorize & Patch", invokeGetSubstepName(4, 0));

        ws.setUserType(WizardState.UserType.OWNER);
        assertEquals("Optional Patches", invokeGetSubstepName(4, 0));

        assertEquals("Steam Patch", invokeGetSubstepName(4, 1));
    }

    @Test
    void testWindowTitle() {
        if (GraphicsEnvironment.isHeadless()) return;

        String title = guidance.getTitle();
        assertNotNull(title);
        assertTrue(title.contains("Echo VR Installer"),
                "Expected 'Echo VR Installer', got: " + title);
    }

    @Test
    void testSidebarExists() {
        if (GraphicsEnvironment.isHeadless()) return;

        JPanel sidebar = getField("sidebarPanel");
        assertNotNull(sidebar);

        JLabel[] subLabels = getField("sidebarSubLabels");
        assertNotNull(subLabels);
        assertEquals(3, subLabels.length);
    }

    @Test
    void testBackInitiallyDisabled() {
        if (GraphicsEnvironment.isHeadless()) return;

        SpecialButton backBtn = getField("backBtn");
        assertNotNull(backBtn);
        assertFalse(backBtn.isEnabled());
    }

    @Test
    void testNextButtonInitiallyEnabled() {
        if (GraphicsEnvironment.isHeadless()) return;

        SpecialButton nextBtn = getField("nextBtn");
        assertNotNull(nextBtn);
        assertTrue(nextBtn.isEnabled());
    }

    @Test
    void testNextButtonDisabledAtLastStep() {
        if (GraphicsEnvironment.isHeadless()) return;

        SpecialButton nextBtn = getField("nextBtn");
        invokeShowStep(5, 0);
        pause(50);

        assertFalse(nextBtn.isEnabled());
        assertEquals("Finish", getButtonText(nextBtn));
    }

    @Test
    void testStatusBarText() {
        if (GraphicsEnvironment.isHeadless()) return;

        JLabel dlProgressLabel = getField("dlProgressLabel");
        assertEquals("Choose your player type", dlProgressLabel.getText());

        invokeShowStep(1, 0);
        pause(50);
        assertEquals("How do you launch Echo VR?", dlProgressLabel.getText());

        invokeShowStep(2, 0);
        pause(50);
        assertEquals("Choose your Echo VR install path", dlProgressLabel.getText());

        invokeShowStep(3, 0);
        pause(50);
        assertEquals("Ready to download", dlProgressLabel.getText());

        invokeShowStep(5, 0);
        pause(50);
        assertEquals("Echo VR installation complete!", dlProgressLabel.getText());
    }

    @Test
    void testStatusBarTextStep4Owner() {
        if (GraphicsEnvironment.isHeadless()) return;

        JLabel dlProgressLabel = getField("dlProgressLabel");
        PCWizardState ws = getField("wizardState");
        ws.setUserType(WizardState.UserType.OWNER);

        invokeShowStep(4, 0);
        pause(50);
        assertEquals("Apply optional patches", dlProgressLabel.getText());

        ws.setPlayStyle(PCWizardState.PlayStyle.STEAMVR);
        invokeShowStep(4, 1);
        pause(50);
        assertEquals("Apply Steam patch for Revive compatibility", dlProgressLabel.getText());
    }

    @Test
    void testStatusBarTextStep4NewPlayer() {
        if (GraphicsEnvironment.isHeadless()) return;

        JLabel dlProgressLabel = getField("dlProgressLabel");
        PCWizardState ws = getField("wizardState");
        ws.setUserType(WizardState.UserType.NEW_PLAYER);

        invokeShowStep(4, 0);
        pause(50);
        assertEquals("Authorize with Discord to generate your patch", dlProgressLabel.getText());
    }

    @Test
    void testConfirmAbortNoDownload() {
        if (GraphicsEnvironment.isHeadless()) return;

        boolean result = invokeConfirmAbortDownload();
        assertTrue(result);
    }

    @Test
    void testStep3IndicatorShowsCheckWhenEchoExists() {
        if (GraphicsEnvironment.isHeadless()) return;
        Path tempDir = null;
        try {
            try {
                tempDir = Files.createTempDirectory("echovr-test");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp directory", e);
            }
            File exeDir = new File(tempDir.toFile(), "ready-at-dawn-echo-arena/bin/win10");
            exeDir.mkdirs();
            try {
                new File(exeDir, "echovr.exe").createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create echovr.exe", e);
            }

            PCWizardState ws = getField("wizardState");
            ws.setInstallPath(tempDir.toString());

            invokeBuildStep3();

            // Field doesn't exist yet → getField throws → test fails (RED)
            JLabel pathIndicator = getField("pathIndicator");
            assertEquals("\u2713", pathIndicator.getText());
        } finally {
            if (tempDir != null) {
                try {
                    Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file); return FileVisitResult.CONTINUE;
                        }
                        @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir); return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException ignored) {}
            }
        }
    }

    @Test
    void testStep3IndicatorShowsCrossWhenEchoAbsent() {
        if (GraphicsEnvironment.isHeadless()) return;
        Path tempDir = null;
        try {
            try {
                tempDir = Files.createTempDirectory("echovr-test");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp directory", e);
            }

            PCWizardState ws = getField("wizardState");
            ws.setInstallPath(tempDir.toString());

            invokeBuildStep3();

            // Field doesn't exist yet → getField throws → test fails (RED)
            JLabel pathIndicator = getField("pathIndicator");
            assertEquals("\u2717", pathIndicator.getText());
        } finally {
            if (tempDir != null) {
                try {
                    Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file); return FileVisitResult.CONTINUE;
                        }
                        @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir); return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) {
        try {
            Class<?> clazz = guidance.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    return (T) f.get(guidance);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Failed to access field: " + name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: " + name, e);
        }
    }

    private int invokeGetSubstepCount(int step) {
        try {
            Method m = FrameGuidancePC.class.getDeclaredMethod("getSubstepCount", int.class);
            m.setAccessible(true);
            return (int) m.invoke(guidance, step);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getSubstepCount(" + step + ")", e);
        }
    }

    private String invokeGetSubstepName(int step, int substep) {
        try {
            Method m = FrameGuidancePC.class.getDeclaredMethod("getSubstepName", int.class, int.class);
            m.setAccessible(true);
            return (String) m.invoke(guidance, step, substep);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getSubstepName(" + step + ", " + substep + ")", e);
        }
    }

    private void invokeShowStep(int step, int substep) {
        try {
            Method m = FrameGuidancePC.class.getDeclaredMethod("showStep", int.class, int.class);
            m.setAccessible(true);
            m.invoke(guidance, step, substep);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke showStep(" + step + ", " + substep + ")", e);
        }
    }

    private boolean invokeConfirmAbortDownload() {
        try {
            Class<?> clazz = guidance.getClass();
            while (clazz != null) {
                try {
                    Method m = clazz.getDeclaredMethod("confirmAbortDownload");
                    m.setAccessible(true);
                    return (boolean) m.invoke(guidance);
                } catch (NoSuchMethodException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Failed to invoke confirmAbortDownload");
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke confirmAbortDownload", e);
        }
    }

    private void invokeBuildStep3() {
        try {
            Method m = FrameGuidancePC.class.getDeclaredMethod("buildStep3", int.class);
            m.setAccessible(true);
            JPanel contentPanel = getField("contentPanel");
            m.invoke(guidance, contentPanel.getWidth());
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke buildStep3", e);
        }
    }

    private String getButtonText(SpecialButton button) {
        for (Component comp : button.getComponents()) {
            if (comp instanceof JLabel) {
                String text = ((JLabel) comp).getText();
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    private static void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
