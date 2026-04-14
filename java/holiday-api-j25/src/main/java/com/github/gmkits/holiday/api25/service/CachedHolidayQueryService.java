package com.github.gmkits.holiday.api25.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.gmkits.holiday.api25.config.HolidayApi25Properties;
import com.github.gmkits.holiday.api25.dto.BundleMetadataPayload;
import com.github.gmkits.holiday.api25.dto.RegionInfo;
import com.github.gmkits.holiday.api25.dto.VersionPayload;
import com.github.gmkits.holiday.api25.exception.ApiException;
import com.github.gmkits.holiday.api25.repository.ManifestRepository;
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.spec.DayInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        return days;
    }

    @Cacheable(cacheNames = "yearInfo", key = "#regionCode + ':' + #year")
    public List<DayInfo> getYear(String regionCode, int year) {
        List<DayInfo> days = holidayService.getYear(regionCode, year);
        if (days.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "YEAR_NOT_FOUND", "未找到年份 " + year + " 的节假日数据");
        }
        return days;
    }

    public JsonNode getManifest() {
        return manifestRepository.getManifest();
    }

    public JsonNode reloadManifest() {
        return manifestRepository.reloadManifest();
    }

    public List<RegionInfo> getRegions() {
        List<RegionInfo> result = new ArrayList<RegionInfo>();
        for (String region : manifestRepository.getSupportedRegions()) {
            result.add(RegionInfo.builder()
                    .code(region)
                    .name(resolveRegionName(region))
                    .build());
        }
        return result;
    }

    public VersionPayload getVersion() {
        JsonNode manifest = manifestRepository.getManifest();
        return VersionPayload.builder()
                .apiVersion(properties.getApiVersion())
                .specVersion(manifest.path("specVersion").asText(""))
                .bundleFormatVersion(manifest.path("bundleFormatVersion").asText(""))
                .publishedAt(manifest.path("publishedAt").asText(""))
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

    private Map<String, String> resolveRegionName(String regionCode) {
        if ("CN".equals(regionCode)) {
            Map<String, String> names = new LinkedHashMap<String, String>();
            names.put("zh-CN", "中国大陆");
            names.put("en-US", "Mainland China");
            return names;
        }
        return Collections.singletonMap("zh-CN", regionCode);
    }
}
