package com.github.gmkits.holiday.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * API 返回的不可变版本与数据覆盖信息。
 */
public final class VersionInfo {

    private final String apiVersion;
    private final String dataVersion;
    private final List<String> regions;

    /**
     * 创建版本信息。
     *
     * @param apiVersion API 版本
     * @param dataVersion 离线数据版本
     * @param regions 支持的区域代码；构造时执行防御性复制
     */
    public VersionInfo(String apiVersion, String dataVersion, List<String> regions) {
        this.apiVersion = apiVersion;
        this.dataVersion = dataVersion;
        this.regions = regions == null || regions.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(regions));
    }

    /**
     * 返回 API 版本。
     *
     * @return API 版本
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * 返回离线数据版本。
     *
     * @return 离线数据版本
     */
    public String getDataVersion() {
        return dataVersion;
    }

    /**
     * 返回支持的区域代码。
     *
     * @return 不可变区域代码列表
     */
    public List<String> getRegions() {
        return regions;
    }

    /**
     * 按三个响应字段比较版本信息。
     *
     * @param other 待比较对象
     * @return 三个字段相同时返回 {@code true}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VersionInfo)) return false;
        VersionInfo that = (VersionInfo) other;
        return Objects.equals(apiVersion, that.apiVersion)
                && Objects.equals(dataVersion, that.dataVersion)
                && Objects.equals(regions, that.regions);
    }

    /**
     * 返回三个响应字段的组合哈希值。
     *
     * @return 组合哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, dataVersion, regions);
    }

    /**
     * 返回便于日志查看的版本信息。
     *
     * @return 版本信息字符串
     */
    @Override
    public String toString() {
        return "VersionInfo{apiVersion='" + apiVersion + "', dataVersion='"
                + dataVersion + "', regions=" + regions + "}";
    }
}
