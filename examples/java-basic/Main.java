// Basic Java example
// Compile: javac -cp ../../java/cn-holiday-kit/target/cn-holiday-kit-1.0.0-rc1.jar Main.java
// Run: java -cp .:../../java/cn-holiday-kit/target/cn-holiday-kit-1.0.0-rc1.jar Main

import com.github.gmkits.holiday.CnHolidayKit;
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.spec.DayInfo;
import java.time.LocalDate;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        HolidayService service =
            CnHolidayKit.fromAssets(Paths.get("../../data/date-assets"));

        LocalDate date = LocalDate.of(2025, 10, 1);
        DayInfo info = service.getDayInfo(date);

        System.out.println("=== Holiday Kit Java Example ===");
        System.out.println(date + ": " + (info.isHoliday() ? "Holiday" : "Workday"));
        System.out.println("  Statutory: " + info.isStatutoryHoliday());
        System.out.println("  Names: " + info.getHolidayNames());
        System.out.println("  Labels: " + info.getLabels());
    }
}
