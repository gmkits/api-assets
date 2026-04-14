package com.github.gmkits.holiday.api.dto;

import java.util.List;

public class VersionInfo {

    private final String apiVersion;
    private final String dataVersion;
    private final List<String> regions;

    public VersionInfo(String apiVersion, String dataVersion, List<String> regions) {
        this.apiVersion = apiVersion;
        this.dataVersion = dataVersion;
        this.regions = regions;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getDataVersion() {
        return dataVersion;
    }

    public List<String> getRegions() {
        return regions;
    }
}
