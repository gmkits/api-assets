package com.github.gmkits.holiday.api25.dto;

import lombok.Data;

import java.util.List;

/**
 * 预热请求参数。
 */
@Data
public class WarmupRequest {
    private List<String> regions;
    private List<Integer> years;
    private Boolean includeCurrentAndNextYear;
}
