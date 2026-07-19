package com.github.gmkits.holiday.moduletest;

import com.github.gmkits.holiday.CnHolidayKit;
import com.github.gmkits.holiday.spec.DayInfo;

import java.time.LocalDate;

/**
 * 仅通过 module-path 消费最终单一 JAR 的冒烟程序。
 */
public final class ModulePathSmoke {

    private ModulePathSmoke() {
    }

    /**
     * 验证自动模块名称、公开包和内置节假日资产能够一起工作。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        DayInfo day = CnHolidayKit.create().getDayInfo(LocalDate.of(2026, 10, 1));
        if (day == null || !day.isStatutoryHoliday()) {
            throw new AssertionError("Module-path consumer cannot read built-in holiday assets");
        }
        System.out.println("module-path-smoke: OK");
    }
}
