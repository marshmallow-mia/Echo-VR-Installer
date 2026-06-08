package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PCWizardStateTest {

    private PCWizardState state;

    @BeforeEach
    void setUp() {
        state = new PCWizardState();
    }

    @Test
    void testDefaultPlayStyleIsNull() {
        assertNull(state.getPlayStyle(), "playStyle should be null by default");
    }

    @Test
    void testSetAndGetPlayStyle() {
        state.setPlayStyle(PCWizardState.PlayStyle.STEAMVR);
        assertEquals(PCWizardState.PlayStyle.STEAMVR, state.getPlayStyle());

        state.setPlayStyle(PCWizardState.PlayStyle.META_LINK);
        assertEquals(PCWizardState.PlayStyle.META_LINK, state.getPlayStyle());
    }

    @Test
    void testGetBinPath() {
        state.setInstallPath("C:/EchoVR");
        assertEquals("C:/EchoVR/ready-at-dawn-echo-arena/bin/win10", state.getBinPath());
    }

    @Test
    void testPathNormalizationInherited() {
        state.setInstallPath("C:\\EchoVR\\");
        assertEquals("C:/EchoVR", state.getInstallPath());
    }
}
