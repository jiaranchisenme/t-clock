package com.focusly.pomodoro.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskUpdateRequest {
    @Size(max = 100, message = "任务名称最多 100 字")
    private String title;

    @Size(max = 500, message = "任务描述最多 500 字")
    private String description;

    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    private Integer status;
}
