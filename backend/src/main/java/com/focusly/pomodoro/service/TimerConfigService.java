package com.focusly.pomodoro.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.focusly.pomodoro.entity.TimerConfig;
import com.focusly.pomodoro.mapper.TimerConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimerConfigService {
    private final TimerConfigMapper mapper;

    /**
     * 获取当前配置（多行容错：取 id 最小行，避免空表时前端崩溃）。
     * 写安全：若表意外空，回写默认 25/5。
     */
    public TimerConfig getConfig() {
        return mapper.selectOne(Wrappers.<TimerConfig>lambdaQuery().last("LIMIT 1"));
    }

    public TimerConfig updateConfig(Integer workDuration, Integer breakDuration) {
        TimerConfig cfg = getConfig();
        if (cfg == null) {
            cfg = new TimerConfig();
            cfg.setWorkDuration(workDuration);
            cfg.setBreakDuration(breakDuration);
            mapper.insert(cfg);
        } else {
            cfg.setWorkDuration(workDuration);
            cfg.setBreakDuration(breakDuration);
            mapper.updateById(cfg);
        }
        return cfg;
    }
}
