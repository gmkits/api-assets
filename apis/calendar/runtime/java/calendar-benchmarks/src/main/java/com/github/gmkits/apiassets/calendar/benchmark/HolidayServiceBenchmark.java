package com.github.gmkits.apiassets.calendar.benchmark;

import com.github.gmkits.apiassets.calendar.core.HdayBundle;
import com.github.gmkits.apiassets.calendar.core.HdayReader;
import com.github.gmkits.apiassets.calendar.core.HolidayService;
import com.github.gmkits.apiassets.calendar.core.HolidayServiceBuilder;
import com.github.gmkits.apiassets.calendar.core.WorkdayStats;
import com.github.gmkits.apiassets.calendar.lunar.LunarCalendar;
import com.github.gmkits.apiassets.calendar.lunar.LunarInfo;
import com.github.gmkits.apiassets.calendar.spec.DayInfo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Hot-path benchmarks for a shared, thread-safe {@link HolidayService}.
 *
 * Run from the repository root:
 * {@code java -Xms4g -Xmx4g -jar apis/calendar/runtime/java/calendar-benchmarks/target/benchmarks.jar}
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 1, jvmArgsAppend = {"-Xms4g", "-Xmx4g", "-XX:+UseG1GC"})
public class HolidayServiceBenchmark {

    @State(Scope.Benchmark)
    public static class SharedState {
        HolidayService service;
        LocalDate day;
        LocalDate rangeStart;
        LocalDate rangeEnd;
        LocalDate batchStart;
        LocalDate batchEnd;

        @Setup(Level.Trial)
        public void setup() {
            Path dataPath = resolveAssetRoot();
            service = serviceFromAssets(dataPath);
            day = LocalDate.of(2026, 10, 1);
            rangeStart = LocalDate.of(2026, 1, 1);
            rangeEnd = LocalDate.of(2026, 12, 31);
            batchStart = LocalDate.of(2000, 1, 1);
            batchEnd = batchStart.plusDays(4095);
            if (service.getDayInfo(day) == null) {
                throw new IllegalStateException("Benchmark assets not found under " + dataPath);
            }
        }
    }

    /** Bytes and paths reused while measuring cold parser/service construction. */
    @State(Scope.Thread)
    public static class ColdState {
        byte[] bundle;
        Path assetRoot;
        LocalDate day;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            assetRoot = resolveAssetRoot();
            bundle = Files.readAllBytes(
                    assetRoot.resolve("holidays/bundles/CN/2026.hday"));
            day = LocalDate.of(2026, 10, 1);
        }
    }

    @Benchmark
    public DayInfo singleDay(SharedState state) {
        return state.service.getDayInfo(state.day);
    }

    @Benchmark
    public boolean isHoliday(SharedState state) {
        return state.service.isHoliday(state.day);
    }

    @Benchmark
    public List<DayInfo> month(SharedState state) {
        return state.service.getMonth(2026, 10);
    }

    @Benchmark
    public int countWorkdaysForYear(SharedState state) {
        return state.service.countWorkdays(state.rangeStart, state.rangeEnd);
    }

    @Benchmark
    public WorkdayStats workdayStatsForYear(SharedState state) {
        return state.service.getWorkdayStats("CN", state.rangeStart, state.rangeEnd);
    }

    @Benchmark
    public int holidaySummary(SharedState state) {
        return state.service.getHolidayPeriods("CN", 2026).size();
    }

    @Benchmark
    public List<DayInfo> batch366Days(SharedState state) {
        return state.service.getRange("CN", state.rangeStart, state.rangeEnd);
    }

    @Benchmark
    public int batch4096Days(SharedState state) {
        return state.service.getRange("CN", state.batchStart, state.batchEnd).size();
    }

    @Benchmark
    public LunarInfo lunarConversion(SharedState state) {
        return LunarCalendar.solarToLunar(state.day);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public HdayBundle coldBundleParse(ColdState state) throws IOException {
        return HdayReader.read(new ByteArrayInputStream(state.bundle));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public DayInfo firstServiceQuery(ColdState state) {
        return serviceFromAssets(state.assetRoot).getDayInfo(state.day);
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentMixedQueries(SharedState state, Blackhole blackhole) {
        blackhole.consume(state.service.getDayInfo(state.day));
        blackhole.consume(state.service.isHoliday(state.day));
        blackhole.consume(state.service.getMonth(2026, 10));
        blackhole.consume(state.service.countWorkdays(state.rangeStart, state.rangeEnd));
    }

    private static Path resolveAssetRoot() {
        String configured = System.getProperty("calendar.assets.path");
        if (configured != null && !configured.trim().isEmpty()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        Path repositoryRoot = Paths.get("apis/calendar/assets/runtime").toAbsolutePath().normalize();
        if (Files.isDirectory(repositoryRoot)) return repositoryRoot;
        return Paths.get("../../assets/runtime").toAbsolutePath().normalize();
    }

    private static HolidayService serviceFromAssets(Path root) {
        return new HolidayServiceBuilder()
                .assetPath(root)
                .enableClasspathFallback(false)
                .build();
    }
}
