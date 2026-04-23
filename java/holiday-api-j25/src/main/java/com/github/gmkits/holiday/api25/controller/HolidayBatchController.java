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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 批量查询接口（JDK 25 虚拟线程并行 fan-out）。
 *
 * <p>每次请求建一个 per-task 虚拟线程执行器（非 preview API），把每个日期 fork
 * 到独立虚拟线程；和 {@code StructuredTaskScope} 等价但不依赖 preview 特性。
 * 单次最多 100 个日期（参见 {@link BatchDayQueryRequest}），任一子任务失败
 * 不会影响其它结果。</p>
 *
 * <p>当 {@code java.util.concurrent.StructuredTaskScope} 在未来 JDK 版本中
 * 转正后，可平滑替换为 {@code StructuredTaskScope.open(Joiner.awaitAll())}。</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Tag(name = "holiday-batch", description = "节假日批量查询接口（虚拟线程并行）")
public class HolidayBatchController {

    private static final String DEFAULT_REGION = "CN";

    private final CachedHolidayQueryService cachedHolidayQueryService;

    @PostMapping("/days:batch")
    @Operation(summary = "批量按日期查询",
            description = "在虚拟线程上对每个日期 fan-out 并行查询，失败按条返回。")
    public ApiResponse<List<BatchDayQueryRequest.Item>> batchDays(
            @Valid @RequestBody BatchDayQueryRequest body,
            HttpServletRequest request) {
        if (body == null || body.getDates() == null || body.getDates().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_BATCH", "dates 不能为空");
        }
        String regionCode = body.getRegionCode() == null || body.getRegionCode().isBlank()
                ? DEFAULT_REGION : body.getRegionCode();

        List<LocalDate> dates = body.getDates();
        List<Callable<DayInfo>> tasks = new ArrayList<>(dates.size());
        for (LocalDate date : dates) {
            tasks.add(() -> cachedHolidayQueryService.getDay(regionCode, date));
        }

        List<Future<DayInfo>> futures;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            futures = executor.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "BATCH_INTERRUPTED",
                    "批量查询被中断");
        }

        List<BatchDayQueryRequest.Item> items = new ArrayList<>(dates.size());
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            Future<DayInfo> future = futures.get(i);
            try {
                items.add(BatchDayQueryRequest.Item.ok(date, future.get()));
            } catch (CancellationException ex) {
                items.add(BatchDayQueryRequest.Item.error(date, "已取消"));
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                items.add(BatchDayQueryRequest.Item.error(date,
                        cause == null ? ex.getMessage() : cause.getMessage()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                items.add(BatchDayQueryRequest.Item.error(date, "中断"));
            }
        }
        return ApiResponses.success(List.copyOf(items), request);
    }
}
