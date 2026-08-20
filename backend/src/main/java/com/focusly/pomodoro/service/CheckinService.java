package com.focusly.pomodoro.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.focusly.pomodoro.entity.Checkin;
import com.focusly.pomodoro.mapper.CheckinMapper;
import com.focusly.pomodoro.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckinService {
    private final CheckinMapper mapper;
    private final SessionMapper sessionMapper;

    /**
     * 当日累计专注分钟数：实时从 sessions 聚合（需求 2）。
     */
    private int todayFocusMinutesFromSessions(LocalDate day) {
        return sessionMapper.sumWorkMinutes(day, day.plusDays(1));
    }

    /**
     * 用户主动打卡（需求 3/4）：
     * - 同一日期只能打卡一次：已有记录则抛业务异常（转 409，前端友好提示）
     * - 记录 date、studyTime（点击时当日累计专注分钟数）、createTime
     * - UNIQUE(checkin_date) 兜底防并发重复
     */
    public Checkin doCheckin() {
        LocalDate today = LocalDate.now();
        Checkin existing = mapper.selectOne(
            Wrappers.<Checkin>lambdaQuery().eq(Checkin::getCheckinDate, today));
        if (existing != null) {
            // 需求 4/6：同一日期只能打卡一次，重复提交给出友好提示
            throw new IllegalStateException("今日已打卡，请勿重复提交");
        }
        Checkin checkin = new Checkin();
        checkin.setCheckinDate(today);
        // studyTime = 点击打卡时当日累计专注分钟数（来自 sessions）
        checkin.setFocusTotalMinutes(todayFocusMinutesFromSessions(today));
        // createTime = 打卡动作时间（需求 3）；DB 列亦有 DEFAULT 兜底
        checkin.setCreatedAt(LocalDateTime.now());
        mapper.insert(checkin);
        return checkin;
    }

    /**
     * 今日状态（需求 2/5）：
     * - checked：daily_checkin 是否存在今日行（= 用户是否点击过打卡）
     * - focusTotalMinutes：当日累计专注分钟数，实时从 sessions 计算（未打卡也有值）
     */
    public Map<String, Object> getTodayStatus() {
        LocalDate today = LocalDate.now();
        Checkin existing = mapper.selectOne(
            Wrappers.<Checkin>lambdaQuery().eq(Checkin::getCheckinDate, today));
        boolean checked = existing != null;
        int minutes = todayFocusMinutesFromSessions(today);
        return Map.of("checked", checked, "focusTotalMinutes", minutes);
    }

    public List<LocalDate> listCheckinDates(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        return mapper.findCheckedDates(start, end);
    }

    /**
     * 计算连续打卡天数（streak，需求 9/10）：
     * - 今日已打卡 → 从今日往前数连续日期链
     * - 今日未打卡 → 从昨日往前数连续日期链（需求 10）
     * - 一旦遇到断点即停
     * 性能：一次查询近 400 天已打卡日期集合，内存反推连续链，避免逐日查库。
     */
    public int getStreak() {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = mapper.findCheckedDates(today.minusDays(400), today);
        java.util.Set<LocalDate> set = new java.util.HashSet<>(dates);

        LocalDate cursor = set.contains(today) ? today : today.minusDays(1);

        int streak = 0;
        while (set.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
