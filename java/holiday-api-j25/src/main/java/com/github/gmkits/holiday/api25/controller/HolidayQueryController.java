package com.github.gmkits.holiday.api25.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.gmkits.holiday.api25.dto.ApiResponse;
import com.github.gmkits.holiday.api25.dto.ApiResponses;
import com.github.gmkits.holiday.api25.dto.BundleMetadataPayload;
import com.github.gmkits.holiday.api25.dto.RegionInfo;
import com.github.gmkits.holiday.api25.dto.VersionPayload;
import com.github.gmkits.holiday.api25.service.CachedHolidayQueryService;
import com.github.gmkits.holiday.spec.DayInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 查询接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
@Tag(name = "holiday-query", description = "节假日查询接口")
public class HolidayQueryController {

    private final CachedHolidayQueryService cachedHolidayQueryService;

    @GetMapping("/day")
    @Operation(summary = "查询单日")
    public ApiResponse<DayInfo> getDay(
            @RequestParam(defaultValue = "CN") @Pattern(regexp = "^[A-Z0-9_-]{2,32}$") String regionCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        return ApiResponses.success(cachedHolidayQueryService.getDay(regionCode, date), request);
    }

    @GetMapping("/range")
    @Operation(summary = "查询区间")
    public ApiResponse<List<DayInfo>> getRange(
            @RequestParam(defaultValue = "CN") @Pattern(regexp = "^[A-Z0-9_-]{2,32}$") String regionCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {
        return ApiResponses.success(cachedHolidayQueryService.getRange(regionCode, from, to), request);
    }

    @GetMapping("/year")
    @Operation(summary = "查询整年")
    public ApiResponse<List<DayInfo>> getYear(
            @RequestParam(defaultValue = "CN") @Pattern(regexp = "^[A-Z0-9_-]{2,32}$") String regionCode,
            @RequestParam @Min(1900) @Max(3000) int year,
            HttpServletRequest request) {
        return ApiResponses.success(cachedHolidayQueryService.getYear(regionCode, year), request);
    }

    @GetMapping("/regions")
    @Operation(summary = "查询支持地区")
    public ApiResponse<List<RegionInfo>> getRegions(HttpServletRequest request) {
        return ApiResponses.success(cachedHolidayQueryService.getRegions(), request);
    }

    @GetMapping("/version")
    @Operation(summary = "查询版本信息")
    public ApiResponse<VersionPayload> getVersion(HttpServletRequest request) {
        return ApiResponses.success(cachedHolidayQueryService.getVersion(), request);
    }

    @GetMapping("/manifest")
    @Operation(summary = "读取 manifest")
    public ApiResponse<JsonNode> getManifest(HttpServletRequest request) {
        return ApiResponses.success(cachedHolidayQueryService.getManifest(), request);
    }

    @GetMapping("/bundles/{regionCode}/{year}/metadata")
    @Operation(summary = "查询 bundle 元信息")
    public ApiResponse<BundleMetadataPayload> getBundleMetadata(
            @PathVariable @Pattern(regexp = "^[A-Z0-9_-]{2,32}$") String regionCode,
            @PathVariable @Min(1900) @Max(3000) int year,
            HttpServletRequest request) {
        return ApiResponses.success(cachedHolidayQueryService.getBundleMetadata(regionCode, year), request);
    }
}
