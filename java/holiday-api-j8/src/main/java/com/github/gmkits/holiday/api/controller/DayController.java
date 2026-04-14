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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        List<DayInfo> result = new ArrayList<DayInfo>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DayInfo info = holidayService.getDayInfo(regionCode, cursor);
            if (info != null) {
                result.add(info);
            }
            cursor = cursor.plusDays(1);
        }
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/year")
    public ResponseEntity<List<DayInfo>> getYear(
            @RequestParam(defaultValue = "CN") String regionCode,
            @RequestParam int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        List<DayInfo> result = new ArrayList<DayInfo>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DayInfo info = holidayService.getDayInfo(regionCode, cursor);
            if (info != null) {
                result.add(info);
            }
            cursor = cursor.plusDays(1);
        }
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/regions")
    public List<String> getRegions() {
        return Collections.singletonList("CN");
    }

    @GetMapping("/version")
    public VersionInfo getVersion() {
        return new VersionInfo("1.0.0", "1.0.0-SNAPSHOT", Collections.singletonList("CN"));
    }
}
