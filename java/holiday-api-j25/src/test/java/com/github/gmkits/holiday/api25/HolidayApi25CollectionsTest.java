package com.github.gmkits.holiday.api25;

import com.github.gmkits.holiday.api25.dto.OperationResult;
import com.github.gmkits.holiday.api25.dto.RegionInfo;
import com.github.gmkits.holiday.api25.dto.VersionPayload;
import com.github.gmkits.holiday.api25.dto.WarmupRequest;
import com.github.gmkits.holiday.api25.repository.ManifestRepository;
import com.github.gmkits.holiday.api25.service.CachedHolidayQueryService;
import com.github.gmkits.holiday.api25.service.HolidayOpsService;
import com.github.gmkits.holiday.spec.DayInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class HolidayApi25CollectionsTest {

    @Autowired
    private ManifestRepository manifestRepository;

    @Autowired
    private CachedHolidayQueryService cachedHolidayQueryService;

    @Autowired
    private HolidayOpsService holidayOpsService;

    @Test
    void manifestSupportedRegions_shouldReuseSnapshotList() {
        List<String> first = manifestRepository.getSupportedRegions();
        List<String> second = manifestRepository.getSupportedRegions();

        assertSame(first, second);
        assertThrows(UnsupportedOperationException.class, () -> first.add("TEST"));
    }

    @Test
    void responsePayloads_shouldExposeReadonlyCollections() {
        List<RegionInfo> regions = cachedHolidayQueryService.getRegions();
        VersionPayload version = cachedHolidayQueryService.getVersion();

        assertFalse(regions.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> regions.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> regions.get(0).getName().put("en-US", "Test"));
        assertThrows(UnsupportedOperationException.class, () -> version.getRegions().add("TEST"));
    }

    @Test
    void cachedQueryAndWarmupResults_shouldExposeReadonlyCollections() {
        List<DayInfo> year = cachedHolidayQueryService.getYear("CN", 2025);
        OperationResult result = holidayOpsService.warmUp(List.of("CN"), List.of(2025), false);

        assertFalse(year.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> year.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> result.getWarmedKeys().add("CN:2026"));
    }

    @Test
    void warmupRequest_shouldDefensivelyCopyCollections() {
        List<String> regions = new ArrayList<>(List.of("CN"));
        List<Integer> years = new ArrayList<>(List.of(2025));
        WarmupRequest request = new WarmupRequest();

        request.setRegions(regions);
        request.setYears(years);
        regions.add("TEST");
        years.add(2026);

        assertEquals(List.of("CN"), request.getRegions());
        assertEquals(List.of(2025), request.getYears());
        assertThrows(UnsupportedOperationException.class, () -> request.getRegions().add("HK"));
        assertThrows(UnsupportedOperationException.class, () -> request.getYears().add(2027));
    }
}
