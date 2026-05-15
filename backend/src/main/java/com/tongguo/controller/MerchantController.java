package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.DashboardDTO;
import com.tongguo.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/m")
public class MerchantController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public Result<DashboardDTO> dashboard() {
        return Result.ok(dashboardService.getTodayData());
    }
}
