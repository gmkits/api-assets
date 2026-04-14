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
 * {@link HolidayService} 默认实现。
 *
 * <p>bundle 以 `(region, year)` 为粒度懒加载并放入 LRU 缓存；区间和整年查询会优先走 bundle 级批量路径，
 * 避免逐日重复查单天。</p>
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
        return getRange(defaultRegion, from, to);
    }

    @Override
    public List<DayInfo> getRange(String regionCode, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return new ArrayList<DayInfo>();
        }

        List<DayInfo> result = new ArrayList<DayInfo>();
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            HdayBundle bundle = resolveBundle(regionCode, year);
            if (bundle == null) {
                continue;
            }
            int startIndex = year == from.getYear() ? from.getDayOfYear() - 1 : 0;
            int endIndex = year == to.getYear() ? to.getDayOfYear() - 1 : bundle.getDayCount() - 1;
            result.addAll(bundle.getRange(startIndex, endIndex));
        }
        return result;
    }

    @Override
    public List<DayInfo> getYear(int year) {
        return getYear(defaultRegion, year);
    }

    @Override
    public List<DayInfo> getYear(String regionCode, int year) {
        HdayBundle bundle = resolveBundle(regionCode, year);
        if (bundle == null) {
            return new ArrayList<DayInfo>();
        }
        return new ArrayList<DayInfo>(bundle.getDayInfos());
    }

    @Override
    public List<DayInfo> getMonth(int year, int month) {
        return getMonth(defaultRegion, year, month);
    }

    @Override
    public List<DayInfo> getMonth(String regionCode, int year, int month) {
        HdayBundle bundle = resolveBundle(regionCode, year);
        if (bundle == null) {
            return new ArrayList<DayInfo>();
        }
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        int startIndex = first.getDayOfYear() - 1;
        int endIndex = last.getDayOfYear() - 1;
        return bundle.getRange(startIndex, endIndex);
    }

    @Override
    public int countWorkdays(LocalDate from, LocalDate to) {
        return countWorkdays(defaultRegion, from, to);
    }

    @Override
    public int countWorkdays(String regionCode, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return 0;
        }

        // 直接在 bundle 上计数，避免分配中间 List<DayInfo>
        int count = 0;
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            HdayBundle bundle = resolveBundle(regionCode, year);
            if (bundle == null) {
                continue;
            }
            int startIndex = year == from.getYear() ? from.getDayOfYear() - 1 : 0;
            int endIndex = year == to.getYear() ? to.getDayOfYear() - 1 : bundle.getDayCount() - 1;
            count += bundle.countWorkdays(startIndex, endIndex);
        }
        return count;
    }

    @Override
    public DayInfo getNextHoliday(LocalDate from) {
        return getNextHoliday(defaultRegion, from);
    }

    @Override
    public DayInfo getNextHoliday(String regionCode, LocalDate from) {
        // 先在当年搜索
        HdayBundle bundle = resolveBundle(regionCode, from.getYear());
        if (bundle != null) {
            int startIndex = from.getDayOfYear() - 1;
            List<DayInfo> days = bundle.getRange(startIndex, bundle.getDayCount() - 1);
            for (DayInfo day : days) {
                if (day.isStatutoryHoliday()) {
                    return day;
                }
            }
        }
        // 搜索下一年
        HdayBundle nextBundle = resolveBundle(regionCode, from.getYear() + 1);
        if (nextBundle != null) {
            for (DayInfo day : nextBundle.getDayInfos()) {
                if (day.isStatutoryHoliday()) {
                    return day;
                }
            }
        }
        return null;
    }

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
            try {
                in.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
