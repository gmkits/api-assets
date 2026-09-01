package com.github.gmkits.apiassets.calendar.service.api;

import com.github.gmkits.apiassets.calendar.service.ApiException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 严格解析日期查询的 locale 和顶层字段投影。 */
public final class CalendarQueryOptions {
    public enum Field {
        HOLIDAY_NAMES("holidayNames"),
        LABELS("labels"),
        LUNAR("lunar"),
        SOLAR_TERM("solarTerm"),
        GAN_ZHI("ganZhi"),
        FESTIVALS("festivals");

        private final String wireName;

        Field(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }

        static Field fromWireName(String value) {
            for (Field field : values()) {
                if (field.wireName.equals(value)) return field;
            }
            throw ApiException.badRequest("不支持的 fields 字段: " + value);
        }
    }

    public static final String DEFAULT_LOCALE = "zh-CN";
    public static final Set<String> SUPPORTED_LOCALES = Set.of("zh-CN", "en-US");
    private final String locale;
    private final EnumSet<Field> fields;

    private CalendarQueryOptions(String locale, EnumSet<Field> fields) {
        this.locale = locale;
        this.fields = fields;
    }

    public static CalendarQueryOptions of(String locale, String fields) {
        String normalizedLocale = normalizeLocale(locale);
        if (fields == null) return full(normalizedLocale);
        if (fields.isBlank()) throw ApiException.badRequest("fields 不能为空");
        EnumSet<Field> selected = EnumSet.noneOf(Field.class);
        for (String token : fields.split(",", -1)) {
            if (token.isBlank()) throw ApiException.badRequest("fields 包含空字段");
            selected.add(Field.fromWireName(token.trim()));
        }
        return new CalendarQueryOptions(normalizedLocale, selected);
    }

    public static CalendarQueryOptions of(String locale, List<String> fields) {
        String normalizedLocale = normalizeLocale(locale);
        if (fields == null) return full(normalizedLocale);
        if (fields.isEmpty()) throw ApiException.badRequest("fields 不能为空");
        EnumSet<Field> selected = EnumSet.noneOf(Field.class);
        for (String token : fields) {
            if (token == null || token.isBlank()) {
                throw ApiException.badRequest("fields 包含空字段");
            }
            selected.add(Field.fromWireName(token.trim()));
        }
        return new CalendarQueryOptions(normalizedLocale, selected);
    }

    private static CalendarQueryOptions full(String locale) {
        return new CalendarQueryOptions(locale, EnumSet.allOf(Field.class));
    }

    public static String normalizeLocale(String locale) {
        String value = locale == null ? DEFAULT_LOCALE : locale.trim();
        if (!SUPPORTED_LOCALES.contains(value)) {
            throw ApiException.badRequest("不支持的 locale: " + value);
        }
        return value;
    }

    public String locale() { return locale; }

    public boolean includes(Field field) { return fields.contains(field); }

    public Set<Field> fields() {
        return Collections.unmodifiableSet(fields);
    }
}
