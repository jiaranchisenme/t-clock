package com.focusly.pomodoro.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StatsBundle {
    private List<DayStat> daily7;
    private List<DayStat> daily30;
    private int weeklyTotal;
    private int monthlyTotal;
}
