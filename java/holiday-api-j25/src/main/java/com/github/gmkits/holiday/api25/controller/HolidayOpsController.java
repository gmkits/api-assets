package com.github.gmkits.holiday.api25.controller;

import com.github.gmkits.holiday.api25.dto.ApiResponse;
import com.github.gmkits.holiday.api25.dto.ApiResponses;
import com.github.gmkits.holiday.api25.dto.OperationResult;
import com.github.gmkits.holiday.api25.dto.WarmupRequest;
import com.github.gmkits.holiday.api25.service.HolidayOpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/ops")
@Tag(name = "holiday-ops", description = "缓存和 manifest 运维接口")
public class HolidayOpsController {

    private final HolidayOpsService holidayOpsService;

    @PostMapping("/cache/clear")
    @Operation(summary = "清空缓存")
    public ApiResponse<OperationResult> clearCaches(
            @RequestParam(defaultValue = "false") boolean reloadManifest,
            HttpServletRequest request) {
        return ApiResponses.success(holidayOpsService.clearCaches(reloadManifest), request);
    }

    @PostMapping("/cache/warmup")
    @Operation(summary = "预热缓存")
    public ApiResponse<OperationResult> warmUp(
            @RequestBody(required = false) WarmupRequest warmupRequest,
            HttpServletRequest request) {
        boolean includeCurrentAndNextYear = warmupRequest == null || warmupRequest.getIncludeCurrentAndNextYear() == null
                ? true : warmupRequest.getIncludeCurrentAndNextYear().booleanValue();
        return ApiResponses.success(
                holidayOpsService.warmUp(
                        warmupRequest == null ? null : warmupRequest.getRegions(),
                        warmupRequest == null ? null : warmupRequest.getYears(),
                        includeCurrentAndNextYear),
                request);
    }

    @PostMapping("/manifest/reload")
    @Operation(summary = "重载 manifest")
    public ApiResponse<OperationResult> reloadManifest(HttpServletRequest request) {
        return ApiResponses.success(holidayOpsService.reloadManifest(), request);
    }
}
