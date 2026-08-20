package com.focusly.pomodoro.controller;

import com.focusly.pomodoro.common.Result;
import com.focusly.pomodoro.dto.request.TimerConfigRequest;
import com.focusly.pomodoro.entity.TimerConfig;
import com.focusly.pomodoro.service.TimerConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timer")
@RequiredArgsConstructor
public class TimerConfigController {
    private final TimerConfigService service;

    @GetMapping("/config")
    public Result<TimerConfig> get() {
        return Result.ok(service.getConfig());
    }

    @PutMapping("/config")
    public Result<TimerConfig> update(@Valid @RequestBody TimerConfigRequest req) {
        return Result.ok(service.updateConfig(req.getWorkDuration(), req.getBreakDuration()));
    }
}
