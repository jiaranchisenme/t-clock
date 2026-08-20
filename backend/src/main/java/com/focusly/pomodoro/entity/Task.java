package com.focusly.pomodoro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    /** 0=未完成 1=已完成 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
