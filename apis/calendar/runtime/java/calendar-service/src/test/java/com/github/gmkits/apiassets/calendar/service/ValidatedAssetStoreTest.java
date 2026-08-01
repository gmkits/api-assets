package com.github.gmkits.apiassets.calendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatedAssetStoreTest {
    @TempDir
    Path temporary;

    @Test
    void rejectsCorruptedCalendarAssetAtStartup() throws Exception {
        Path source = Path.of(System.getProperty("calendar.assets.path"));
        Files.copy(source.resolve("manifest.json"), temporary.resolve("manifest.json"));
        Path calendar = temporary.resolve("calendar/calendar.cdat");
        Files.createDirectories(calendar.getParent());
        byte[] bytes = Files.readAllBytes(source.resolve("calendar/calendar.cdat"));
        bytes[bytes.length - 5] ^= 1;
        Files.write(calendar, bytes);

        CalendarProperties properties = new CalendarProperties();
        properties.setAssetPath(temporary.toString());
        assertThrows(IllegalStateException.class,
                () -> new ValidatedAssetStore(properties, new ObjectMapper()));
    }
}
