package com.focusly.pomodoro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_checkin")
public class Checkin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate checkinDate;
    private Integer focusTotalMinutes;
    private LocalDateTime createdAt;
}
