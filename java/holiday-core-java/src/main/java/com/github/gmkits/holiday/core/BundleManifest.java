package com.github.gmkits.holiday.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 发布 manifest 中与运行时加载有关的最小只读索引。
 *
 * <p>项目不为这一份固定结构的 JSON 引入序列化依赖。解析器只提取
 * {@code bundles -> region -> year -> sha256}，其余发布字段保持可扩展。</p>
 */
final class BundleManifest {

    private static final Pattern VERSION = Pattern.compile(
            "\"bundleFormatVersion\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REGION = Pattern.compile(
            "\"([A-Z]{2}(?:-[A-Z0-9]{1,8})*)\"\\s*:\\s*\\{");
    private static final Pattern YEAR = Pattern.compile(
            "\"([0-9]{1,4})\"\\s*:\\s*\\{");
    private static final Pattern SHA256 = Pattern.compile(
            "\"sha256\"\\s*:\\s*\"([0-9a-fA-F]{64})\"");

    private final Map<String, String> hashes;

    private BundleManifest(Map<String, String> hashes) {
        this.hashes = Collections.unmodifiableMap(hashes);
    }

    static BundleManifest filesystem(Path bundleRoot) throws IOException {
        if (bundleRoot == null || bundleRoot.getParent() == null) return null;
        Path manifest = bundleRoot.getParent().resolve("manifest.json");
        if (!Files.isRegularFile(manifest)) return null;
        return parse(new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8));
    }

    static BundleManifest classpath(ClassLoader loader) throws IOException {
        String[] resources = {
            "cn-holiday-kit/assets/holidays/manifest.json",
            "manifest.json"
        };
        for (String resource : resources) {
            try (InputStream input = loader.getResourceAsStream(resource)) {
                if (input != null) {
                    return parse(new String(readAllBytes(input), StandardCharsets.UTF_8));
                }
            }
        }
        return null;
    }

    boolean contains(String region, int year) {
        return hashes.containsKey(key(region, year));
    }

    int[] yearRange(String region) {
        String prefix = region + "/";
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (String bundleKey : hashes.keySet()) {
            if (!bundleKey.startsWith(prefix)) continue;
            int year = Integer.parseInt(bundleKey.substring(prefix.length()));
            minimum = Math.min(minimum, year);
            maximum = Math.max(maximum, year);
        }
        return minimum == Integer.MAX_VALUE ? null : new int[] {minimum, maximum};
    }

    void verify(String region, int year, byte[] data) throws IOException {
        String expected = hashes.get(key(region, year));
        if (expected == null) {
            throw new IOException("Bundle is not declared by manifest: "
                    + region + "/" + year);
        }
        String actual = sha256(data);
        if (!expected.equalsIgnoreCase(actual)) {
            throw new IOException("Bundle SHA-256 mismatch for " + region + "/" + year
                    + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static BundleManifest parse(String json) throws IOException {
        Matcher version = VERSION.matcher(json);
        if (!version.find() || !"2".equals(version.group(1))) {
            throw new IOException("Unsupported or missing bundleFormatVersion");
        }
        int bundlesKey = json.indexOf("\"bundles\"");
        int bundlesStart = bundlesKey < 0 ? -1 : json.indexOf('{', bundlesKey);
        if (bundlesStart < 0) throw new IOException("Manifest has no bundles object");
        int bundlesEnd = matchingBrace(json, bundlesStart);

        Map<String, String> hashes = new HashMap<>();
        Matcher regions = REGION.matcher(json);
        regions.region(bundlesStart + 1, bundlesEnd);
        while (regions.find()) {
            String region = regions.group(1);
            int regionStart = json.indexOf('{', regions.start());
            int regionEnd = matchingBrace(json, regionStart);
            Matcher years = YEAR.matcher(json);
            years.region(regionStart + 1, regionEnd);
            while (years.find()) {
                int year = Integer.parseInt(years.group(1));
                int yearStart = json.indexOf('{', years.start());
                int yearEnd = matchingBrace(json, yearStart);
                Matcher sha = SHA256.matcher(json);
                sha.region(yearStart + 1, yearEnd);
                if (!sha.find()) {
                    throw new IOException(
                            "Manifest entry has no SHA-256: " + region + "/" + year);
                }
                String previous = hashes.put(key(region, year), sha.group(1).toLowerCase());
                if (previous != null) {
                    throw new IOException(
                            "Duplicate manifest entry: " + region + "/" + year);
                }
            }
            regions.region(regionEnd + 1, bundlesEnd);
        }
        if (hashes.isEmpty()) throw new IOException("Manifest contains no bundles");
        return new BundleManifest(hashes);
    }

    private static int matchingBrace(String text, int start) throws IOException {
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char value = text.charAt(index);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (value == '\\') {
                    escaped = true;
                } else if (value == '"') {
                    quoted = false;
                }
                continue;
            }
            if (value == '"') {
                quoted = true;
            } else if (value == '{') {
                depth++;
            } else if (value == '}' && --depth == 0) {
                return index;
            }
        }
        throw new IOException("Unclosed JSON object in manifest");
    }

    private static String sha256(byte[] data) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder value = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                value.append(String.format("%02x", item & 0xff));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }

    private static String key(String region, int year) {
        return region + "/" + year;
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }
}
