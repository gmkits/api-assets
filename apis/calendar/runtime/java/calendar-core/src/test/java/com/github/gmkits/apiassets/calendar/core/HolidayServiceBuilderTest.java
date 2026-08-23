package com.github.gmkits.apiassets.calendar.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HolidayServiceBuilderTest {

    @Test
    void assetPathRefusesChangingJvmGlobalRoot() {
        String previous = System.getProperty("calendar.assets.path");
        try {
            System.clearProperty("calendar.assets.path");
            new HolidayServiceBuilder().assetPath(Path.of("target/assets-a"));
            assertThrows(
                    IllegalStateException.class,
                    () -> new HolidayServiceBuilder().assetPath(Path.of("target/assets-b")));
        } finally {
            if (previous == null) {
                System.clearProperty("calendar.assets.path");
            } else {
                System.setProperty("calendar.assets.path", previous);
            }
        }
    }
}
