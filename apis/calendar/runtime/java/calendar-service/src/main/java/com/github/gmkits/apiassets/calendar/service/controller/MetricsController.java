package com.github.gmkits.apiassets.calendar.service.controller;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 前置平台采集的 Prometheus 文本指标。 */
@RestController
public final class MetricsController {
    private final PrometheusMeterRegistry registry;

    public MetricsController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(value = "/internal/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public String metrics() {
        return registry.scrape();
    }
}
