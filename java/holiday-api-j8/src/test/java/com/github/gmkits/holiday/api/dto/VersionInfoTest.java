package com.github.gmkits.holiday.api.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionInfoTest {

    @Test
    void regions_shouldBeCopiedAsReadonlySnapshot() {
        List<String> regions = new ArrayList<>(Arrays.asList("CN"));
        VersionInfo versionInfo = new VersionInfo("1.0.0", "1.0.0-SNAPSHOT", regions);

        regions.add("TEST");

        assertEquals(Arrays.asList("CN"), versionInfo.getRegions());
        assertThrows(UnsupportedOperationException.class, () -> versionInfo.getRegions().add("HK"));
    }
}
