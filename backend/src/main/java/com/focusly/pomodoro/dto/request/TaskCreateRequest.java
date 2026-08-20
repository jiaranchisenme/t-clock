package com.focusly.pomodoro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称最多 100 字")
    private String title;

    @Size(max = 500, message = "任务描述最多 500 字")
    private String description;
}
