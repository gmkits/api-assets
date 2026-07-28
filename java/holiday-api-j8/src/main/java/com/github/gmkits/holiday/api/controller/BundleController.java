package com.github.gmkits.holiday.api.controller;

import com.github.gmkits.holiday.api.HolidayProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 提供离线 manifest 与单年 bundle 下载。
 */
@RestController
@RequestMapping("/api/v1")
public class BundleController {

    private final HolidayProperties properties;

    /**
     * 创建 bundle 控制器。
     *
     * @param properties 资产路径配置
     */
    public BundleController(HolidayProperties properties) {
        this.properties = properties;
    }

    /**
     * 返回当前离线 bundle manifest。
     *
     * @return manifest JSON；不存在时返回 404
     * @throws IOException 读取外部或 classpath 资源失败时抛出
     */
    @GetMapping(value = "/manifest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getManifest() throws IOException {
        Path external = externalManifest();
        if (external != null && Files.isRegularFile(external)) {
            return ResponseEntity.ok(new String(Files.readAllBytes(external), StandardCharsets.UTF_8));
        }
        if (external != null && !properties.isClasspathFallback()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new ClassPathResource(
                "cn-holiday-kit/assets/holidays/manifest.json");
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream in = resource.getInputStream()) {
            String content = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        }
    }

    /**
     * 下载指定区域与年份的二进制 bundle。
     *
     * @param region 区域代码
     * @param year 公历年份
     * @return bundle 字节；不存在时返回 404
     * @throws IOException 读取资源失败时抛出
     */
    @GetMapping("/bundle/{region}/{year}")
    public ResponseEntity<byte[]> getBundle(
            @PathVariable String region,
            @PathVariable int year) throws IOException {
        Path external = externalBundle(region, year);
        if (external != null && Files.isRegularFile(external)) {
            return bundleResponse(Files.readAllBytes(external), year);
        }
        if (external != null && !properties.isClasspathFallback()) {
            return ResponseEntity.notFound().build();
        }
        String path = String.format(
                "cn-holiday-kit/assets/holidays/bundles/%s/%d.hday", region, year);
        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream in = resource.getInputStream()) {
            return bundleResponse(StreamUtils.copyToByteArray(in), year);
        }
    }

    private Path externalManifest() {
        if (!hasText(properties.getAssetPath())) return null;
        return Paths.get(properties.getAssetPath()).resolve("holidays").resolve("manifest.json");
    }

    private Path externalBundle(String region, int year) {
        if (hasText(properties.getAssetPath())) {
            return Paths.get(properties.getAssetPath()).resolve("holidays")
                    .resolve("bundles").resolve(region).resolve(year + ".hday");
        }
        return null;
    }

    private static ResponseEntity<byte[]> bundleResponse(byte[] bytes, int year) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + year + ".hday\"");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
