package com.github.gmkits.holiday.api25.controller;

import com.github.gmkits.holiday.api25.dto.ApiResponse;
import com.github.gmkits.holiday.api25.dto.ApiResponses;
import com.github.gmkits.holiday.api25.dto.BatchDayQueryRequest;
import com.github.gmkits.holiday.api25.exception.ApiException;
import com.github.gmkits.holiday.api25.service.CachedHolidayQueryService;
import com.github.gmkits.holiday.spec.DayInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量查询接口。
 *
 * <p>底层查询为纯内存操作，顺序批量查询比为每个日期创建虚拟线程开销更低。
 * 单次最多 100 个日期，任一查询失败按条返回。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Tag(name = "holiday-batch", description = "节假日批量查询接口")
public class HolidayBatchController {

    private static final String DEFAULT_REGION = "CN";

    private final CachedHolidayQueryService cachedHolidayQueryService;

    @PostMapping("/days:batch")
    @Operation(summary = "批量按日期查询",
            description = "一次查询多个日期，失败按条返回。")
    public ApiResponse<List<BatchDayQueryRequest.Item>> batchDays(
            @Valid @RequestBody BatchDayQueryRequest body,
            HttpServletRequest request) {
        if (body == null || body.getDates() == null || body.getDates().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_BATCH", "dates 不能为空");
        }
        String regionCode = body.getRegionCode() == null || body.getRegionCode().isBlank()
                ? DEFAULT_REGION : body.getRegionCode();

        List<LocalDate> dates = body.getDates();
        List<BatchDayQueryRequest.Item> items = new ArrayList<>(dates.size());
        for (LocalDate date : dates) {
            try {
                DayInfo day = cachedHolidayQueryService.getDay(regionCode, date);
                items.add(BatchDayQueryRequest.Item.ok(date, day));
            } catch (RuntimeException ex) {
                items.add(BatchDayQueryRequest.Item.error(date, ex.getMessage()));
            }
        }
        return ApiResponses.success(List.copyOf(items), request);
    }
}
