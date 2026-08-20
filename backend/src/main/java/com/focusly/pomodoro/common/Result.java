package com.focusly.pomodoro.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应壳：{ code, msg, data }
 * - code = 0  成功
 * - code != 0  业务错误
 * 与前端响应拦截器约定一致。
 */
@Data
@NoArgsConstructor
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(1, msg, null);
    }
}
