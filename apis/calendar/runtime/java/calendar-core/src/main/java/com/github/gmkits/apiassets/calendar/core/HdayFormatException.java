package com.github.gmkits.apiassets.calendar.core;

import java.io.IOException;

/**
 * 表示 {@code .hday} 文件违反二进制格式约束。
 *
 * <p>错误码与 TypeScript {@code HdayFormatError} 保持一致，调用方可以区分
 * 文件不存在、版本不支持和数据损坏，而不需要解析异常文本。</p>
 */
public final class HdayFormatException extends IOException {

    /** 稳定的格式错误分类。 */
    public enum Code {
        /** 文件长度不足以容纳完整头部。 */
        TOO_SMALL,
        /** 文件魔数不是 {@code HDAY}。 */
        BAD_MAGIC,
        /** 文件主版本不受当前读取器支持。 */
        UNSUPPORTED_VERSION,
        /** 文件内容与尾部 CRC32 校验值不一致。 */
        BAD_CRC,
        /** 头部字段值或组合违反格式约束。 */
        BAD_HEADER,
        /** 地区代码或字符串池包含无效 UTF-8。 */
        BAD_UTF8,
        /** section 目录越界、重叠、重复或描述符无效。 */
        BAD_SECTION_TABLE,
        /** 文件包含当前读取器无法理解的关键 section。 */
        UNKNOWN_CRITICAL_SECTION,
        /** 文件缺少必须存在的关键 section。 */
        MISSING_SECTION,
        /** section 的内部编码或长度无效。 */
        BAD_SECTION,
        /** 字符串、名称列表或日期索引越界。 */
        BAD_INDEX,
        /** 日期覆盖记录包含非法日期、状态或保留位。 */
        BAD_DAY_OVERRIDE
    }

    /** 可供程序稳定判断的错误分类。 */
    private final Code code;

    /**
     * 创建格式异常。
     *
     * @param code 稳定错误码
     * @param message 可读错误说明
     */
    public HdayFormatException(Code code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 返回稳定错误码。
     *
     * @return 错误分类
     */
    public Code getCode() {
        return code;
    }
}
