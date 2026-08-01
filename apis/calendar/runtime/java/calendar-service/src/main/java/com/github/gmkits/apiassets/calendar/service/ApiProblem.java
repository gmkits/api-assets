package com.github.gmkits.apiassets.calendar.service;

/** RFC Problem Details 的稳定 JSON 结构。 */
public record ApiProblem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String requestId) {
}
