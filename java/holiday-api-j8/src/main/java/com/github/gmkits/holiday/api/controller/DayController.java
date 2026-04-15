package com.github.gmkits.holiday.api.controller;

import com.github.gmkits.holiday.api.dto.VersionInfo;
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.spec.DayInfo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;

import java.time.LocalDate;
import java.util.List;

/**
 * 对外提供基础查询接口的控制器。
 */
@RestController
@RequestMapping("/api/v1")
public class DayController {

    private final HolidayService holidayService;

    public DayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping("/day")
    public ResponseEntity<DayInfo> getDay(
            @RequestParam(defaultValue = "CN") String regionCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DayInfo info = holidayService.getDayInfo(regionCode, date);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    @GetMapping("/range")
    public ResponseEntity<List<DayInfo>> getRange(
            @RequestParam(defaultValue = "CN") String regionCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<DayInfo> result = holidayService.getRange(regionCode, from, to);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/year")
    public ResponseEntity<List<DayInfo>> getYear(
            @RequestParam(defaultValue = "CN") String regionCode,
            @RequestParam int year) {
        List<DayInfo> result = holidayService.getYear(regionCode, year);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/regions")
    public List<String> getRegions() {
        return ImmutableList.of("CN");
    }

    @GetMapping("/version")
    public VersionInfo getVersion() {
        return new VersionInfo("1.0.0", "1.0.0-SNAPSHOT", ImmutableList.of("CN"));
    }
}
