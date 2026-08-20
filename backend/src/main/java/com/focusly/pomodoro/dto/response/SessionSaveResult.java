package com.focusly.pomodoro.dto.response;

import lombok.Data;

@Data
public class SessionSaveResult {
    private Long sessionId;
    private int todayFocusMinutes;
}
