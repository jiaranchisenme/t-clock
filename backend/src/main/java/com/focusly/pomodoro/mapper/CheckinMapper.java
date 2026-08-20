package com.focusly.pomodoro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusly.pomodoro.entity.Checkin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface CheckinMapper extends BaseMapper<Checkin> {

    /**
     * 查询指定范围内已打卡日期（用于日历高亮）。
     */
    @Select("""
        SELECT checkin_date FROM daily_checkin
        WHERE checkin_date >= #{startDate} AND checkin_date <= #{endDate}
        ORDER BY checkin_date ASC
        """)
    List<LocalDate> findCheckedDates(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}
