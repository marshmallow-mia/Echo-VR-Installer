package bl00dy_c0d3_.echovr_installer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads, parses and interprets the {@code adb devices -l} table.
 *
 * <p>Everything except {@link #probe()} is a pure function of the command's text, which is
 * what makes the interesting logic testable without a headset attached.
 *
 * <p>This replaces the old first-match-wins scan in {@code InstallerQuest.checkQuestStatus},
 * which returned "connected" on the first line ending in {@code device} and therefore never
 * noticed a second entry -- an emulator, a phone, an offline stub, a second headset. The
 * installer reported "Quest connected" and every later adb command then failed with
 * {@code more than one device/emulator}.
 */
public final class AdbDevices {

    private AdbDevices() {}

    /** Outcome of interpreting a device table. See {@link Selection#statusCode()}. */
    public enum Status { READY, UNAUTHORIZED, AMBIGUOUS, NONE }

    /**
     * Keys {@code adb devices -l} appends after the state. States can contain spaces
     * ("no permissions (user in plugdev group); see [http://...]"), these cannot -- which is
     * how the two are told apart while parsing.
     */
    private static final Set<String> PROP_KEYS =
            Set.of("usb", "product", "model", "device", "transport_id");

    /**
     * Quest board/codenames, matched as prefixes because builds append suffixes
     * (e.g. {@code hollywood_r}). Used only to break a tie between several authorized
     * devices -- never as a hard filter, so an unrecognised future headset still works when
     * it is the only candidate.
     */
    private static final String[] QUEST_CODENAMES = {
            "hollywood", "eureka", "panther", "monterey", "seacliff", "vr_monterey"
    };

    /** One row of the device table. */
    public record Device(String serial, String state, Map<String, String> props, String raw) {

        public Device {
            props = Collections.unmodifiableMap(new LinkedHashMap<>(props));
        }

        /** True only for the plain {@code device} state -- not offline, recovery, sideload. */
        public boolean authorized() { return "device".equals(state); }

        public boolean emulator() { return serial.startsWith("emulator-"); }

        /** Marketing model as adb reports it, e.g. {@code Quest_3}; null when -l gave none. */
        public String model() { return props.get("model"); }

        /** Looks like a Meta headset, by model name or board codename. */
        public boolean questLike() {
            String model = lower(props.get("model"));
            if (model != null && (model.startsWith("quest") || model.startsWith("oculus"))) {
                return true;
            }
            for (String key : new String[] {"device", "product"}) {
                String value = lower(props.get(key));
                if (value == null) continue;
                for (String codename : QUEST_CODENAMES) {
                    if (value.startsWith(codename)) return true;
                }
            }
            return false;
        }

        /** Human label for a picker button: "Quest 3 -- 1WMHH8150XX07M". */
        public String label() {
            String model = model();
            if (model != null && !model.isEmpty()) {
                return model.replace('_', ' ') + " -- " + serial;
            }
            return serial + " (" + state + ")";
        }

        /** One dense log line: serial, state, the -l props that matter, and our tags. */
        public String describe() {
            StringBuilder sb = new StringBuilder();
            sb.append(serial).append("  ").append(state);
            for (String key : new String[] {"model", "product", "device", "transport_id"}) {
                String value = props.get(key);
                if (value != null) sb.append("  ").append(key).append(':').append(value);
            }
            if (emulator()) sb.append("  [EMULATOR]");
            if (questLike()) sb.append("  [QUEST?]");
            return sb.toString();
        }

        private static String lower(String s) {
            return s == null ? null : s.toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The interpretation of one device table: which serial to target, why, and everything
     * that was seen while deciding.
     */
    public record Selection(String serial, List<Device> all, String reason, Status status) {

        public Selection {
            all = List.copyOf(all);
        }

        /**
         * The legacy {@code InstallerQuest.checkQuestStatus} contract, extended.
         *
         * <p>{@code 0} ready, {@code 1} unauthorized, {@code -1} nothing usable -- all
         * unchanged -- plus {@code 2} for "several usable devices, none selectable". The new
         * value is deliberately not folded into {@code -1}: every existing
         * {@code status == 0} / {@code != 0} gate stays correct without an edit, and only
         * the places that switch on the value need a new arm.
         */
        public int statusCode() {
            return switch (status) {
                case READY -> 0;
                case UNAUTHORIZED -> 1;
                case AMBIGUOUS -> 2;
                case NONE -> -1;
            };
        }

        /** The devices a picker may offer: authorized, and not an emulator. */
        public List<Device> pickable() {
            List<Device> out = new ArrayList<>();
            for (Device d : all) {
                if (d.authorized() && !d.emulator()) out.add(d);
            }
            return out;
        }

        /** The chosen device, or null when nothing was selected. */
        public Device chosen() {
            if (serial == null) return null;
            for (Device d : all) {
                if (d.serial().equals(serial)) return d;
            }
            return null;
        }
    }

    // ------------------------------------------------------------------
    // parsing
    // ------------------------------------------------------------------

    /**
     * Parses the output of {@code adb devices -l}.
     *
     * <p>Tolerates everything that shares the stream: the {@code List of devices attached}
     * header, blank lines, and the {@code * daemon not running; starting now *} banners,
     * which land here because the process runs with {@code redirectErrorStream(true)}.
     */
    public static List<Device> parse(String output) {
        List<Device> devices = new ArrayList<>();
        if (output == null) return devices;

        for (String rawLine : output.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("*")) continue;                       // daemon banners
            if (line.startsWith("List of devices")) continue;         // header
            if (line.startsWith("adb:") || line.startsWith("error:")) continue;

            int split = -1;
            for (int i = 0; i < line.length(); i++) {
                if (Character.isWhitespace(line.charAt(i))) { split = i; break; }
            }
            if (split < 0) continue;                                  // a lone token is not a row

            String serial = line.substring(0, split);
            String rest = line.substring(split).trim();
            if (rest.isEmpty()) continue;

            Map<String, String> props = new LinkedHashMap<>();
            StringBuilder state = new StringBuilder();
            for (String token : rest.split("\\s+")) {
                int colon = token.indexOf(':');
                if (colon > 0 && PROP_KEYS.contains(token.substring(0, colon))) {
                    props.put(token.substring(0, colon), token.substring(colon + 1));
                } else {
                    if (state.length() > 0) state.append(' ');
                    state.append(token);
                }
            }
            devices.add(new Device(serial, state.toString(), props, line));
        }
        return devices;
    }

    // ------------------------------------------------------------------
    // selection
    // ------------------------------------------------------------------

    /** Decides which device to target. Pure: no I/O, no Swing, never blocks. */
    public static Selection select(List<Device> devices) {
        List<Device> candidates = new ArrayList<>();
        for (Device d : devices) {
            if (d.authorized() && !d.emulator()) candidates.add(d);
        }

        if (candidates.size() == 1) {
            Device only = candidates.get(0);
            return new Selection(only.serial(), devices,
                    "one authorized device: " + only.label(), Status.READY);
        }

        if (candidates.size() > 1) {
            List<Device> quests = new ArrayList<>();
            for (Device d : candidates) {
                if (d.questLike()) quests.add(d);
            }
            if (quests.size() == 1) {
                Device quest = quests.get(0);
                StringBuilder reason = new StringBuilder("picked ").append(quest.label())
                        .append(" as the only Quest-like device; skipped");
                for (Device d : candidates) {
                    if (d != quest) reason.append(' ').append(d.label()).append(';');
                }
                return new Selection(quest.serial(), devices, reason.toString(), Status.READY);
            }
            StringBuilder reason = new StringBuilder(candidates.size()
                    + " usable devices and no way to tell which is the headset:");
            for (Device d : candidates) {
                reason.append(' ').append(d.label()).append(';');
            }
            return new Selection(null, devices, reason.toString(), Status.AMBIGUOUS);
        }

        // Nothing usable. Say why, so the log distinguishes the cases.
        for (Device d : devices) {
            boolean noPermissions = d.state().startsWith("no permissions");
            if ("unauthorized".equals(d.state()) || noPermissions
                    || d.state().contains("developer.android.com")) {
                // Both land on UNAUTHORIZED because that is what the pre-existing dialog
                // handles, but the reason distinguishes them: "no permissions" is a Linux
                // udev-rules problem, not a pending Allow prompt in the headset.
                String why = noPermissions
                        ? "device " + d.serial() + " is not accessible to this user (udev rules)"
                        : "device " + d.serial() + " has not allowed this PC yet";
                return new Selection(null, devices, why, Status.UNAUTHORIZED);
            }
        }
        for (Device d : devices) {
            if (!d.emulator()) {
                return new Selection(null, devices,
                        "device " + d.serial() + " is in state '" + d.state() + "'", Status.NONE);
            }
        }
        if (!devices.isEmpty()) {
            return new Selection(null, devices, "only emulators are attached", Status.NONE);
        }
        return new Selection(null, devices, "no devices attached", Status.NONE);
    }

    // ------------------------------------------------------------------
    // probing (the one impure entry point)
    // ------------------------------------------------------------------

    private static volatile Selection last =
            new Selection(null, List.of(), "not probed yet", Status.NONE);

    /** The most recent {@link #probe()} result, for diagnostics. Never null. */
    public static Selection last() { return last; }

    /**
     * Runs {@code start-server} then {@code devices -l}, logs both the raw table and the
     * parsed interpretation, and caches the selection.
     *
     * <p>The server is started explicitly so the log reflects the daemon the installer
     * actually talks to, rather than whichever one a user's own {@code adb devices} left
     * running. Both subcommands are exempt from serial pinning (see {@code Adb.takesSerial}),
     * so this cannot recurse into target resolution.
     */
    public static Selection probe() {
        Adb.exec("start-server");
        String output = Adb.exec("devices", "-l");

        System.out.println("**adb-devices: raw table");
        for (String line : output.split("\\r?\\n")) {
            if (!line.isBlank()) System.out.println("**adb-devices |   " + line);
        }

        List<Device> devices = parse(output);
        Selection selection = select(devices);

        if (devices.isEmpty()) {
            System.out.println("**adb-devices =   (no device rows)");
        } else {
            for (Device d : devices) {
                System.out.println("**adb-devices =   " + d.describe());
            }
        }
        System.out.println("**adb-devices: status=" + selection.status()
                + " code=" + selection.statusCode()
                + " serial=" + (selection.serial() == null ? "<none>" : selection.serial()));
        System.out.println("**adb-devices: " + selection.reason());

        last = selection;
        Adb.adoptSelection(selection);
        return selection;
    }
}
