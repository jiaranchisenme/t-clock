package com.focusly.pomodoro.controller;

import com.focusly.pomodoro.common.Result;
import com.focusly.pomodoro.entity.Checkin;
import com.focusly.pomodoro.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinController {
    private final CheckinService service;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping
    public Result<Checkin> doCheckin() {
        return Result.ok(service.doCheckin());
    }

    @GetMapping("/today")
    public Result<Map<String, Object>> today() {
        return Result.ok(service.getTodayStatus());
    }

    @GetMapping("/dates")
    public Result<List<String>> dates(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(service.listCheckinDates(days).stream()
            .map(d -> d.format(ISO))
            .collect(Collectors.toList()));
    }

    @GetMapping("/streak")
    public Result<Map<String, Object>> streak() {
        return Result.ok(Map.of("streak", service.getStreak()));
    }
}
