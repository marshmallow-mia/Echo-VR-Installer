import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Stamps an icon and a VERSIONINFO block into an existing PE executable, without Windows and
 * without wine. Used to give the jpackage launcher lifted out of the Windows JDK the same icon and
 * Explorer metadata a real `jpackage --icon` run would give it.
 *
 * Strategy: the launcher's .rsrc section is 2.5 KB and is not the last section, so it cannot grow in
 * place. Instead the existing resource tree is parsed, the new resources are merged into it, and the
 * whole tree is re-serialised into a brand-new section appended to the end of the file. Only the
 * resource data directory entry points at it, so the old .rsrc simply becomes dead bytes.
 */
public final class PeIconStamper {

    private static final int RT_ICON = 3, RT_GROUP_ICON = 14, RT_VERSION = 16;
    private static final int DEFAULT_LANG = 1033;      // en-US, what the JDK launcher already uses
    private static final int DEFAULT_CODEPAGE = 1252;

    /** One resource leaf: the bytes plus the code page recorded alongside them. */
    private record Leaf(byte[] data, int codePage) {}

    // type -> name -> language -> leaf. TreeMaps because a resource directory's entries must be
    // written in ascending order of ID; Windows binary-searches them.
    private final TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Leaf>>> tree = new TreeMap<>();

    private byte[] img;
    private int peOff, optOff, numSections, sectionAlign, fileAlign, ddOff;
    private final List<int[]> sections = new ArrayList<>();   // {vSize, vAddr, rawSize, rawPtr}
    private final List<Integer> sectionHdrOffsets = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: PeIconStamper <exe> <ico> <version> [key=value ...]");
            System.exit(2);
        }
        Map<String, String> strings = new LinkedHashMap<>();
        for (int i = 3; i < args.length; i++) {
            int eq = args[i].indexOf('=');
            strings.put(args[i].substring(0, eq), args[i].substring(eq + 1));
        }
        stamp(Paths.get(args[0]), Paths.get(args[1]), args[2], strings);
        System.out.println("stamped " + args[0]);
    }

    public static void stamp(Path exe, Path ico, String version, Map<String, String> strings)
            throws IOException {
        PeIconStamper s = new PeIconStamper();
        s.img = Files.readAllBytes(exe);
        s.parseHeaders();
        s.readExistingResources();
        if (ico != null) s.addIcon(Files.readAllBytes(ico));
        if (version != null) s.setVersionInfo(version, strings);
        Files.write(exe, s.rebuild());
    }

    // ---------------------------------------------------------------------------- little-endian I/O
    private int u8(int o)  { return img[o] & 0xFF; }
    private int u16(int o) { return u8(o) | (u8(o + 1) << 8); }
    private int i32(int o) { return u16(o) | (u16(o + 2) << 16); }
    private long u32(int o) { return i32(o) & 0xFFFFFFFFL; }
    private void put16(byte[] b, int o, int v) { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); }
    private void put32(byte[] b, int o, int v) { put16(b, o, v & 0xFFFF); put16(b, o + 2, v >>> 16); }
    private static int align(int v, int a) { return (v + a - 1) / a * a; }

    // ------------------------------------------------------------------------------------ headers
    private void parseHeaders() {
        if (u16(0) != 0x5A4D) throw new IllegalStateException("not an MZ image");
        peOff = i32(0x3C);
        if (i32(peOff) != 0x00004550) throw new IllegalStateException("not a PE image");
        numSections = u16(peOff + 6);
        int optSize = u16(peOff + 20);
        optOff = peOff + 24;
        int magic = u16(optOff);
        if (magic != 0x20B) throw new IllegalStateException("expected PE32+ (got magic 0x" + Integer.toHexString(magic) + ")");
        sectionAlign = i32(optOff + 32);
        fileAlign = i32(optOff + 36);
        ddOff = optOff + 112;                       // data directories, PE32+ layout
        int secTable = optOff + optSize;
        for (int i = 0; i < numSections; i++) {
            int b = secTable + i * 40;
            sectionHdrOffsets.add(b);
            sections.add(new int[]{i32(b + 8), i32(b + 12), i32(b + 16), i32(b + 20)});
        }
        int headersEnd = secTable + (numSections + 1) * 40;
        int firstRaw = sections.stream().mapToInt(s -> s[3]).filter(p -> p > 0).min().orElse(Integer.MAX_VALUE);
        if (headersEnd > Math.min(i32(optOff + 60), firstRaw)) {
            throw new IllegalStateException("no room in the PE header for another section entry");
        }
    }

    private int rvaToOffset(int rva) {
        for (int[] s : sections) {
            if (rva >= s[1] && rva < s[1] + Math.max(s[0], s[2])) return s[3] + (rva - s[1]);
        }
        throw new IllegalStateException("RVA 0x" + Integer.toHexString(rva) + " is outside every section");
    }

    // -------------------------------------------------------------------------- read existing tree
    private void readExistingResources() {
        int rva = i32(ddOff + 2 * 8);
        if (rva == 0) return;                       // no resources at all: nothing to preserve
        walk(rvaToOffset(rva), rvaToOffset(rva), 0, 0, 0);
    }

    private void walk(int base, int dirOff, int level, int type, int name) {
        int named = u16(dirOff + 12), ids = u16(dirOff + 14);
        if (named != 0) {
            // The JDK launcher uses integer IDs throughout. Bailing out is better than silently
            // dropping a string-named resource we do not know how to re-emit.
            throw new IllegalStateException("string-named resources are not supported (level " + level + ")");
        }
        for (int i = 0; i < ids; i++) {
            int e = dirOff + 16 + i * 8;
            int id = i32(e);
            int off = i32(e + 4);
            if ((off & 0x80000000) != 0) {
                int child = base + (off & 0x7FFFFFFF);
                if (level == 0) walk(base, child, 1, id, 0);
                else if (level == 1) walk(base, child, 2, type, id);
                else throw new IllegalStateException("resource tree deeper than 3 levels");
            } else {
                int de = base + off;
                int dataRva = i32(de), size = i32(de + 4), cp = i32(de + 8);
                byte[] data = Arrays.copyOfRange(img, rvaToOffset(dataRva), rvaToOffset(dataRva) + size);
                put(type, name, id, new Leaf(data, cp));
            }
        }
    }

    private void put(int type, int name, int lang, Leaf leaf) {
        tree.computeIfAbsent(type, k -> new TreeMap<>())
            .computeIfAbsent(name, k -> new TreeMap<>())
            .put(lang, leaf);
    }

    // ---------------------------------------------------------------------------------- icon group
    /** Splits a .ico file into RT_ICON leaves plus the RT_GROUP_ICON directory that indexes them. */
    private void addIcon(byte[] ico) {
        if (u16le(ico, 0) != 0 || u16le(ico, 2) != 1) throw new IllegalArgumentException("not an .ico file");
        int count = u16le(ico, 4);
        byte[] group = new byte[6 + count * 14];
        put16(group, 0, 0); put16(group, 2, 1); put16(group, 4, count);
        tree.remove(RT_ICON);
        tree.remove(RT_GROUP_ICON);
        for (int i = 0; i < count; i++) {
            int e = 6 + i * 16;
            int bytes = i32le(ico, e + 8), offset = i32le(ico, e + 12);
            int id = i + 1;
            put(RT_ICON, id, DEFAULT_LANG,
                new Leaf(Arrays.copyOfRange(ico, offset, offset + bytes), DEFAULT_CODEPAGE));
            // GRPICONDIRENTRY is ICONDIRENTRY with the 4-byte file offset replaced by a 2-byte id.
            int g = 6 + i * 14;
            System.arraycopy(ico, e, group, g, 12);
            put32(group, g + 8, bytes);
            put16(group, g + 12, id);
        }
        // ID 1: Explorer shows the icon group with the lowest ID as the application icon.
        put(RT_GROUP_ICON, 1, DEFAULT_LANG, new Leaf(group, DEFAULT_CODEPAGE));
    }

    private static int u16le(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
    private static int i32le(byte[] b, int o) { return u16le(b, o) | (u16le(b, o + 2) << 16); }

    // --------------------------------------------------------------------------------- VERSIONINFO
    private void setVersionInfo(String version, Map<String, String> strings) {
        int[] v = new int[4];
        String[] parts = version.split("[.\\-+]");
        for (int i = 0; i < 4 && i < parts.length; i++) {
            try { v[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) {}
        }
        int ms = (v[0] << 16) | v[1], ls = (v[2] << 16) | v[3];

        byte[] fixed = new byte[52];
        put32(fixed, 0, 0xFEEF04BD);        // dwSignature
        put32(fixed, 4, 0x00010000);        // dwStrucVersion
        put32(fixed, 8, ms); put32(fixed, 12, ls);
        put32(fixed, 16, ms); put32(fixed, 20, ls);
        put32(fixed, 24, 0x3F);             // dwFileFlagsMask
        put32(fixed, 28, 0);                // dwFileFlags
        put32(fixed, 32, 0x00040004);       // dwFileOS = VOS_NT_WINDOWS32
        put32(fixed, 36, 1);                // dwFileType = VFT_APP
        // dwFileSubtype and the two dwFileDate words stay zero.

        List<byte[]> entries = new ArrayList<>();
        Map<String, String> all = new LinkedHashMap<>(strings);
        all.putIfAbsent("FileVersion", version);
        all.putIfAbsent("ProductVersion", version);
        for (Map.Entry<String, String> e : all.entrySet()) {
            byte[] val = wide(e.getValue());
            entries.add(node(e.getKey(), val, val.length / 2, 1));
        }
        byte[] table = node("040904E4", concat(entries), 0, 1);   // en-US / codepage 1252
        byte[] sfi = node("StringFileInfo", table, 0, 1);
        byte[] translation = new byte[4];
        put32(translation, 0, 0x04E40409);
        byte[] vfi = node("VarFileInfo", node("Translation", translation, 4, 0), 0, 1);
        byte[] root = node("VS_VERSION_INFO", concat(List.of(fixed, sfi, vfi)), fixed.length, 0);

        put(RT_VERSION, 1, DEFAULT_LANG, new Leaf(root, DEFAULT_CODEPAGE));
    }

    /**
     * One VS_VERSIONINFO node: wLength, wValueLength, wType, a null-terminated UTF-16 key, DWORD
     * padding, then the value/children. wValueLength counts WCHARs for text nodes and bytes for
     * binary ones, which is why it is passed in rather than derived.
     */
    private byte[] node(String key, byte[] value, int valueLength, int type) {
        byte[] k = wide(key);
        int headerLen = align(6 + k.length, 4);
        byte[] out = new byte[headerLen + value.length];
        put16(out, 0, out.length);
        put16(out, 2, valueLength);
        put16(out, 4, type);
        System.arraycopy(k, 0, out, 6, k.length);
        System.arraycopy(value, 0, out, headerLen, value.length);
        return out;
    }

    private static byte[] wide(String s) {
        byte[] out = new byte[(s.length() + 1) * 2];
        for (int i = 0; i < s.length(); i++) { out[i * 2] = (byte) s.charAt(i); out[i * 2 + 1] = (byte) (s.charAt(i) >> 8); }
        return out;
    }

    private static byte[] concat(List<byte[]> parts) {
        int n = 0;
        for (byte[] p : parts) n += align(p.length, 4);
        byte[] out = new byte[n];
        int o = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, out, o, p.length); o += align(p.length, 4); }
        return out;
    }

    // ------------------------------------------------------------------------------- serialisation
    private byte[] serializeResources(int sectionRva) {
        int leaves = 0, dirs = 1;
        for (var t : tree.values()) { dirs++; for (var n : t.values()) { dirs++; leaves += n.size(); } }
        int dirBytes = 0;
        dirBytes += 16 + 8 * tree.size();
        for (var t : tree.values()) {
            dirBytes += 16 + 8 * t.size();
            for (var n : t.values()) dirBytes += 16 + 8 * n.size();
        }
        int dataEntryBase = align(dirBytes, 4);
        int blobBase = align(dataEntryBase + leaves * 16, 8);

        // Pass 1: lay the blobs out so the data entries can reference them.
        Map<Leaf, Integer> blobOffset = new IdentityHashMap<>();
        int cursor = blobBase;
        for (var t : tree.values()) for (var n : t.values()) for (Leaf l : n.values()) {
            blobOffset.put(l, cursor);
            cursor = align(cursor + l.data().length, 8);
        }
        byte[] out = new byte[cursor];

        // Pass 2: emit the three directory levels breadth-first, then the data entries, then blobs.
        int typeDirEnd = 16 + 8 * tree.size();
        int nameDirCursor = typeDirEnd;
        int langDirCursor = typeDirEnd;
        for (var t : tree.values()) langDirCursor += 16 + 8 * t.size();
        int dataEntryCursor = dataEntryBase;

        writeDirHeader(out, 0, tree.size());
        int e = 16;
        for (var type : tree.entrySet()) {
            put32(out, e, type.getKey());
            put32(out, e + 4, nameDirCursor | 0x80000000);
            e += 8;
            writeDirHeader(out, nameDirCursor, type.getValue().size());
            int e2 = nameDirCursor + 16;
            for (var name : type.getValue().entrySet()) {
                put32(out, e2, name.getKey());
                put32(out, e2 + 4, langDirCursor | 0x80000000);
                e2 += 8;
                writeDirHeader(out, langDirCursor, name.getValue().size());
                int e3 = langDirCursor + 16;
                for (var lang : name.getValue().entrySet()) {
                    put32(out, e3, lang.getKey());
                    put32(out, e3 + 4, dataEntryCursor);
                    e3 += 8;
                    Leaf l = lang.getValue();
                    int blob = blobOffset.get(l);
                    put32(out, dataEntryCursor, sectionRva + blob);
                    put32(out, dataEntryCursor + 4, l.data().length);
                    put32(out, dataEntryCursor + 8, l.codePage());
                    dataEntryCursor += 16;
                    System.arraycopy(l.data(), 0, out, blob, l.data().length);
                }
                langDirCursor += 16 + 8 * name.getValue().size();
            }
            nameDirCursor += 16 + 8 * type.getValue().size();
        }
        return out;
    }

    private void writeDirHeader(byte[] out, int off, int idCount) {
        put16(out, off + 12, 0);          // NumberOfNamedEntries
        put16(out, off + 14, idCount);    // NumberOfIdEntries
    }

    // ------------------------------------------------------------------------------ append + fixup
    private byte[] rebuild() {
        int[] last = sections.get(sections.size() - 1);
        int newVa = align(last[1] + last[0], sectionAlign);
        byte[] rsrc = serializeResources(newVa);
        int newRawPtr = align(img.length, fileAlign);
        int newRawSize = align(rsrc.length, fileAlign);

        byte[] out = Arrays.copyOf(img, newRawPtr + newRawSize);
        System.arraycopy(rsrc, 0, out, newRawPtr, rsrc.length);

        int hdr = sectionHdrOffsets.get(sectionHdrOffsets.size() - 1) + 40;
        byte[] name = ".rsrc".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(name, 0, out, hdr, name.length);
        put32(out, hdr + 8, rsrc.length);      // VirtualSize
        put32(out, hdr + 12, newVa);           // VirtualAddress
        put32(out, hdr + 16, newRawSize);      // SizeOfRawData
        put32(out, hdr + 20, newRawPtr);       // PointerToRawData
        put32(out, hdr + 36, 0x40000040);      // CNT_INITIALIZED_DATA | MEM_READ

        put16(out, peOff + 6, numSections + 1);
        put32(out, optOff + 56, align(newVa + rsrc.length, sectionAlign));   // SizeOfImage
        put32(out, ddOff + 2 * 8, newVa);                                    // resource dir RVA
        put32(out, ddOff + 2 * 8 + 4, rsrc.length);                          // resource dir size

        put32(out, optOff + 64, 0);
        put32(out, optOff + 64, checksum(out));
        return out;
    }

    /** The PE header checksum: a folded 16-bit ones-complement sum of the image plus its length. */
    private int checksum(byte[] b) {
        long sum = 0;
        for (int i = 0; i + 1 < b.length; i += 2) {
            sum += ((b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8));
            sum = (sum & 0xFFFF) + (sum >>> 16);
        }
        if ((b.length & 1) != 0) {
            sum += (b[b.length - 1] & 0xFF);
            sum = (sum & 0xFFFF) + (sum >>> 16);
        }
        sum = (sum & 0xFFFF) + (sum >>> 16);
        return (int) (sum + b.length);
    }
}
