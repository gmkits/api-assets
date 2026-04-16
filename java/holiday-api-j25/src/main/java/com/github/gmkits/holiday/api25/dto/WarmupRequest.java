package com.github.gmkits.holiday.api25.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 预热请求参数。
 */
@Data
public class WarmupRequest {
    private List<String> regions = List.of();
    private List<Integer> years = List.of();
    private Boolean includeCurrentAndNextYear;

    public void setRegions(List<String> regions) {
        this.regions = toReadOnlyList(regions);
    }

    public void setYears(List<Integer> years) {
        this.years = toReadOnlyList(years);
    }

    private static <T> List<T> toReadOnlyList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
