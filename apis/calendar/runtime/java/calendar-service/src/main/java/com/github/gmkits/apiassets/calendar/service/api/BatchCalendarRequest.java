package com.github.gmkits.apiassets.calendar.service.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** 范围批量查询请求体。未知 JSON 属性由服务的 Jackson 配置拒绝。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record BatchCalendarRequest(
        String region,
        String locale,
        List<String> fields,
        List<RangeRequest> ranges) {
    public record RangeRequest(String from, String to) { }
}
