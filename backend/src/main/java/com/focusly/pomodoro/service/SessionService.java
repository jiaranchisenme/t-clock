package com.focusly.pomodoro.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.focusly.pomodoro.dto.request.SessionSaveRequest;
import com.focusly.pomodoro.dto.response.SessionSaveResult;
import com.focusly.pomodoro.entity.Checkin;
import com.focusly.pomodoro.entity.Session;
import com.focusly.pomodoro.mapper.CheckinMapper;
import com.focusly.pomodoro.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionMapper sessionMapper;
    private final CheckinMapper checkinMapper;

    /**
     * 写安全核心：
     * 1) 单事务内插入会话 + UPSERT 当日打卡累计，保证两表一致
     * 2) 仅 WORK 类型会话累计专注时长（BREAK 不计）
     * 3) UNIQUE(checkin_date) 兜底，并发首次打卡由 DB 拒绝
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
            LocalDate today = req.getStartTime().toLocalDate();
            // UPSERT：当日打卡行不存在则建，存在则累加
            Checkin checkin = checkinMapper.selectOne(
                Wrappers.<Checkin>lambdaQuery().eq(Checkin::getCheckinDate, today));
            if (checkin == null) {
                checkin = new Checkin();
                checkin.setCheckinDate(today);
                checkin.setFocusTotalMinutes(req.getDurationMinutes());
                try {
                    checkinMapper.insert(checkin);
                } catch (org.springframework.dao.DuplicateKeyException dup) {
                    // 并发首次打卡：被 DB UNIQUE 拦截，回退到累加路径
                    checkin = checkinMapper.selectOne(
                        Wrappers.<Checkin>lambdaQuery().eq(Checkin::getCheckinDate, today));
                    checkin.setFocusTotalMinutes(checkin.getFocusTotalMinutes() + req.getDurationMinutes());
                    checkinMapper.updateById(checkin);
                }
            } else {
                checkin.setFocusTotalMinutes(checkin.getFocusTotalMinutes() + req.getDurationMinutes());
                checkinMapper.updateById(checkin);
            }
            todayFocus = checkin.getFocusTotalMinutes();
        }

        SessionSaveResult result = new SessionSaveResult();
        result.setSessionId(session.getId());
        result.setTodayFocusMinutes(todayFocus);
        return result;
    }
}
