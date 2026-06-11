package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

public class DesignRuleVerificationTest {

    @Test
    void testBaseWizardSidebarWidth() throws Exception {
        Field f = BaseWizard.class.getDeclaredField("SIDEBAR_W");
        f.setAccessible(true);
        int value = f.getInt(null);
        assertEquals(120, value,
                "Design rule: SIDEBAR_W must be 120 (sidebar inner width)");
    }

    @Test
    void testBaseWizardBoxBorder() throws Exception {
        Field f = BaseWizard.class.getDeclaredField("BOX_BORDER");
        f.setAccessible(true);
        Color c = (Color) f.get(null);
        assertNotNull(c, "BOX_BORDER must be present");
        assertEquals(50, c.getRed(), "BOX_BORDER red channel");
        assertEquals(50, c.getGreen(), "BOX_BORDER green channel");
        assertEquals(50, c.getBlue(), "BOX_BORDER blue channel");
        assertEquals(150, c.getAlpha(), "BOX_BORDER alpha channel");
    }

    @Test
    void testFrameGuidancePCStepCount() {
        if (GraphicsEnvironment.isHeadless()) return;
        FrameGuidancePC wizard = new FrameGuidancePC(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
        assertEquals(6, wizard.getStepCount(),
                "PC wizard must have exactly 6 steps: Type, Play, Path, Download, Patch, Done");
    }

    @Test
    void testFrameGuidanceQuestStepCount() {
        if (GraphicsEnvironment.isHeadless()) return;
        FrameGuidanceQuest wizard = new FrameGuidanceQuest(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
        assertEquals(4, wizard.getStepCount(),
                "Quest wizard must have exactly 4 steps: Type, Download, Install, Done");
    }

    @Test
    void testFramePCUpdateStepCount() {
        if (GraphicsEnvironment.isHeadless()) return;
        FramePCUpdate wizard = new FramePCUpdate(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
        assertEquals(3, wizard.getStepCount(),
                "PC Update wizard must have exactly 3 steps: Path, Update, Done");
    }

    @Test
    void testFrameGuidancePCChipLabels() {
        if (GraphicsEnvironment.isHeadless()) return;
        FrameGuidancePC wizard = new FrameGuidancePC(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
        String[] expected = {"Type", "Play", "Path", "Download", "Patch", "Done"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], wizard.getChipLabel(i),
                    "PC wizard chip label at step " + i);
        }
    }

    @Test
    void testFrameGuidanceQuestChipLabels() {
        if (GraphicsEnvironment.isHeadless()) return;
        FrameGuidanceQuest wizard = new FrameGuidanceQuest(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
        String[] expected = {"Type", "Download", "Install", "Done"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], wizard.getChipLabel(i),
                    "Quest wizard chip label at step " + i);
        }
    }

    @Test
    void testFramePCUpdateChipLabels() {
        if (GraphicsEnvironment.isHeadless()) return;
        FramePCUpdate wizard = new FramePCUpdate(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
        String[] expected = {"Path", "Update", "Done"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], wizard.getChipLabel(i),
                    "PC Update wizard chip label at step " + i);
        }
    }

    @Test
    void testFramePCUpdateSubstepCounts() {
        if (GraphicsEnvironment.isHeadless()) return;
        FramePCUpdate wizard = new FramePCUpdate(null) {
            @Override
            public void setVisible(boolean b) {
                super.setVisible(false);
            }
        };
        assertEquals(1, wizard.getSubstepCount(0),
                "PC Update step 0 (Path) must have 1 substep");
        assertEquals(2, wizard.getSubstepCount(1),
                "PC Update step 1 (Update) must have 2 substeps");
        assertEquals(1, wizard.getSubstepCount(2),
                "PC Update step 2 (Done) must have 1 substep");
    }

    @Test
    void testPCWizardStateExtendsWizardState() {
        assertTrue(WizardState.class.isAssignableFrom(PCWizardState.class),
                "PCWizardState must extend WizardState");
        assertEquals(WizardState.class, PCWizardState.class.getSuperclass(),
                "PCWizardState direct parent must be WizardState");
    }

    @Test
    void testQuestWizardStateExtendsWizardState() {
        assertTrue(WizardState.class.isAssignableFrom(QuestWizardState.class),
                "QuestWizardState must extend WizardState");
        assertEquals(WizardState.class, QuestWizardState.class.getSuperclass(),
                "QuestWizardState direct parent must be WizardState");
    }

    @Test
    void testAllAbstractMethodsImplementedInPC() throws Exception {
        assertAbstractMethodsImplemented(BaseWizard.class, FrameGuidancePC.class, "FrameGuidancePC");
    }

    @Test
    void testAllAbstractMethodsImplementedInQuest() throws Exception {
        assertAbstractMethodsImplemented(BaseWizard.class, FrameGuidanceQuest.class, "FrameGuidanceQuest");
    }

    @Test
    void testAllAbstractMethodsImplementedInPCUpdate() throws Exception {
        assertAbstractMethodsImplemented(BaseWizard.class, FramePCUpdate.class, "FramePCUpdate");
    }

    private static void assertAbstractMethodsImplemented(
            Class<?> abstractClass, Class<?> concreteClass, String className) throws Exception {

        for (Method m : abstractClass.getDeclaredMethods()) {
            if (!Modifier.isAbstract(m.getModifiers())) continue;

            Method override = null;
            try {
                override = concreteClass.getDeclaredMethod(m.getName(), m.getParameterTypes());
            } catch (NoSuchMethodException e) {
                Class<?> c = concreteClass;
                while (c != null && c != Object.class) {
                    try {
                        override = c.getDeclaredMethod(m.getName(), m.getParameterTypes());
                        break;
                    } catch (NoSuchMethodException ignored) {
                        c = c.getSuperclass();
                    }
                }
            }

            assertNotNull(override,
                    className + " must implement abstract method: " + m.getName()
                            + "(" + paramTypesToString(m.getParameterTypes()) + ")");
            assertFalse(Modifier.isAbstract(override.getModifiers()),
                    className + " method " + m.getName() + " must not be abstract");
        }
    }

    private static String paramTypesToString(Class<?>[] types) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(types[i].getSimpleName());
        }
        return sb.toString();
    }
}
