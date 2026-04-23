package com.github.gmkits.holiday.api25.dto;

import com.github.gmkits.holiday.spec.DayInfo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量按日期查询请求体。
 */
@Data
@NoArgsConstructor
public class BatchDayQueryRequest {

    /** 日期列表，最多 100 个。 */
    @NotNull
    @NotEmpty
    @Size(max = 100, message = "单次批量请求最多 100 个日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private List<LocalDate> dates;

    /** 默认地区。 */
    private String regionCode;

    /**
     * 单条结果。{@code data} 与 {@code error} 互斥：成功时 error 为 null，失败时 data 为 null。
     */
    public record Item(LocalDate date, DayInfo data, String error) {
        public static Item ok(LocalDate date, DayInfo data) {
            return new Item(date, data, null);
        }

        public static Item error(LocalDate date, String error) {
            return new Item(date, null, error);
        }
    }
}
