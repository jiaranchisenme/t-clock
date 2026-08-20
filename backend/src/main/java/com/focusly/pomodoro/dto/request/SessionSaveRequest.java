package com.focusly.pomodoro.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionSaveRequest {
    @NotBlank(message = "会话类型不能为空")
    private String sessionType;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "时长不能为空")
    @Min(value = 1, message = "时长至少 1 分钟")
    @Max(value = 600, message = "时长最多 600 分钟")
    private Integer durationMinutes;

    private Long taskId;
}
