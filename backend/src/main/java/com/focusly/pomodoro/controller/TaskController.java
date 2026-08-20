package com.focusly.pomodoro.controller;

import com.focusly.pomodoro.common.Result;
import com.focusly.pomodoro.dto.request.TaskCreateRequest;
import com.focusly.pomodoro.dto.request.TaskUpdateRequest;
import com.focusly.pomodoro.entity.Task;
import com.focusly.pomodoro.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService service;

    @GetMapping
    public Result<List<Task>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<Task> create(@Valid @RequestBody TaskCreateRequest req) {
        return Result.ok(service.create(req.getTitle(), req.getDescription()));
    }

    @PutMapping("/{id}")
    public Result<Task> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest req) {
        return Result.ok(service.update(id, req.getTitle(), req.getDescription(), req.getStatus()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> clearAll() {
        service.clearAll();
        return Result.ok();
    }
}
