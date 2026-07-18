package com.github.gmkits.holiday.api;

import com.github.gmkits.holiday.api.controller.BundleController;
import com.github.gmkits.holiday.spring.HolidayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BundleControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void servesReplaceableExternalAssets() throws Exception {
        Path holidays = tempDir.resolve("holidays");
        Path bundle = holidays.resolve("bundles").resolve("CN").resolve("2026.hday");
        Files.createDirectories(bundle.getParent());
        Files.write(holidays.resolve("manifest.json"),
                "{\"source\":\"external\"}".getBytes(StandardCharsets.UTF_8));
        byte[] bundleBytes = {0x48, 0x44, 0x41, 0x59};
        Files.write(bundle, bundleBytes);

        HolidayProperties properties = new HolidayProperties();
        properties.setAssetPath(tempDir.toString());
        BundleController controller = new BundleController(properties);

        assertEquals("{\"source\":\"external\"}", controller.getManifest().getBody());
        ResponseEntity<byte[]> response = controller.getBundle("CN", 2026);
        assertArrayEquals(bundleBytes, response.getBody());

        properties.setClasspathFallback(false);
        assertEquals(404, controller.getBundle("CN", 2099).getStatusCodeValue());
    }
}
