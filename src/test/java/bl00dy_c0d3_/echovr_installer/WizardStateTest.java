package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WizardStateTest {

    private WizardState state;

    @BeforeEach
    void setUp() {
        state = new WizardState();
    }

    @Test
    void testDefaultState() {
        assertNull(state.getUserType(), "userType should be null by default");
        assertEquals("", state.getInstallPath(), "installPath should be empty string by default");
    }

    @Test
    void testSetUserTypeOwner() {
        state.setUserType(WizardState.UserType.OWNER);
        assertEquals(WizardState.UserType.OWNER, state.getUserType());
    }

    @Test
    void testSetUserTypeNewPlayer() {
        state.setUserType(WizardState.UserType.NEW_PLAYER);
        assertEquals(WizardState.UserType.NEW_PLAYER, state.getUserType());
    }

    @Test
    void testSetAndGetInstallPath() {
        String path = "C:/EchoVR/ready-at-dawn-echo-arena";
        state.setInstallPath(path);
        assertEquals(path, state.getInstallPath());
    }

    @Test
    void testPathNormalization() {
        state.setInstallPath("C:\\EchoVR\\");
        assertEquals("C:/EchoVR", state.getInstallPath());
    }

    @Test
    void testToString() {
        state.setUserType(WizardState.UserType.OWNER);
        state.setInstallPath("C:/EchoVR");
        String result = state.toString();
        assertTrue(result.contains("OWNER"), "toString should contain userType");
        assertTrue(result.contains("C:/EchoVR"), "toString should contain installPath");
    }
}
