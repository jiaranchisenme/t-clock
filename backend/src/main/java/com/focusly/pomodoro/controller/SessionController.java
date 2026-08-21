package com.focusly.pomodoro.controller;

import com.focusly.pomodoro.common.Result;
import com.focusly.pomodoro.dto.request.SessionSaveRequest;
import com.focusly.pomodoro.dto.response.SessionSaveResult;
import com.focusly.pomodoro.entity.Session;
import com.focusly.pomodoro.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService service;

    @PostMapping
    public Result<SessionSaveResult> save(@Valid @RequestBody SessionSaveRequest req) {
        return Result.ok(service.save(req));
    }

    /**
     * 列表接口（需求 3：/stats/week 或 /stats/month 失败时 fallback 本地聚合）
     * 参数 startDate/endDate（YYYY-MM-DD，可选）按日期闭区间过滤。
     */
    @GetMapping
    public Result<List<Session>> list(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
        return Result.ok(service.list(startDate, endDate));
    }
}
