package com.github.gmkits.holiday.lunar;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可替换日历资产加载器。
 *
 * <p>优先读取 {@code -Dcn.holiday.assets.path=/path/to/date-assets} 或
 * {@code CN_HOLIDAY_ASSETS}，找不到配置时读取随统一库发布的 classpath 资产。</p>
 */
final class CalendarAssetLoader {

    private static final String ROOT_PROPERTY = "cn.holiday.assets.path";
    private static final String ROOT_ENV = "CN_HOLIDAY_ASSETS";
    private static final String CLASSPATH_ROOT = "cn-holiday-kit/assets/";
    private static final Pattern HEX_VALUE = Pattern.compile("0x([0-9a-fA-F]+)");

    private CalendarAssetLoader() {
    }

    static int[] loadLunarYears(int[] fallback) {
        try (InputStream input = open("calendar/lunar-years.hex")) {
            if (input == null) return fallback;
            Matcher matcher = HEX_VALUE.matcher(readUtf8(input));
            List<Integer> values = new ArrayList<>();
            while (matcher.find()) values.add(Integer.parseInt(matcher.group(1), 16));
            if (values.size() != fallback.length) {
                throw new IllegalStateException("Expected " + fallback.length
                        + " lunar year values, got " + values.size());
            }
            int[] result = new int[values.size()];
            for (int i = 0; i < result.length; i++) result[i] = values.get(i);
            return result;
        } catch (IOException | RuntimeException ex) {
            throw new ExceptionInInitializerError("Failed to load lunar date asset: " + ex.getMessage());
        }
    }

    static long[] loadSolarTerms(int startYear, int endYear, int[] baseDays, long[] fallback) {
        try (InputStream input = open("calendar/solar-terms.csv")) {
            int yearCount = endYear - startYear + 1;
            if (input == null) {
                if (fallback.length == yearCount) return fallback;
                long[] aligned = new long[yearCount];
                System.arraycopy(fallback, fallback.length - yearCount, aligned, 0, yearCount);
                return aligned;
            }
            long[] packed = new long[yearCount];
            boolean[][] seen = new boolean[yearCount][24];

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",", 3);
                    LocalDate date = LocalDate.parse(parts[0]);
                    int year = date.getYear();
                    if (year < startYear || year > endYear) continue;
                    int termIndex = Integer.parseInt(parts[1]);
                    if (termIndex < 0 || termIndex >= 24) {
                        throw new IllegalStateException("Invalid solar term index: " + line);
                    }
                    int offset = date.getDayOfMonth() - baseDays[termIndex];
                    if (offset < 0 || offset > 3) {
                        throw new IllegalStateException("Invalid solar term offset: " + line);
                    }
                    int yearIndex = year - startYear;
                    packed[yearIndex] |= (long) offset << (termIndex * 2);
                    seen[yearIndex][termIndex] = true;
                }
            }

            for (int yearIndex = 0; yearIndex < yearCount; yearIndex++) {
                for (int termIndex = 0; termIndex < 24; termIndex++) {
                    if (!seen[yearIndex][termIndex]) {
                        throw new IllegalStateException("Missing solar term "
                                + (startYear + yearIndex) + "#" + termIndex);
                    }
                }
            }
            return packed;
        } catch (IOException | RuntimeException ex) {
            throw new ExceptionInInitializerError("Failed to load solar-term asset: " + ex.getMessage());
        }
    }

    static String sourceDescription() {
        Path root = externalRoot();
        return root == null ? "classpath:" + CLASSPATH_ROOT
                : root.toAbsolutePath().normalize().toString();
    }

    private static InputStream open(String relativePath) throws IOException {
        Path root = externalRoot();
        if (root != null) {
            Path file = root.resolve(relativePath);
            if (!Files.isRegularFile(file)) throw new IOException("Missing external asset " + file);
            return Files.newInputStream(file);
        }
        return CalendarAssetLoader.class.getClassLoader()
                .getResourceAsStream(CLASSPATH_ROOT + relativePath);
    }

    private static Path externalRoot() {
        String configured = System.getProperty(ROOT_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) configured = System.getenv(ROOT_ENV);
        return configured == null || configured.trim().isEmpty() ? null : Paths.get(configured.trim());
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
