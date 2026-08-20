package com.focusly.pomodoro.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常兜底，避免堆栈泄漏给前端，统一返回 Result 壳。
 * - 参数校验失败：400 业务错误
 * - 唯一约束冲突（防重复打卡）：409
 * - 其他运行时异常：500 兜底
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(400, "参数错误: " + msg);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDup(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMessage());
        return Result.fail(409, "数据重复，今日已打卡");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegal(IllegalArgumentException e) {
        log.warn("业务校验失败: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException e) {
        // 业务状态冲突（如重复打卡），返回 409
        log.warn("业务状态冲突: {}", e.getMessage());
        return Result.fail(409, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        log.error("未预期异常", e);
        return Result.fail(500, "服务器内部错误");
    }
}
