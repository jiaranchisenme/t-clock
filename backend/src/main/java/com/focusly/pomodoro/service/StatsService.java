package com.focusly.pomodoro.service;

import com.focusly.pomodoro.dto.response.DayStat;
import com.focusly.pomodoro.dto.response.StatsBundle;
import com.focusly.pomodoro.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 统计看板：基于 pomodoro_session 按日聚合，前端零计算。
 * 默认值兜底：缺失日期补 0，保证图表横轴连续。
 */
@Service
@RequiredArgsConstructor
public class StatsService {
    private final SessionMapper sessionMapper;

    public StatsBundle getStats() {
        LocalDate today = LocalDate.now();
        LocalDate start30 = today.minusDays(29);
        LocalDate start7 = today.minusDays(6);

        // 一次查询 30 天范围，内存切分 7 天与 30 天，减少 DB 往返
        Map<LocalDate, Integer> byDate = aggregate(start30, today.plusDays(1));

        StatsBundle bundle = new StatsBundle();
        bundle.setDaily7(fillRange(start7, today, byDate));
        bundle.setDaily30(fillRange(start30, today, byDate));

        int weekly = bundle.getDaily7().stream().mapToInt(DayStat::getMinutes).sum();
        int monthly = bundle.getDaily30().stream().mapToInt(DayStat::getMinutes).sum();
        bundle.setWeeklyTotal(weekly);
        bundle.setMonthlyTotal(monthly);
        return bundle;
    }

    /** 需求 1：近 7 天（含今日），按日期从旧到新排序，缺失日期补 0，按日求和 studyTime */
    public List<DayStat> week() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        Map<LocalDate, Integer> byDate = aggregate(start, today.plusDays(1));
        return fillRange(start, today, byDate);
    }

    /** 需求 1：近 30 天（含今日） */
    public List<DayStat> month() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);
        Map<LocalDate, Integer> byDate = aggregate(start, today.plusDays(1));
        return fillRange(start, today, byDate);
    }

    private Map<LocalDate, Integer> aggregate(LocalDate start, LocalDate endExclusive) {
        List<Map<String, Object>> rows = sessionMapper.aggregateDailyMinutes(start, endExclusive);
        Map<LocalDate, Integer> byDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate d = ((java.sql.Date) row.get("day")).toLocalDate();
            int m = ((Number) row.get("minutes")).intValue();
            byDate.merge(d, m, Integer::sum); // 按日求和 studyTime（需求 4）
        }
        return byDate;
    }

    /** 需求 5/6：缺失日期补 0，从旧到新顺序 */
    private List<DayStat> fillRange(LocalDate start, LocalDate end, Map<LocalDate, Integer> src) {
        List<DayStat> list = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayStat stat = new DayStat();
            stat.setDate(d.toString());
            stat.setMinutes(src.getOrDefault(d, 0));
            list.add(stat);
        }
        return list;
    }
}
