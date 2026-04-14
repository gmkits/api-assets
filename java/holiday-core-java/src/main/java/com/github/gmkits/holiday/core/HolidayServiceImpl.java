package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.DayInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link HolidayService}.
 *
 * <p>Bundles are lazily loaded per (region, year) pair. An {@link LRUCache}
 * keeps recently used bundles in memory. Resolution order:</p>
 * <ol>
 *   <li>Filesystem lookup under {@code dataPath}/{region}/{year}.hday</li>
 *   <li>Classpath lookup at {@code bundles/{region}/{year}.hday} (if enabled)</li>
 * </ol>
 */
final class HolidayServiceImpl implements HolidayService {

    private final String defaultRegion;
    private final Path dataPath;
    private final boolean classpathFallback;
    private final LRUCache<String, HdayBundle> cache;

    HolidayServiceImpl(String defaultRegion, Path dataPath, boolean classpathFallback) {
        this.defaultRegion = defaultRegion;
        this.dataPath = dataPath;
        this.classpathFallback = classpathFallback;
        this.cache = new LRUCache<String, HdayBundle>();
    }

    // ---- HolidayService implementation ----

    @Override
    public DayInfo getDayInfo(LocalDate date) {
        return getDayInfo(defaultRegion, date);
    }

    @Override
    public DayInfo getDayInfo(String regionCode, LocalDate date) {
        HdayBundle bundle = resolveBundle(regionCode, date.getYear());
        if (bundle == null) {
            return null;
        }
        return bundle.getDayInfo(date);
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        DayInfo info = getDayInfo(date);
        return info != null && info.isHoliday();
    }

    @Override
    public boolean isWorkday(LocalDate date) {
        DayInfo info = getDayInfo(date);
        return info != null && info.isWorkday();
    }

    @Override
    public boolean isStatutoryHoliday(LocalDate date) {
        DayInfo info = getDayInfo(date);
        return info != null && info.isStatutoryHoliday();
    }

    @Override
    public boolean isAdjustedWorkday(LocalDate date) {
        DayInfo info = getDayInfo(date);
        return info != null && info.isAdjustedWorkday();
    }

    @Override
    public List<DayInfo> getRange(LocalDate from, LocalDate to) {
        List<DayInfo> result = new ArrayList<DayInfo>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DayInfo info = getDayInfo(cursor);
            if (info != null) {
                result.add(info);
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    @Override
    public List<DayInfo> getYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getRange(start, end);
    }

    // ---- Bundle resolution ----

    private HdayBundle resolveBundle(String region, int year) {
        String key = region + "/" + year;
        HdayBundle bundle = cache.get(key);
        if (bundle != null) {
            return bundle;
        }

        bundle = loadFromFilesystem(region, year);
        if (bundle == null && classpathFallback) {
            bundle = loadFromClasspath(region, year);
        }
        if (bundle != null) {
            cache.put(key, bundle);
        }
        return bundle;
    }

    private HdayBundle loadFromFilesystem(String region, int year) {
        if (dataPath == null) {
            return null;
        }
        Path file = dataPath.resolve(region).resolve(year + ".hday");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return HdayReader.read(file);
        } catch (IOException e) {
            return null;
        }
    }

    private HdayBundle loadFromClasspath(String region, int year) {
        String resource = "bundles/" + region + "/" + year + ".hday";
        InputStream in = getClass().getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            return null;
        }
        try {
            return HdayReader.read(in);
        } catch (IOException e) {
            return null;
        } finally {
            try { in.close(); } catch (IOException ignored) { }
        }
    }
}
