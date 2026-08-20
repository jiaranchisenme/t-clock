package com.focusly.pomodoro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("timer_config")
public class TimerConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer workDuration;
    private Integer breakDuration;
    private LocalDateTime updatedAt;
}
