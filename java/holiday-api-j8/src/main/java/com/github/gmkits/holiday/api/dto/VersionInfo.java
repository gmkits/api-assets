package com.github.gmkits.holiday.api.dto;

import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public class VersionInfo {

    String apiVersion;
    String dataVersion;
    List<String> regions;

    public VersionInfo(String apiVersion, String dataVersion, List<String> regions) {
        this.apiVersion = apiVersion;
        this.dataVersion = dataVersion;
        this.regions = regions == null || regions.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(regions));
    }
}
