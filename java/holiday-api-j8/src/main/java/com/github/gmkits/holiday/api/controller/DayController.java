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
import java.util.Collections;
import java.util.List;

/**
 * 对外提供基础查询接口的控制器。
 */
@RestController
@RequestMapping("/api/v1")
public class DayController {

    private final HolidayService holidayService;

    /**
     * 创建只依赖核心查询服务的控制器。
     *
     * @param holidayService 线程安全节假日服务
     */
    public DayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    /**
     * 查询单个公历日。
     *
     * @param regionCode 区域代码
     * @param date 公历日期
     * @return 单日信息；未安装对应年份时返回 404
     */
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

    /**
     * 查询闭区间内的全部日期。
     *
     * @param regionCode 区域代码
     * @param from 起始日期，包含
     * @param to 结束日期，包含
     * @return 日期列表；没有可用数据时返回 404
     */
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

    /**
     * 查询指定公历年。
     *
     * @param regionCode 区域代码
     * @param year 公历年份
     * @return 整年日期列表；未安装时返回 404
     */
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

    /**
     * 返回支持的区域代码。
     *
     * @return 当前固定为只含 {@code CN} 的列表
     */
    @GetMapping("/regions")
    public List<String> getRegions() {
        return Collections.singletonList("CN");
    }

    /**
     * 返回 API、数据格式和区域信息。
     *
     * @return 版本信息
     */
    @GetMapping("/version")
    public VersionInfo getVersion() {
        return new VersionInfo("1.0.0", "1.0.0-SNAPSHOT", Collections.singletonList("CN"));
    }
}
