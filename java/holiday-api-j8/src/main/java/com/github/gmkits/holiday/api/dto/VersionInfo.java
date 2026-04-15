package com.github.gmkits.holiday.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class VersionInfo {

    private final String apiVersion;
    private final String dataVersion;
    private final List<String> regions;
}
