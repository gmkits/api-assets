package com.github.gmkits.holiday.benchmark;

import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import com.github.gmkits.holiday.spec.DayInfo;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Hot-path benchmarks for a shared, thread-safe {@link HolidayService}.
 *
 * Run from the {@code java} directory:
 * {@code java -Xms4g -Xmx4g -jar holiday-benchmarks/target/benchmarks.jar}
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

        @Setup(Level.Trial)
        public void setup() {
            Path dataPath = Paths.get(System.getProperty("holiday.data.path", "../data/bundles"))
                    .toAbsolutePath().normalize();
            service = new HolidayServiceBuilder()
                    .dataPath(dataPath)
                    .enableClasspathFallback(false)
                    .build();
            day = LocalDate.of(2026, 10, 1);
            rangeStart = LocalDate.of(2026, 1, 1);
            rangeEnd = LocalDate.of(2026, 12, 31);
            if (service.getDayInfo(day) == null) {
                throw new IllegalStateException("Benchmark data not found under " + dataPath);
            }
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
    @Threads(Threads.MAX)
    public void concurrentMixedQueries(SharedState state, Blackhole blackhole) {
        blackhole.consume(state.service.getDayInfo(state.day));
        blackhole.consume(state.service.isHoliday(state.day));
        blackhole.consume(state.service.getMonth(2026, 10));
        blackhole.consume(state.service.countWorkdays(state.rangeStart, state.rangeEnd));
    }
}
