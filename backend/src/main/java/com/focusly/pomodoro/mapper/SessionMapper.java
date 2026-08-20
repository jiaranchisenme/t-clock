package com.focusly.pomodoro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusly.pomodoro.entity.Session;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SessionMapper extends BaseMapper<Session> {

    /**
     * 按日聚合会话专注时长（支撑统计看板）。
     * 使用 GROUP BY DATE(start_time) 按日聚合。
     */
    @Select("""
        SELECT DATE(start_time) AS day, COALESCE(SUM(duration_minutes), 0) AS minutes
        FROM pomodoro_session
        WHERE session_type = 'WORK'
          AND start_time >= #{startDate}
          AND start_time < #{endDate}
        GROUP BY DATE(start_time)
        ORDER BY day ASC
        """)
    List<Map<String, Object>> aggregateDailyMinutes(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
