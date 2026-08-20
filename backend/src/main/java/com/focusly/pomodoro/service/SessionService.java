package com.focusly.pomodoro.service;

import com.focusly.pomodoro.dto.request.SessionSaveRequest;
import com.focusly.pomodoro.dto.response.SessionSaveResult;
import com.focusly.pomodoro.entity.Session;
import com.focusly.pomodoro.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
}
