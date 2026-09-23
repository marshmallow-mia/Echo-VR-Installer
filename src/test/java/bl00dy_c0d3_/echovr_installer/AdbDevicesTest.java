package bl00dy_c0d3_.echovr_installer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless tests for the {@code adb devices -l} parser and the target-selection heuristic.
 * Device-free by construction: both are pure functions of the command's text, which is why
 * they were factored out of the adb plumbing in the first place.
 */
public class AdbDevicesTest {

    private static final String HEADER = "List of devices attached\n";

    private static AdbDevices.Device one(String body) {
        List<AdbDevices.Device> devices = AdbDevices.parse(HEADER + body);
        assertEquals(1, devices.size(), "expected exactly one parsed device");
        return devices.get(0);
    }

    // --- parser ---

    @Test
    void headerOnlyYieldsNoDevices() {
        assertTrue(AdbDevices.parse(HEADER).isEmpty());
        assertTrue(AdbDevices.parse("").isEmpty());
        assertTrue(AdbDevices.parse(null).isEmpty());
    }

    @Test
    void daemonBannersAreIgnored() {
        List<AdbDevices.Device> devices = AdbDevices.parse(
                "* daemon not running; starting now at tcp:5037\n"
              + "* daemon started successfully\n"
              + HEADER
              + "1WMHH8150XX07M\tdevice\n");
        assertEquals(1, devices.size());
        assertEquals("1WMHH8150XX07M", devices.get(0).serial());
    }

    @Test
    void longFormPropsAreAllCaptured() {
        AdbDevices.Device d = one("1WMHH8150XX07M       device usb:1-2 product:hollywood "
                + "model:Quest_3 device:eureka transport_id:2\n");
        assertEquals("1WMHH8150XX07M", d.serial());
        assertEquals("device", d.state());
        assertEquals("1-2", d.props().get("usb"));
        assertEquals("hollywood", d.props().get("product"));
        assertEquals("Quest_3", d.props().get("model"));
        assertEquals("eureka", d.props().get("device"));
        assertEquals("2", d.props().get("transport_id"));
        assertTrue(d.authorized());
        assertFalse(d.emulator());
        assertTrue(d.questLike());
    }

    @Test
    void shortFormFromOldAdbHasNoProps() {
        AdbDevices.Device d = one("1WMHH8150XX07M\tdevice\n");
        assertEquals("device", d.state());
        assertTrue(d.props().isEmpty());
        assertNull(d.model());
        assertFalse(d.questLike());
    }

    @Test
    void nonDeviceStatesAreParsed() {
        assertEquals("unauthorized", one("ABC123\tunauthorized\n").state());
        assertEquals("offline", one("ABC123\toffline\n").state());
        assertEquals("recovery", one("ABC123\trecovery\n").state());
        assertEquals("sideload", one("ABC123\tsideload\n").state());
        assertFalse(one("ABC123\toffline\n").authorized());
    }

    @Test
    void emulatorIsRecognised() {
        AdbDevices.Device d = one("emulator-5554   device product:sdk_gphone64 model:sdk_gphone64_arm64\n");
        assertTrue(d.emulator());
        assertTrue(d.authorized());
        assertFalse(d.questLike());
    }

    /** Multi-word state with a trailing URL must not pollute props. */
    @Test
    void noPermissionsStateStaysOneState() {
        AdbDevices.Device d = one("0123456789ABCDEF\tno permissions (user in plugdev group); "
                + "see [http://developer.android.com/tools/device.html]\n");
        assertEquals("0123456789ABCDEF", d.serial());
        assertTrue(d.state().startsWith("no permissions"));
        assertTrue(d.state().endsWith("[http://developer.android.com/tools/device.html]"));
        assertTrue(d.props().isEmpty(), "the URL must not be read as a prop: " + d.props());
    }

    @Test
    void crlfAndBlankLinesAndSpacesParseIdentically() {
        List<AdbDevices.Device> crlf = AdbDevices.parse(
                "List of devices attached\r\n1WMHH8150XX07M\tdevice model:Quest_3\r\n\r\n");
        List<AdbDevices.Device> lf = AdbDevices.parse(
                "List of devices attached\n1WMHH8150XX07M     device model:Quest_3\n\n");
        assertEquals(1, crlf.size());
        assertEquals(1, lf.size());
        assertEquals(lf.get(0).serial(), crlf.get(0).serial());
        assertEquals(lf.get(0).state(), crlf.get(0).state());
        assertEquals(lf.get(0).props(), crlf.get(0).props());
    }

    // --- selection ---

    private static AdbDevices.Selection selectionOf(String body) {
        return AdbDevices.select(AdbDevices.parse(HEADER + body));
    }

    @Test
    void singleAuthorizedDeviceIsReady() {
        AdbDevices.Selection s = selectionOf("1WMHH8150XX07M\tdevice model:Quest_3\n");
        assertEquals(AdbDevices.Status.READY, s.status());
        assertEquals(0, s.statusCode());
        assertEquals("1WMHH8150XX07M", s.serial());
    }

    /** An unrecognised headset must still be picked when it is the only candidate. */
    @Test
    void singleAuthorizedDeviceNeedsNoQuestHeuristic() {
        AdbDevices.Selection s = selectionOf("XYZ999\tdevice model:Future_Headset_9\n");
        assertEquals(AdbDevices.Status.READY, s.status());
        assertEquals("XYZ999", s.serial());
    }

    @Test
    void offlineSiblingDoesNotBlockSelection() {
        AdbDevices.Selection s = selectionOf(
                "1WMHH8150XX07M\tdevice model:Quest_3\nDEADBEEF\toffline\n");
        assertEquals(AdbDevices.Status.READY, s.status());
        assertEquals("1WMHH8150XX07M", s.serial());
    }

    /** The reported failure: a Quest alongside a running emulator. */
    @Test
    void emulatorIsSkippedInFavourOfTheRealDevice() {
        AdbDevices.Selection s = selectionOf(
                "emulator-5554   device product:sdk_gphone64 model:sdk_gphone64_arm64\n"
              + "1WMHH8150XX07M  device product:hollywood model:Quest_3 device:eureka\n");
        assertEquals(AdbDevices.Status.READY, s.status());
        assertEquals("1WMHH8150XX07M", s.serial());
    }

    @Test
    void questIsPreferredOverAnAuthorizedPhone() {
        AdbDevices.Selection s = selectionOf(
                "PHONE123        device product:raven model:Pixel_6_Pro\n"
              + "1WMHH8150XX07M  device product:hollywood model:Quest_3 device:eureka\n");
        assertEquals(AdbDevices.Status.READY, s.status());
        assertEquals("1WMHH8150XX07M", s.serial());
        assertTrue(s.reason().contains("Pixel 6 Pro"), "rejected device should be named: " + s.reason());
    }

    @Test
    void twoIndistinguishableDevicesAreAmbiguous() {
        AdbDevices.Selection s = selectionOf(
                "PHONE123\tdevice model:Pixel_6_Pro\nTABLET456\tdevice model:Galaxy_Tab\n");
        assertEquals(AdbDevices.Status.AMBIGUOUS, s.status());
        assertEquals(2, s.statusCode());
        assertNull(s.serial());
        assertTrue(s.reason().contains("PHONE123"));
        assertTrue(s.reason().contains("TABLET456"));
        assertEquals(2, s.pickable().size(), "both must reach the picker");
    }

    @Test
    void twoQuestsAreAmbiguous() {
        AdbDevices.Selection s = selectionOf(
                "QUEST_A\tdevice model:Quest_3 device:eureka\n"
              + "QUEST_B\tdevice model:Quest_2 device:hollywood\n");
        assertEquals(AdbDevices.Status.AMBIGUOUS, s.status());
        assertEquals(2, s.pickable().size());
    }

    @Test
    void unauthorizedOnlyIsStatusOne() {
        AdbDevices.Selection s = selectionOf("1WMHH8150XX07M\tunauthorized\n");
        assertEquals(AdbDevices.Status.UNAUTHORIZED, s.status());
        assertEquals(1, s.statusCode());
        assertNull(s.serial());
    }

    @Test
    void noPermissionsIsReportedAsUdevNotAPendingPrompt() {
        AdbDevices.Selection s = selectionOf("ABC\tno permissions (user in plugdev group); "
                + "see [http://developer.android.com/tools/device.html]\n");
        assertEquals(AdbDevices.Status.UNAUTHORIZED, s.status());
        assertTrue(s.reason().contains("udev"), s.reason());
    }

    @Test
    void offlineOnlyIsStatusMinusOneWithASpecificReason() {
        AdbDevices.Selection s = selectionOf("1WMHH8150XX07M\toffline\n");
        assertEquals(AdbDevices.Status.NONE, s.status());
        assertEquals(-1, s.statusCode());
        assertTrue(s.reason().contains("offline"), s.reason());
    }

    @Test
    void emulatorAloneIsNotAUsableDevice() {
        AdbDevices.Selection s = selectionOf("emulator-5554\tdevice\n");
        assertEquals(AdbDevices.Status.NONE, s.status());
        assertTrue(s.reason().contains("emulator"), s.reason());
    }

    @Test
    void nothingAttachedIsStatusMinusOne() {
        AdbDevices.Selection s = selectionOf("");
        assertEquals(AdbDevices.Status.NONE, s.status());
        assertEquals(-1, s.statusCode());
        assertTrue(s.reason().contains("no devices"), s.reason());
    }

    @Test
    void chosenResolvesBackToTheDevice() {
        AdbDevices.Selection s = selectionOf(
                "1WMHH8150XX07M\tdevice model:Quest_3\nemulator-5554\tdevice\n");
        assertNotNull(s.chosen());
        assertEquals("Quest 3 -- 1WMHH8150XX07M", s.chosen().label());
    }
}
