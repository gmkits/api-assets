package com.github.gmkits.holiday.api25.repository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.MissingNode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private volatile ManifestSnapshot snapshot;

    public JsonNode getManifest() {
        return getSnapshot().manifest();
    }

    public JsonNode reloadManifest() {
        synchronized (lock) {
            snapshot = loadSnapshot();
            return snapshot.manifest();
        }
    }

    public JsonNode getBundleMetadataNode(String regionCode, int year) {
        JsonNode bundleNode = getSnapshot().findBundleMetadata(regionCode, year);
        if (bundleNode.isMissingNode() || bundleNode.isNull()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BUNDLE_NOT_FOUND",
                    "未找到地区 " + regionCode + " 在年份 " + year + " 的 bundle 元信息");
        }
        return bundleNode;
    }

    public List<String> getSupportedRegions() {
        return getSnapshot().supportedRegions();
    }

    public String getSpecVersion() {
        return getSnapshot().specVersion();
    }

    public String getBundleFormatVersion() {
        return getSnapshot().bundleFormatVersion();
    }

    public String getPublishedAt() {
        return getSnapshot().publishedAt();
    }

    private ManifestSnapshot getSnapshot() {
        ManifestSnapshot current = snapshot;
        if (current != null) {
            return current;
        }
        synchronized (lock) {
            if (snapshot == null) {
                snapshot = loadSnapshot();
            }
            return snapshot;
        }
    }

    private ManifestSnapshot loadSnapshot() {
        return ManifestSnapshot.from(loadManifestNode());
    }

    private JsonNode loadManifestNode() {
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

    private static int parseBundleYear(String yearText) {
        try {
            return Integer.parseInt(yearText);
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MANIFEST_READ_FAILED",
                    "manifest bundle 年份无效: " + yearText);
        }
    }

    private static final class ManifestSnapshot {
        private final JsonNode manifest;
        private final List<String> supportedRegions;
        private final Map<String, Map<Integer, JsonNode>> bundleMetadataIndex;
        private final String specVersion;
        private final String bundleFormatVersion;
        private final String publishedAt;

        private ManifestSnapshot(JsonNode manifest,
                                 List<String> supportedRegions,
                                 Map<String, Map<Integer, JsonNode>> bundleMetadataIndex) {
            this.manifest = manifest;
            this.supportedRegions = supportedRegions;
            this.bundleMetadataIndex = bundleMetadataIndex;
            this.specVersion = manifest.path("specVersion").asText("");
            this.bundleFormatVersion = manifest.path("bundleFormatVersion").asText("");
            this.publishedAt = manifest.path("publishedAt").asText("");
        }

        static ManifestSnapshot from(JsonNode manifest) {
            JsonNode bundlesNode = manifest.path("bundles");
            List<String> supportedRegions = new ArrayList<>(bundlesNode.size());
            Map<String, Map<Integer, JsonNode>> bundleMetadataIndex = new HashMap<>(bundlesNode.size());
            for (String regionCode : bundlesNode.propertyNames()) {
                JsonNode regionBundles = bundlesNode.path(regionCode);
                supportedRegions.add(regionCode);
                Map<Integer, JsonNode> metadataByYear = new HashMap<>(regionBundles.size());
                for (String yearText : regionBundles.propertyNames()) {
                    metadataByYear.put(parseBundleYear(yearText), regionBundles.path(yearText));
                }
                bundleMetadataIndex.put(regionCode, Map.copyOf(metadataByYear));
            }
            return new ManifestSnapshot(
                    manifest,
                    List.copyOf(supportedRegions),
                    Map.copyOf(bundleMetadataIndex));
        }

        JsonNode manifest() {
            return manifest;
        }

        List<String> supportedRegions() {
            return supportedRegions;
        }

        String specVersion() {
            return specVersion;
        }

        String bundleFormatVersion() {
            return bundleFormatVersion;
        }

        String publishedAt() {
            return publishedAt;
        }

        JsonNode findBundleMetadata(String regionCode, int year) {
            Map<Integer, JsonNode> metadataByYear = bundleMetadataIndex.get(regionCode);
            if (metadataByYear == null) {
                return MissingNode.getInstance();
            }
            JsonNode metadata = metadataByYear.get(year);
            return metadata == null ? MissingNode.getInstance() : metadata;
        }
    }
}
