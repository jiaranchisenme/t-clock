package com.focusly.pomodoro.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimerConfigRequest {
    @NotNull(message = "学习时长不能为空")
    @Min(value = 1, message = "学习时长至少 1 分钟")
    @Max(value = 180, message = "学习时长最多 180 分钟")
    private Integer workDuration;

    @NotNull(message = "休息时长不能为空")
    @Min(value = 1, message = "休息时长至少 1 分钟")
    @Max(value = 60, message = "休息时长最多 60 分钟")
    private Integer breakDuration;
}
