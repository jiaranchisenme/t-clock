package com.focusly.pomodoro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pomodoro_session")
public class Session {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Long taskId;
    private LocalDateTime createdAt;
}
