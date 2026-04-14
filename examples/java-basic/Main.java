// Basic Java example
// Compile: javac -cp ../../java/holiday-core-java/build/libs/*:../../java/holiday-spec-java/build/libs/* Main.java
// Run: java -cp .:../../java/holiday-core-java/build/libs/*:../../java/holiday-spec-java/build/libs/* Main

import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.spec.DayInfo;
import java.time.LocalDate;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        HolidayService service = HolidayServiceBuilder.newBuilder()
            .defaultRegion("CN")
            .dataPath(Paths.get("../../data/bundles"))
            .build();

        LocalDate date = LocalDate.of(2025, 10, 1);
        DayInfo info = service.getDayInfo(date);

        System.out.println("=== Holiday Kit Java Example ===");
        System.out.println(date + ": " + (info.isHoliday() ? "Holiday" : "Workday"));
        System.out.println("  Statutory: " + info.isStatutoryHoliday());
        System.out.println("  Names: " + info.getHolidayNames());
        System.out.println("  Labels: " + info.getLabels());
    }
}
