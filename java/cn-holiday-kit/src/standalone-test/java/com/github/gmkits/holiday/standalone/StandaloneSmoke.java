package com.github.gmkits.holiday.standalone;

import com.github.gmkits.holiday.CnHolidayKit;
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.lunar.LunarInfo;
import com.github.gmkits.holiday.spec.DayInfo;

import java.time.LocalDate;

/**
 * 仅依赖最终单一 JAR 编译和运行的黑盒冒烟程序。
 */
public final class StandaloneSmoke {

    private StandaloneSmoke() {
    }

    /**
     * 验证统一入口、核心类型、农历类型和内置资产都位于同一个发布物中。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        HolidayService service = CnHolidayKit.create();
        DayInfo nationalDay = service.getDayInfo(LocalDate.of(2026, 10, 1));
        LunarInfo springFestival = CnHolidayKit.solarToLunar(LocalDate.of(2026, 2, 17));

        if (nationalDay == null || !nationalDay.isStatutoryHoliday()) {
            throw new AssertionError("Built-in CN holiday asset is unavailable");
        }
        if (springFestival.getDate().getMonth() != 1
                || springFestival.getDate().getDay() != 1) {
            throw new AssertionError("Built-in lunar asset is unavailable");
        }
        if (!"立春".equals(CnHolidayKit.getSolarTerm(LocalDate.of(2025, 2, 3)))) {
            throw new AssertionError("Built-in solar-term asset is unavailable");
        }
        System.out.println("single-jar-smoke: OK");
    }
}
