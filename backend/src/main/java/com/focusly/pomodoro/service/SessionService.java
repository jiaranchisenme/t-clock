package com.focusly.pomodoro.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.focusly.pomodoro.dto.request.SessionSaveRequest;
import com.focusly.pomodoro.dto.response.SessionSaveResult;
import com.focusly.pomodoro.entity.Session;
import com.focusly.pomodoro.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionMapper sessionMapper;

    /**
     * 写安全核心：
     * 1) 单事务内插入会话
     * 2) 仅 WORK 类型会话累计专注时长（BREAK 不计）
     * 3) 当日累计专注分钟数由 sessions 实时聚合得出（需求：根据 sessions 计算当日累计专注分钟数），
     *    不再 UPSERT daily_checkin——daily_checkin 行仅由用户点击打卡时创建，
     *    从而避免「学过即已打卡」的语义混淆（满足「同一日期只能打卡一次」与按钮禁用语义）。
     */
    @Transactional
    public SessionSaveResult save(SessionSaveRequest req) {
        Session session = new Session();
        session.setSessionType(req.getSessionType());
        session.setStartTime(req.getStartTime());
        session.setEndTime(req.getEndTime());
        session.setDurationMinutes(req.getDurationMinutes());
        session.setTaskId(req.getTaskId());
        sessionMapper.insert(session);

        int todayFocus = 0;
        if ("WORK".equals(req.getSessionType())) {
            // 当日累计 = 今日所有 WORK 会话时长之和（实时从 sessions 计算）
            LocalDate today = req.getStartTime().toLocalDate();
            todayFocus = sessionMapper.sumWorkMinutes(today, today.plusDays(1));
        }

        SessionSaveResult result = new SessionSaveResult();
        result.setSessionId(session.getId());
        result.setTodayFocusMinutes(todayFocus);
        return result;
    }

    /**
     * 会话列表（需求 3：/stats/week 或 /stats/month 失败时，前端本地 sessions 聚合 fallback）。
     * - startDate / endDate（可选，闭区间按日期）：不传则返回最近 90 天
     * - 按 startTime 升序
     */
    public List<Session> list(String startDate, String endDate) {
        LocalDate start = parseDate(startDate, LocalDate.now().minusDays(90));
        LocalDate end = parseDate(endDate, LocalDate.now());
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        return sessionMapper.selectList(
            Wrappers.<Session>lambdaQuery()
                .ge(Session::getStartTime, from)
                .lt(Session::getStartTime, to)
                .orderByAsc(Session::getStartTime));
    }

    private LocalDate parseDate(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return LocalDate.parse(s);
        } catch (Exception ignore) {
            return fallback;
        }
    }
}
