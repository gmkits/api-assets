package com.github.gmkits.apiassets.calendar.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleManifestTest {

    @TempDir
    Path temporary;

    @Test
    void manifestRestrictsKeysAndVerifiesSha256() throws IOException {
        Path bundles = Files.createDirectories(temporary.resolve("bundles"));
        String json = "{"
                + "\"bundleFormatVersion\":\"2\","
                + "\"bundles\":{\"CN\":{\"2025\":{"
                + "\"sha256\":\""
                + "ba7816bf8f01cfea414140de5dae2223"
                + "b00361a396177a9cb410ff61f20015ad\""
                + "}}}}";
        Files.write(
                temporary.resolve("manifest.json"),
                json.getBytes(StandardCharsets.UTF_8));

        BundleManifest manifest = BundleManifest.filesystem(bundles);
        assertNotNull(manifest);
        assertTrue(manifest.contains("CN", 2025));
        assertFalse(manifest.contains("CN", 2026));
        assertArrayEquals(new int[] {2025, 2025}, manifest.yearRange("CN"));
        manifest.verify("CN", 2025, "abc".getBytes(StandardCharsets.UTF_8));
        assertThrows(
                IOException.class,
                () -> manifest.verify(
                        "CN", 2025, "abd".getBytes(StandardCharsets.UTF_8)));
    }
}
