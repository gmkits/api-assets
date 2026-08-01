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
            @RequestParam(name = "regionCode", defaultValue = "CN") String regionCode,
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(holidayService.getDayInfo(regionCode, date));
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
            @RequestParam(name = "regionCode", defaultValue = "CN") String regionCode,
            @RequestParam(name = "from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("起始日期不能晚于结束日期");
        }
        List<DayInfo> result = holidayService.getRange(regionCode, from, to);
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
            @RequestParam(name = "regionCode", defaultValue = "CN") String regionCode,
            @RequestParam(name = "year") int year) {
        return ResponseEntity.ok(holidayService.getYear(regionCode, year));
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
        Package apiPackage = DayController.class.getPackage();
        String apiVersion = apiPackage == null
                ? null : apiPackage.getImplementationVersion();
        if (apiVersion == null || apiVersion.trim().isEmpty()) {
            apiVersion = "dev";
        }
        return new VersionInfo(
                apiVersion,
                "2026.GOV_NOTICE",
                Collections.singletonList("CN"));
    }
}
