package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.HourlyDTO;
import com.tongguo.dto.RevenueTrendDTO;
import com.tongguo.dto.TopDishDTO;
import com.tongguo.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/m/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/revenue-trend")
    public Result<List<RevenueTrendDTO>> revenueTrend(@RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.getRevenueTrend(startDate, endDate));
    }

    @GetMapping("/top-dishes")
    public Result<List<TopDishDTO>> topDishes(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate,
                                                @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.ok(statisticsService.getTopDishes(startDate, endDate, limit));
    }

    @GetMapping("/hourly")
    public Result<List<HourlyDTO>> hourly(@RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.getHourlyDistribution(startDate, endDate));
    }
}
