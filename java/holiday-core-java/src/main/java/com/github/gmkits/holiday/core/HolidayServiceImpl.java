package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.DayInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Pattern;

/**
 * {@link HolidayService} 默认实现。
 *
 * <p>bundle 以 {@code (region, year)} 为粒度懒加载并放入无锁并发缓存；
 * 不缓存缺失或非法键。区间和整年查询优先走 bundle 级批量路径，
 * 避免逐日重复查询和中间集合。</p>
 */
final class HolidayServiceImpl implements HolidayService {

    private static final Pattern REGION_CODE =
            Pattern.compile("[A-Z]{2}(?:-[A-Z0-9]{1,8})*");

    private final String defaultRegion;
    private final Path dataPath;
    private final boolean classpathFallback;
    private final ConcurrentMap<String, HdayBundle> cache;
    private final BundleManifest filesystemManifest;
    private final BundleManifest classpathManifest;
    private final int defaultCacheStartYear;
    private final AtomicReferenceArray<HdayBundle> defaultYearCache;

    HolidayServiceImpl(String defaultRegion, Path dataPath, boolean classpathFallback) {
        this.defaultRegion = defaultRegion;
        this.dataPath = dataPath;
        this.classpathFallback = classpathFallback;
        this.cache = new ConcurrentHashMap<>();
        try {
            this.filesystemManifest = BundleManifest.filesystem(dataPath);
            this.classpathManifest = classpathFallback
                    ? BundleManifest.classpath(getClass().getClassLoader())
                    : null;
            int[] defaultRange = mergeRanges(
                    filesystemManifest == null
                            ? null : filesystemManifest.yearRange(defaultRegion),
                    classpathManifest == null
                            ? null : classpathManifest.yearRange(defaultRegion));
            this.defaultCacheStartYear = defaultRange == null ? 0 : defaultRange[0];
            this.defaultYearCache = defaultRange == null ? null
                    : new AtomicReferenceArray<>(
                            defaultRange[1] - defaultRange[0] + 1);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read holiday manifest", exception);
        }
    }

    @Override
    public DayInfo getDayInfo(LocalDate date) {
        return getDayInfo(defaultRegion, date);
    }

    @Override
    public DayInfo getDayInfo(String regionCode, LocalDate date) {
        return requireBundle(regionCode, date.getYear()).getDayInfo(date);
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        HdayBundle bundle = requireBundle(defaultRegion, date.getYear());
        return bundle.isHoliday(date.getDayOfYear() - 1);
    }

    @Override
    public boolean isWorkday(LocalDate date) {
        HdayBundle bundle = requireBundle(defaultRegion, date.getYear());
        return bundle.isWorkday(date.getDayOfYear() - 1);
    }

    @Override
    public boolean isStatutoryHoliday(LocalDate date) {
        HdayBundle bundle = requireBundle(defaultRegion, date.getYear());
        return bundle.isStatutoryHoliday(date.getDayOfYear() - 1);
    }

    @Override
    public boolean isAdjustedWorkday(LocalDate date) {
        HdayBundle bundle = requireBundle(defaultRegion, date.getYear());
        return bundle.isAdjustedWorkday(date.getDayOfYear() - 1);
    }

    @Override
    public List<DayInfo> getRange(LocalDate from, LocalDate to) {
        return getRange(defaultRegion, from, to);
    }

    @Override
    public List<DayInfo> getRange(String regionCode, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return Collections.emptyList();
        }

        List<DayInfo> result = new ArrayList<>(estimateRangeCapacity(from, to));
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            HdayBundle bundle = requireBundle(regionCode, year);
            int startIndex = year == from.getYear() ? from.getDayOfYear() - 1 : 0;
            int endIndex = year == to.getYear() ? to.getDayOfYear() - 1 : bundle.getDayCount() - 1;
            bundle.appendRangeTo(result, startIndex, endIndex);
        }
        return result;
    }

    @Override
    public List<DayInfo> getYear(int year) {
        return getYear(defaultRegion, year);
    }

    @Override
    public List<DayInfo> getYear(String regionCode, int year) {
        return requireBundle(regionCode, year).getDayInfos();
    }

    @Override
    public List<DayInfo> getMonth(int year, int month) {
        return getMonth(defaultRegion, year, month);
    }

    @Override
    public List<DayInfo> getMonth(String regionCode, int year, int month) {
        HdayBundle bundle = requireBundle(regionCode, year);
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
            HdayBundle bundle = requireBundle(regionCode, year);
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
        HdayBundle bundle = requireBundle(regionCode, from.getYear());
        DayInfo day = bundle.findStatutoryHoliday(from.getDayOfYear() - 1);
        if (day != null) {
            return day;
        }

        int[] range = availableRange(regionCode);
        int lastYear = range == null ? from.getYear() + 1 : range[1];
        for (int year = from.getYear() + 1; year <= lastYear; year++) {
            if (range != null && !isDeclared(regionCode, year)) continue;
            HdayBundle nextBundle = range == null
                    ? resolveBundle(regionCode, year)
                    : requireBundle(regionCode, year);
            if (nextBundle == null) return null;
            day = nextBundle.findStatutoryHoliday(0);
            if (day != null) return day;
        }
        return null;
    }

    @Override
    public void clearCache() {
        cache.clear();
        if (defaultYearCache != null) {
            for (int index = 0; index < defaultYearCache.length(); index++) {
                defaultYearCache.set(index, null);
            }
        }
    }

    private static int estimateRangeCapacity(LocalDate from, LocalDate to) {
        long dayCount = to.toEpochDay() - from.toEpochDay() + 1;
        // 不按不受信任的超长区间申请巨型数组；跨年时 List 自行按实际结果扩容。
        return (int) Math.min(dayCount, 366L);
    }

    private HdayBundle resolveBundle(String region, int year) {
        if (defaultYearCache != null && defaultRegion.equals(region)) {
            int index = year - defaultCacheStartYear;
            if (index < 0 || index >= defaultYearCache.length()) {
                return null;
            }
            HdayBundle cached = defaultYearCache.get(index);
            if (cached != null) return cached;
            if (!isDeclared(region, year)) return null;
            HdayBundle loaded = loadBundle(region, year);
            if (loaded == null) return null;
            if (defaultYearCache.compareAndSet(index, null, loaded)) {
                return loaded;
            }
            return defaultYearCache.get(index);
        }

        /*
         * 已加载的合法键直接命中缓存。格式和 manifest 校验只属于首次加载路径；
         * 若在每次查询时运行正则和 manifest Map 查询，会污染最常用的单日热路径。
         * 非法键永远不会被写入 cache，因此前置读取不会绕过约束。
         */
        String key = region + "/" + year;
        HdayBundle cached = cache.get(key);
        if (cached != null) return cached;
        if (region == null || !REGION_CODE.matcher(region).matches()
                || year < 1 || year > 9999) {
            return null;
        }
        if ((filesystemManifest != null || classpathManifest != null)
                && !isDeclared(region, year)) {
            return null;
        }
        HdayBundle loaded = loadBundle(region, year);
        if (loaded == null) return null;
        HdayBundle raced = cache.putIfAbsent(key, loaded);
        return raced == null ? loaded : raced;
    }

    private HdayBundle requireBundle(String region, int year) {
        HdayBundle bundle = resolveBundle(region, year);
        if (bundle == null) {
            throw new HolidayDataUnavailableException(region, year);
        }
        return bundle;
    }

    private int[] availableRange(String region) {
        return mergeRanges(
                filesystemManifest == null ? null : filesystemManifest.yearRange(region),
                classpathManifest == null ? null : classpathManifest.yearRange(region));
    }

    private static int[] mergeRanges(int[] first, int[] second) {
        if (first == null) return second;
        if (second == null) return first;
        return new int[] {
            Math.min(first[0], second[0]),
            Math.max(first[1], second[1])
        };
    }

    private HdayBundle loadBundle(String region, int year) {
        HdayBundle bundle = loadFromFilesystem(region, year);
        if (bundle == null && classpathFallback) {
            bundle = loadFromClasspath(region, year);
        }
        return bundle;
    }

    private HdayBundle loadFromFilesystem(String region, int year) {
        if (dataPath == null) {
            return null;
        }
        if (filesystemManifest != null && !filesystemManifest.contains(region, year)) {
            return null;
        }
        Path file = dataPath.resolve(region).resolve(year + ".hday");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            byte[] data = Files.readAllBytes(file);
            if (filesystemManifest != null) {
                filesystemManifest.verify(region, year, data);
            }
            return HdayReader.read(data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read holiday bundle " + file, e);
        }
    }

    private HdayBundle loadFromClasspath(String region, int year) {
        String assetResource = "cn-holiday-kit/assets/holidays/bundles/"
                + region + "/" + year + ".hday";
        HdayBundle bundle = readClasspathBundle(assetResource);
        if (bundle != null) {
            return bundle;
        }
        return readClasspathBundle("bundles/" + region + "/" + year + ".hday");
    }

    private HdayBundle readClasspathBundle(String resource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            byte[] data = readAllBytes(in);
            if (classpathManifest != null) {
                String[] parts = resource.split("/");
                int year = Integer.parseInt(
                        parts[parts.length - 1].replace(".hday", ""));
                String region = parts[parts.length - 2];
                classpathManifest.verify(region, year, data);
            }
            return HdayReader.read(data);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read classpath holiday bundle " + resource, e);
        }
    }

    private boolean isDeclared(String region, int year) {
        return filesystemManifest != null && filesystemManifest.contains(region, year)
                || classpathManifest != null && classpathManifest.contains(region, year);
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
