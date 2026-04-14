package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.CalendarSystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

/**
 * Reads {@code .hday} binary bundle files and produces {@link HdayBundle} instances.
 *
 * <h3>Binary layout</h3>
 * <pre>
 * Header (32 bytes)
 *   magic          : 4B  "HDAY"
 *   majorVersion   : u8
 *   minorVersion   : u8
 *   flags          : u16 LE
 *   year           : u16 LE
 *   regionCodeLen  : u8
 *   regionCode     : 16B  UTF-8 zero-padded
 *   calendarSystem : u8
 *   dayCount       : u16 LE
 *   sectionCount   : u16 LE
 *
 * Section table    : sectionCount × 8B
 *   type   : u16 LE
 *   offset : u32 LE
 *   length : u16 LE
 *
 * Sections (DAY_TABLE, STRING_TABLE, NAME_LIST_TABLE)
 *
 * CRC32            : 4B  LE (over all preceding bytes)
 * </pre>
 */
public final class HdayReader {

    private static final byte[] MAGIC = { 'H', 'D', 'A', 'Y' };
    private static final int HEADER_SIZE = 32;
    private static final int SECTION_ENTRY_SIZE = 8;
    private static final int DAY_ENTRY_SIZE = 8;

    private static final int SECTION_DAY_TABLE        = 0x0001;
    private static final int SECTION_STRING_TABLE      = 0x0002;
    private static final int SECTION_NAME_LIST_TABLE   = 0x0003;

    private HdayReader() { }

    /**
     * Reads a {@code .hday} bundle from the given filesystem path.
     *
     * @param path path to the {@code .hday} file
     * @return the parsed bundle
     * @throws IOException if the file cannot be read or is malformed
     */
    public static HdayBundle read(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        return parse(data);
    }

    /**
     * Reads a {@code .hday} bundle from an {@link InputStream}.
     *
     * @param in the input stream (fully consumed but not closed)
     * @return the parsed bundle
     * @throws IOException if the stream cannot be read or data is malformed
     */
    public static HdayBundle read(InputStream in) throws IOException {
        byte[] data = readAllBytes(in);
        return parse(data);
    }

    private static HdayBundle parse(byte[] data) throws IOException {
        if (data.length < HEADER_SIZE + 4) {
            throw new IOException("File too small to be a valid .hday bundle");
        }

        // Verify CRC32
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length - 4);
        long computed = crc.getValue();
        ByteBuffer crcBuf = ByteBuffer.wrap(data, data.length - 4, 4).order(ByteOrder.LITTLE_ENDIAN);
        long stored = Integer.toUnsignedLong(crcBuf.getInt());
        if (computed != stored) {
            throw new IOException("CRC32 mismatch: expected "
                    + Long.toHexString(stored) + " but computed " + Long.toHexString(computed));
        }

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // --- Header ---
        byte[] magic = new byte[4];
        buf.get(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1]
                || magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
            throw new IOException("Invalid magic bytes");
        }

        int majorVersion = Byte.toUnsignedInt(buf.get());      // offset 4  (u8)
        int minorVersion = Byte.toUnsignedInt(buf.get());       // offset 5  (u8)
        buf.getShort();                                          // offset 6-7  flags (u16), reserved
        int year = Short.toUnsignedInt(buf.getShort());          // offset 8-9  (u16)
        int regionCodeLen = Byte.toUnsignedInt(buf.get());       // offset 10   (u8)
        byte[] regionBytes = new byte[16];
        buf.get(regionBytes);                                    // offset 11-26 (16B)
        String regionCode = new String(regionBytes, 0, regionCodeLen, StandardCharsets.UTF_8);
        int calSys = Byte.toUnsignedInt(buf.get());              // offset 27   (u8)
        CalendarSystem calendarSystem = calSys < CalendarSystem.values().length
                ? CalendarSystem.values()[calSys] : CalendarSystem.GREGORIAN;
        int dayCount = Short.toUnsignedInt(buf.getShort());      // offset 28
        int sectionCount = Short.toUnsignedInt(buf.getShort());  // offset 30

        // --- Section table ---
        int[] secTypes   = new int[sectionCount];
        int[] secOffsets = new int[sectionCount];
        int[] secLengths = new int[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            secTypes[i]   = Short.toUnsignedInt(buf.getShort());
            secOffsets[i] = buf.getInt();
            secLengths[i] = Short.toUnsignedInt(buf.getShort());
        }

        // Locate sections by type
        int dayTableOff = -1, dayTableLen = 0;
        int strTableOff = -1, strTableLen = 0;
        int nameListOff = -1, nameListLen = 0;
        for (int i = 0; i < sectionCount; i++) {
            switch (secTypes[i]) {
                case SECTION_DAY_TABLE:
                    dayTableOff = secOffsets[i]; dayTableLen = secLengths[i]; break;
                case SECTION_STRING_TABLE:
                    strTableOff = secOffsets[i]; strTableLen = secLengths[i]; break;
                case SECTION_NAME_LIST_TABLE:
                    nameListOff = secOffsets[i]; nameListLen = secLengths[i]; break;
                default:
                    break; // unknown section, skip
            }
        }

        // --- Day table ---
        HdayBundle.DayEntry[] days = new HdayBundle.DayEntry[dayCount];
        if (dayTableOff >= 0) {
            buf.position(dayTableOff);
            for (int i = 0; i < dayCount; i++) {
                int flags         = Short.toUnsignedInt(buf.getShort());
                int nameListIndex = Short.toUnsignedInt(buf.getShort());
                int labelListIdx  = Short.toUnsignedInt(buf.getShort());
                int extIndex      = Short.toUnsignedInt(buf.getShort());
                days[i] = new HdayBundle.DayEntry(flags, nameListIndex, labelListIdx, extIndex);
            }
        }

        // --- String table ---
        String[] strings = new String[0];
        if (strTableOff >= 0) {
            buf.position(strTableOff);
            int stringCount = Short.toUnsignedInt(buf.getShort());
            strings = new String[stringCount];
            for (int i = 0; i < stringCount; i++) {
                int len = Short.toUnsignedInt(buf.getShort());
                byte[] strBytes = new byte[len];
                buf.get(strBytes);
                strings[i] = new String(strBytes, StandardCharsets.UTF_8);
            }
        }

        // --- Name list table ---
        int[][][] nameLists = new int[0][][];
        if (nameListOff >= 0) {
            buf.position(nameListOff);
            int listCount = Short.toUnsignedInt(buf.getShort());
            nameLists = new int[listCount][][];
            for (int i = 0; i < listCount; i++) {
                int pairCount = Short.toUnsignedInt(buf.getShort());
                nameLists[i] = new int[pairCount][2];
                for (int j = 0; j < pairCount; j++) {
                    nameLists[i][j][0] = Short.toUnsignedInt(buf.getShort());
                    nameLists[i][j][1] = Short.toUnsignedInt(buf.getShort());
                }
            }
        }

        return new HdayBundle(year, regionCode, calendarSystem, dayCount,
                majorVersion, minorVersion, days, strings, nameLists);
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        byte[] tmp = new byte[4096];
        int n;
        while ((n = in.read(tmp)) != -1) {
            bos.write(tmp, 0, n);
        }
        return bos.toByteArray();
    }
}
