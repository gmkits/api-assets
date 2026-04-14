import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import com.github.gmkits.holiday.spec.DayInfo;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Cross-language test harness for Java SDK.
 * Outputs golden-format JSON for specified dates.
 */
public class CrossLangTest {

    private static final String[] TEST_DATES = {
            "2025-01-01", "2025-01-26", "2025-01-28", "2025-05-01",
            "2025-10-01", "2026-01-01", "2026-02-17", "2026-10-01"
    };

    public static void main(String[] args) throws IOException {
        String bundleDir = args.length > 0 ? args[0] : "../../data/bundles";
        String outputDir = args.length > 1 ? args[1] : "./output/java";

        Path outPath = Paths.get(outputDir);
        Files.createDirectories(outPath);

        HolidayService service = new HolidayServiceBuilder()
                .defaultRegion("CN")
                .dataPath(Paths.get(bundleDir))
                .build();

        for (String dateStr : TEST_DATES) {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            DayInfo info = service.getDayInfo("CN", date);

            if (info == null) {
                System.err.println("WARN: No data for " + dateStr);
                continue;
            }

            String json = toGoldenJson(info);
            Path file = outPath.resolve("CN-" + dateStr + ".day.json");
            try (Writer w = Files.newBufferedWriter(file)) {
                w.write(json);
            }
            System.out.println("Wrote " + file);
        }
    }

    private static String toGoldenJson(DayInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"date\": \"").append(info.getDate()).append("\",\n");
        sb.append("  \"regionCode\": \"").append(info.getRegionCode()).append("\",\n");
        sb.append("  \"calendarSystem\": \"").append(info.getCalendarSystem()).append("\",\n");
        sb.append("  \"isHoliday\": ").append(info.isHoliday()).append(",\n");
        sb.append("  \"isWorkday\": ").append(info.isWorkday()).append(",\n");
        sb.append("  \"isWeekend\": ").append(info.isWeekend()).append(",\n");
        sb.append("  \"isStatutoryHoliday\": ").append(info.isStatutoryHoliday()).append(",\n");
        sb.append("  \"isAdjustedWorkday\": ").append(info.isAdjustedWorkday()).append(",\n");
        sb.append("  \"holidayNames\": ").append(namesJson(info.getHolidayNames())).append(",\n");
        sb.append("  \"labels\": ").append(listJson(info.getLabels())).append(",\n");
        sb.append("  \"sourceVersion\": ").append(
                info.getSourceVersion() != null ? "\"" + info.getSourceVersion() + "\"" : "\"\"").append(",\n");
        sb.append("  \"extensions\": {}\n");
        sb.append("}");
        return sb.toString();
    }

    private static String namesJson(Map<String, List<String>> names) {
        if (names == null || names.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : names.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("    \"").append(escapeJson(entry.getKey())).append("\": ");
            sb.append(listJson(entry.getValue()));
        }
        sb.append("\n  }");
        return sb.toString();
    }

    private static String listJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
