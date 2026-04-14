package com.github.gmkits.holiday.api25.config;

import com.github.gmkits.holiday.api25.service.HolidayOpsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动后按配置预热常用地区和年份。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarmupRunner implements ApplicationRunner {

    private final HolidayOpsService holidayOpsService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            holidayOpsService.warmUp(null, null, true);
        } catch (Exception ex) {
            log.warn("holiday warmup skipped: {}", ex.getMessage());
        }
    }
}
