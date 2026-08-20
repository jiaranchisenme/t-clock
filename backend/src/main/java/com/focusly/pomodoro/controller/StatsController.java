package com.focusly.pomodoro.controller;

import com.focusly.pomodoro.common.Result;
import com.focusly.pomodoro.dto.response.StatsBundle;
import com.focusly.pomodoro.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService service;

    @GetMapping
    public Result<StatsBundle> get() {
        return Result.ok(service.getStats());
    }
}
