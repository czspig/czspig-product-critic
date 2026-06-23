package com.czspig.productcritic.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.czspig.productcritic.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "czspig-product-critic");
        data.put("time", LocalDateTime.now());
        return ApiResponse.ok(data);
    }
}
