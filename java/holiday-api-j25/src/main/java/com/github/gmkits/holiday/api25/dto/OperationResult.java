package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 运维操作结果。
 */
@Value
public class OperationResult {
    String operation;
    String message;
    List<String> warmedKeys;

    @Builder
    private OperationResult(String operation, String message, List<String> warmedKeys) {
        this.operation = operation;
        this.message = message;
        this.warmedKeys = warmedKeys == null ? List.of() : List.copyOf(warmedKeys);
    }
}
