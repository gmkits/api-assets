package com.github.gmkits.apiassets.calendar.service.controller;

import com.github.gmkits.apiassets.calendar.service.ValidatedAssetStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 容器编排使用的无鉴权健康检查。 */
@RestController
@RequestMapping("/internal/health")
public final class HealthController {
    private final ValidatedAssetStore assets;

    public HealthController(ValidatedAssetStore assets) {
        this.assets = assets;
    }

    @GetMapping("/live")
    public Map<String, String> live() {
        return Map.of("status", "UP");
    }

    @GetMapping("/ready")
    public Map<String, Object> ready() {
        return Map.of("status", "UP", "dataVersion", assets.dataVersion());
    }
}
