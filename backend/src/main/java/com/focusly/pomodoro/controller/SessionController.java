package com.focusly.pomodoro.controller;

import com.focusly.pomodoro.common.Result;
import com.focusly.pomodoro.dto.request.SessionSaveRequest;
import com.focusly.pomodoro.dto.response.SessionSaveResult;
import com.focusly.pomodoro.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService service;

    @PostMapping
    public Result<SessionSaveResult> save(@Valid @RequestBody SessionSaveRequest req) {
        return Result.ok(service.save(req));
    }
}
