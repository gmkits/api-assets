package com.github.gmkits.holiday.api25.repository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.github.gmkits.holiday.api25.config.HolidayApi25Properties;
import com.github.gmkits.holiday.api25.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * manifest 加载与缓存。
 */
@Repository
@RequiredArgsConstructor
public class ManifestRepository {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final HolidayApi25Properties properties;
    private final Object lock = new Object();

    private volatile JsonNode manifest;

    public JsonNode getManifest() {
        JsonNode current = manifest;
        if (current != null) {
            return current;
        }
        synchronized (lock) {
            if (manifest == null) {
                manifest = loadManifest();
            }
            return manifest;
        }
    }

    public JsonNode reloadManifest() {
        synchronized (lock) {
            manifest = loadManifest();
            return manifest;
        }
    }

    public JsonNode getBundleMetadataNode(String regionCode, int year) {
        JsonNode bundleNode = getManifest()
                .path("bundles")
                .path(regionCode)
                .path(String.valueOf(year));
        if (bundleNode.isMissingNode() || bundleNode.isNull()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BUNDLE_NOT_FOUND",
                    "未找到地区 " + regionCode + " 在年份 " + year + " 的 bundle 元信息");
        }
        return bundleNode;
    }

    public List<String> getSupportedRegions() {
        JsonNode bundlesNode = getManifest().path("bundles");
        List<String> regions = new ArrayList<>();
        for (String name : bundlesNode.propertyNames()) {
            regions.add(name);
        }
        return regions;
    }

    private JsonNode loadManifest() {
        Resource resource = resourceLoader.getResource(properties.getManifestLocation());
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MANIFEST_NOT_FOUND",
                    "未找到 manifest: " + properties.getManifestLocation());
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MANIFEST_READ_FAILED",
                    "读取 manifest 失败: " + ex.getMessage());
        }
    }
}
