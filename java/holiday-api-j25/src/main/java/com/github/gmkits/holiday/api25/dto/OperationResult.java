package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 运维操作结果。
 */
@Getter
@Builder
public class OperationResult {
    private final String operation;
    private final String message;
    private final List<String> warmedKeys;
}
