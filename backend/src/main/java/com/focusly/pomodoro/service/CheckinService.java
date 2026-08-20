package com.focusly.pomodoro.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.focusly.pomodoro.entity.Checkin;
import com.focusly.pomodoro.mapper.CheckinMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckinService {
    private final CheckinMapper mapper;

    /**
     * 用户主动打卡：
     * - 若当日已有累计（来自会话 UPSERT），直接更新 createdAt 标记打卡动作
     * - 若当日无记录，则插入空时长打卡行
     * - UNIQUE 兜底防重复：异常由 GlobalExceptionHandler 转 409
     */
    public Checkin doCheckin() {
        LocalDate today = LocalDate.now();
        Checkin existing = mapper.selectOne(
            Wrappers.<Checkin>lambdaQuery().eq(Checkin::getCheckinDate, today));
        if (existing != null) {
            // 已有记录（可能来自会话累计）视为已打卡 → 抛业务异常，前端友好提示
            throw new IllegalStateException("今日已打卡，请勿重复提交");
        }
        Checkin checkin = new Checkin();
        checkin.setCheckinDate(today);
        checkin.setFocusTotalMinutes(0);
        mapper.insert(checkin);
        return checkin;
    }

    public Map<String, Object> getTodayStatus() {
        LocalDate today = LocalDate.now();
        Checkin existing = mapper.selectOne(
            Wrappers.<Checkin>lambdaQuery().eq(Checkin::getCheckinDate, today));
        boolean checked = existing != null;
        int minutes = existing != null ? existing.getFocusTotalMinutes() : 0;
        return Map.of("checked", checked, "focusTotalMinutes", minutes);
    }

    public List<LocalDate> listCheckinDates(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        return mapper.findCheckedDates(start, end);
    }
}
