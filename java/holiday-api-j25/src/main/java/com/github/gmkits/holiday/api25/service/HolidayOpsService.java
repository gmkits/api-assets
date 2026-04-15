package com.github.gmkits.holiday.api25.service;

import com.github.gmkits.holiday.api25.config.HolidayApi25Properties;
import com.github.gmkits.holiday.api25.dto.OperationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 运维动作：缓存清理、预热、manifest 重载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayOpsService {

    private final CacheManager holidayCacheManager;
    private final HolidayApi25Properties properties;
    private final CachedHolidayQueryService cachedHolidayQueryService;
    private final ObjectProvider<CachedHolidayQueryService> selfProvider;

    public OperationResult clearCaches(boolean reloadManifest) {
        for (String cacheName : holidayCacheManager.getCacheNames()) {
            Cache cache = holidayCacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        if (reloadManifest) {
            cachedHolidayQueryService.reloadManifest();
        }
        return OperationResult.builder()
                .operation("clearCaches")
                .message(reloadManifest ? "缓存已清空并重载 manifest" : "缓存已清空")
                .warmedKeys(ImmutableList.of())
                .build();
    }

    public OperationResult reloadManifest() {
        cachedHolidayQueryService.reloadManifest();
        return OperationResult.builder()
                .operation("reloadManifest")
                .message("manifest 已重新加载")
                .warmedKeys(ImmutableList.of())
                .build();
    }

    public OperationResult warmUp(List<String> regions, List<Integer> years, boolean includeCurrentAndNextYear) {
        Set<String> effectiveRegions = new LinkedHashSet<>();
        if (regions != null && !regions.isEmpty()) {
            effectiveRegions.addAll(regions);
        } else if (properties.getPreloadRegions() != null && !properties.getPreloadRegions().isEmpty()) {
            effectiveRegions.addAll(properties.getPreloadRegions());
        } else {
            effectiveRegions.add(properties.getDefaultRegion());
        }

        Set<Integer> effectiveYears = new LinkedHashSet<>();
        if (years != null && !years.isEmpty()) {
            effectiveYears.addAll(years);
        }
        if (properties.getPreloadYears() != null) {
            effectiveYears.addAll(properties.getPreloadYears());
        }
        boolean warmCurrent = includeCurrentAndNextYear || properties.isPreloadCurrentAndNextYear();
        if (warmCurrent) {
            int currentYear = Year.now().getValue();
            effectiveYears.add(currentYear);
            effectiveYears.add(currentYear + 1);
        }

        List<String> warmedKeys = new ArrayList<>();
        CachedHolidayQueryService proxy = selfProvider.getObject();
        for (String region : effectiveRegions) {
            for (Integer year : effectiveYears) {
                try {
                    proxy.getYear(region, year.intValue());
                    warmedKeys.add(region + ":" + year);
                } catch (Exception ex) {
                    log.debug("holiday warmup skipped for {}:{} - {}", region, year, ex.getMessage());
                }
            }
        }

        return OperationResult.builder()
                .operation("warmUp")
                .message("预热完成")
                .warmedKeys(warmedKeys)
                .build();
    }
}
