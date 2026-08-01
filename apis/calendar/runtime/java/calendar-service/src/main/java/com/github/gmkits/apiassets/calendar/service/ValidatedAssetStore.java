package com.github.gmkits.apiassets.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动阶段读取并验证镜像中的日期资产，下载接口只返回这里保存的已验证字节。
 */
@Component
public final class ValidatedAssetStore {

    private static final String CLASSPATH_ROOT = "api-assets/calendar/";

    private final Map<String, Asset> assets;
    private final String region;
    private final int holidayStartYear;
    private final int holidayEndYear;
    private final int lunarStartYear;
    private final int lunarEndYear;
    private final int solarTermStartYear = 1901;
    private final int solarTermEndYear = 2100;
    private final String dataVersion;
    private final String generatedAt;

    public ValidatedAssetStore(CalendarProperties properties, ObjectMapper mapper) {
        try {
            Path externalRoot = properties.usesExternalAssets()
                    ? Path.of(properties.getAssetPath()).toAbsolutePath().normalize()
                    : null;
            Map<String, Asset> loaded = new LinkedHashMap<>();

            Asset rootManifest = load(externalRoot, "manifest.json");
            JsonNode root = mapper.readTree(rootManifest.bytes());
            require(root.path("formatVersion").asInt() == 2,
                    "不支持的日期资产 manifest 格式");
            require(properties.getReleaseVersion().equals(requiredText(root, "releaseVersion")),
                    "服务版本与日期资产版本不一致");

            JsonNode calendar = root.path("calendar").path("data");
            String calendarPath = requiredText(calendar, "path");
            Asset calendarAsset = load(externalRoot, calendarPath);
            verify(calendarAsset, requiredText(calendar, "sha256"));
            verifySize(calendarAsset, calendar.path("bytes").asInt(-1), calendarPath);
            lunarStartYear = calendar.path("startYear").asInt();
            lunarEndYear = calendar.path("endYear").asInt();
            require(lunarStartYear == 1900 && lunarEndYear == 2100,
                    "calendar.cdat 农历覆盖范围不完整");

            JsonNode holidayRoot = root.path("holidays");
            region = requiredText(holidayRoot, "region");
            String holidayManifestPath = requiredText(holidayRoot.path("manifest"), "path");
            Asset holidayManifest = load(externalRoot, holidayManifestPath);
            verify(holidayManifest, requiredText(holidayRoot.path("manifest"), "sha256"));
            verifySize(holidayManifest,
                    holidayRoot.path("manifest").path("bytes").asInt(-1),
                    holidayManifestPath);
            JsonNode holidayJson = mapper.readTree(holidayManifest.bytes());
            JsonNode bundles = holidayJson.path("bundles").path(region);
            require(bundles.isObject() && !bundles.isEmpty(), "节假日 manifest 没有可用年度");

            int minimum = Integer.MAX_VALUE;
            int maximum = Integer.MIN_VALUE;
            String latestVersion = "unknown";
            String bundleRoot = requiredText(holidayRoot, "bundleRoot");
            var fields = bundles.properties().iterator();
            while (fields.hasNext()) {
                var entry = fields.next();
                int year = Integer.parseInt(entry.getKey());
                JsonNode descriptor = entry.getValue();
                String relative = bundleRoot + "/" + requiredText(descriptor, "file");
                Asset bundle = load(externalRoot, relative);
                verify(bundle, requiredText(descriptor, "sha256"));
                verifySize(bundle, descriptor.path("size").asInt(-1), relative);
                loaded.put(relative, bundle);
                minimum = Math.min(minimum, year);
                if (year > maximum) {
                    maximum = year;
                    latestVersion = descriptor.path("sourceVersion").asText("unknown");
                }
            }
            holidayStartYear = minimum;
            holidayEndYear = maximum;
            require(holidayStartYear == holidayRoot.path("manifest").path("startYear").asInt()
                            && holidayEndYear == holidayRoot.path("manifest").path("endYear").asInt(),
                    "节假日 manifest 覆盖范围不一致");

            dataVersion = latestVersion;
            generatedAt = root.path("generatedAt").asText();
            loaded.put("manifest.json", rootManifest);
            loaded.put(calendarPath, calendarAsset);
            loaded.put(holidayManifestPath, holidayManifest);
            assets = Collections.unmodifiableMap(loaded);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("日期资产启动校验失败: " + exception.getMessage(), exception);
        }
    }

    public Asset requireAsset(String path) {
        Asset asset = assets.get(path);
        if (asset == null) {
            throw ApiException.notFound("ASSET_NOT_FOUND", "找不到日期资产: " + path);
        }
        return asset;
    }

    public String region() { return region; }
    public int holidayStartYear() { return holidayStartYear; }
    public int holidayEndYear() { return holidayEndYear; }
    public int lunarStartYear() { return lunarStartYear; }
    public int lunarEndYear() { return lunarEndYear; }
    public int solarTermStartYear() { return solarTermStartYear; }
    public int solarTermEndYear() { return solarTermEndYear; }
    public String dataVersion() { return dataVersion; }
    public String generatedAt() { return generatedAt; }

    private static Asset load(Path root, String relative) throws IOException {
        require(relative != null && !relative.isBlank() && !relative.startsWith("/")
                        && !relative.contains(".."),
                "非法资产路径: " + relative);
        byte[] bytes;
        if (root != null) {
            Path path = root.resolve(relative).normalize();
            require(path.startsWith(root) && Files.isRegularFile(path),
                    "外部资产不存在: " + relative);
            bytes = Files.readAllBytes(path);
        } else {
            ClassPathResource resource = new ClassPathResource(CLASSPATH_ROOT + relative);
            require(resource.exists(), "内置资产不存在: " + relative);
            try (InputStream input = resource.getInputStream()) {
                bytes = input.readAllBytes();
            }
        }
        return new Asset(bytes, sha256(bytes));
    }

    private static void verify(Asset asset, String expected) {
        require(asset.sha256().equalsIgnoreCase(expected),
                "资产 SHA-256 不匹配，expected=" + expected + ", actual=" + asset.sha256());
    }

    private static void verifySize(Asset asset, int expected, String path) {
        require(expected == asset.size(), "资产长度不匹配: " + path);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText();
        require(!value.isBlank(), "manifest 缺少字段: " + field);
        return value;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    /** 已完成哈希校验的不可变资产。 */
    public record Asset(byte[] bytes, String sha256) {
        public Asset {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        public int size() {
            return bytes.length;
        }
    }
}
