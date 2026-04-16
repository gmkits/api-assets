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
 * 读取 {@code .hday} 二进制数据包文件并生成 {@link HdayBundle} 实例。
 *
 * <h3>二进制布局</h3>
 * <pre>
 * 文件头（32 字节）
 *   magic          : 4B，固定为 "HDAY"
 *   majorVersion   : u8
 *   minorVersion   : u8
 *   flags          : u16，小端
 *   year           : u16，小端
 *   regionCodeLen  : u8
 *   regionCode     : 16B，UTF-8，尾部补零
 *   calendarSystem : u8
 *   dayCount       : u16，小端
 *   sectionCount   : u16，小端
 *
 * 分段表          : sectionCount × 8B
 *   type   : u16，小端
 *   offset : u32，小端
 *   length : u16，小端
 *
 * 数据分段（DAY_TABLE、STRING_TABLE、NAME_LIST_TABLE）
 *
 * CRC32           : 4B，小端，覆盖前面所有字节
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
     * 从给定文件路径读取 {@code .hday} 数据包。
     *
     * @param path {@code .hday} 文件路径
     * @return 解析后的数据包
     * @throws IOException 当文件无法读取或内容格式非法时抛出
     */
    public static HdayBundle read(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        return parse(data);
    }

    /**
     * 从 {@link InputStream} 读取 {@code .hday} 数据包。
     *
     * @param in 输入流；会被完整读取，但不会在此处关闭
     * @return 解析后的数据包
     * @throws IOException 当流无法读取或数据格式非法时抛出
     */
    public static HdayBundle read(InputStream in) throws IOException {
        byte[] data = readAllBytes(in);
        return parse(data);
    }

    private static HdayBundle parse(byte[] data) throws IOException {
        if (data.length < HEADER_SIZE + 4) {
            throw new IOException("File too small to be a valid .hday bundle");
        }

        // 校验 CRC32。
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

        // 读取文件头。
        byte[] magic = new byte[4];
        buf.get(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1]
                || magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
            throw new IOException("Invalid magic bytes");
        }

        /*
         * 文件头偏移说明：
         * 4/5 为主次版本号，6-7 为预留 flags，
         * 8-10 为年份和区域代码长度，11-26 为区域代码，
         * 27-31 为历法、日期数量和分段数量。
         */
        int majorVersion = Byte.toUnsignedInt(buf.get());
        int minorVersion = Byte.toUnsignedInt(buf.get());
        buf.getShort();
        int year = Short.toUnsignedInt(buf.getShort());
        int regionCodeLen = Byte.toUnsignedInt(buf.get());
        byte[] regionBytes = new byte[16];
        buf.get(regionBytes);
        String regionCode = new String(regionBytes, 0, regionCodeLen, StandardCharsets.UTF_8);
        int calSys = Byte.toUnsignedInt(buf.get());
        CalendarSystem calendarSystem = calSys < CalendarSystem.values().length
                ? CalendarSystem.values()[calSys] : CalendarSystem.GREGORIAN;
        int dayCount = Short.toUnsignedInt(buf.getShort());
        int sectionCount = Short.toUnsignedInt(buf.getShort());

        // 读取分段表。
        int[] secTypes   = new int[sectionCount];
        int[] secOffsets = new int[sectionCount];
        int[] secLengths = new int[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            secTypes[i]   = Short.toUnsignedInt(buf.getShort());
            secOffsets[i] = buf.getInt();
            secLengths[i] = Short.toUnsignedInt(buf.getShort());
        }

        // 按类型定位各个分段。
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
                    // 忽略未知分段类型。
                    break;
            }
        }

        // 读取日期表分段。
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

        // 读取字符串表分段。
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

        // 读取名称列表分段。
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
