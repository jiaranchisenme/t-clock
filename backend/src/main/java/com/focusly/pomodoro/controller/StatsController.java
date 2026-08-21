package com.focusly.pomodoro.controller;

import com.focusly.pomodoro.common.Result;
import com.focusly.pomodoro.dto.response.DayStat;
import com.focusly.pomodoro.dto.response.StatsBundle;
import com.focusly.pomodoro.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService service;

    /** 综合聚合（兼容老调用方） */
    @GetMapping
    public Result<StatsBundle> get() {
        return Result.ok(service.getStats());
    }

    /** 需求 2：近 7 天柱状图数据 */
    @GetMapping("/week")
    public Result<List<DayStat>> week() {
        return Result.ok(service.week());
    }

    /** 需求 2：近 30 天柱状图数据 */
    @GetMapping("/month")
    public Result<List<DayStat>> month() {
        return Result.ok(service.month());
    }
}
