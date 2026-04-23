package com.github.gmkits.holiday.api25.service;

import tools.jackson.databind.JsonNode;
import com.github.gmkits.holiday.api25.config.HolidayApi25Properties;
import com.github.gmkits.holiday.api25.dto.BundleMetadataPayload;
import com.github.gmkits.holiday.api25.dto.RegionInfo;
import com.github.gmkits.holiday.api25.dto.VersionPayload;
import com.github.gmkits.holiday.api25.dto.WorkdayCountPayload;
import com.github.gmkits.holiday.api25.exception.ApiException;
import com.github.gmkits.holiday.api25.repository.ManifestRepository;
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.spec.DayInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 查询编排与缓存入口。
 */
@Service
@RequiredArgsConstructor
public class CachedHolidayQueryService {

    private final HolidayService holidayService;
    private final ManifestRepository manifestRepository;
    private final HolidayApi25Properties properties;

    @Cacheable(cacheNames = "dayInfo", key = "#regionCode + ':' + #date")
    public DayInfo getDay(String regionCode, LocalDate date) {
        DayInfo info = holidayService.getDayInfo(regionCode, date);
        if (info == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DATE_NOT_FOUND",
                    "未找到日期 " + date + " 的节假日数据");
        }
        return info;
    }

    public List<DayInfo> getRange(String regionCode, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "from 不能晚于 to");
        }
        List<DayInfo> days = holidayService.getRange(regionCode, from, to);
        if (days.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RANGE_NOT_FOUND", "指定区间没有可用数据");
        }
        return readOnlyDays(days);
    }

    @Cacheable(cacheNames = "yearInfo", key = "#regionCode + ':' + #year")
    public List<DayInfo> getYear(String regionCode, int year) {
        List<DayInfo> days = holidayService.getYear(regionCode, year);
        if (days.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "YEAR_NOT_FOUND", "未找到年份 " + year + " 的节假日数据");
        }
        return readOnlyDays(days);
    }

    public JsonNode getManifest() {
        return manifestRepository.getManifest();
    }

    /** 当前 manifest 快照的弱 ETag，可用于 HTTP 条件请求。 */
    public String getManifestETag() {
        return manifestRepository.getETag();
    }

    public JsonNode reloadManifest() {
        return manifestRepository.reloadManifest();
    }

    public List<RegionInfo> getRegions() {
        return manifestRepository.getSupportedRegions().stream()
                .map(CachedHolidayQueryService::toRegionInfo)
                .toList();
    }

    public VersionPayload getVersion() {
        return VersionPayload.builder()
                .apiVersion(properties.getApiVersion())
                .specVersion(manifestRepository.getSpecVersion())
                .bundleFormatVersion(manifestRepository.getBundleFormatVersion())
                .publishedAt(manifestRepository.getPublishedAt())
                .regions(manifestRepository.getSupportedRegions())
                .build();
    }

    @Cacheable(cacheNames = "bundleMetadata", key = "#regionCode + ':' + #year")
    public BundleMetadataPayload getBundleMetadata(String regionCode, int year) {
        JsonNode metadata = manifestRepository.getBundleMetadataNode(regionCode, year);
        return BundleMetadataPayload.builder()
                .regionCode(regionCode)
                .year(year)
                .file(metadata.path("file").asText())
                .sha256(metadata.path("sha256").asText())
                .crc32(metadata.path("crc32").asText())
                .sourceVersion(metadata.path("sourceVersion").asText())
                .size(metadata.path("size").asLong())
                .build();
    }

    private static final ImmutableMap<String, String> CN_REGION_NAMES = ImmutableMap.of(
            "zh-CN", "中国大陆",
            "en-US", "Mainland China");

    private static Map<String, String> resolveRegionName(String regionCode) {
        if ("CN".equals(regionCode)) {
            return CN_REGION_NAMES;
        }
        return ImmutableMap.of("zh-CN", regionCode);
    }

    private static RegionInfo toRegionInfo(String regionCode) {
        return RegionInfo.builder()
                .code(regionCode)
                .name(resolveRegionName(regionCode))
                .build();
    }

    /**
     * 查询指定月份。
     */
    public List<DayInfo> getMonth(String regionCode, int year, int month) {
        List<DayInfo> days = holidayService.getMonth(regionCode, year, month);
        if (days.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MONTH_NOT_FOUND",
                    "未找到 " + year + "-" + month + " 的节假日数据");
        }
        return readOnlyDays(days);
    }

    /**
     * 统计工作日。
     */
    public WorkdayCountPayload countWorkdays(String regionCode, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "from 不能晚于 to");
        }
        int workdays = holidayService.countWorkdays(regionCode, from, to);
        int totalDays = (int) (to.toEpochDay() - from.toEpochDay()) + 1;
        return WorkdayCountPayload.builder()
                .from(from.toString())
                .to(to.toString())
                .workdays(workdays)
                .totalDays(totalDays)
                .holidays(totalDays - workdays)
                .build();
    }

    /**
     * 查找下一个法定节假日。
     */
    public DayInfo getNextHoliday(String regionCode, LocalDate from) {
        DayInfo info = holidayService.getNextHoliday(regionCode, from);
        if (info == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "HOLIDAY_NOT_FOUND",
                    "从 " + from + " 起未找到下一个法定节假日");
        }
        return info;
    }

    private static List<DayInfo> readOnlyDays(List<DayInfo> days) {
        if (days.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(days));
    }
}
