package com.focusly.pomodoro.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.focusly.pomodoro.entity.TimerConfig;
import com.focusly.pomodoro.mapper.TimerConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimerConfigService {
    public static final int DEFAULT_WORK = 25;
    public static final int DEFAULT_BREAK = 5;

    private final TimerConfigMapper mapper;

    /**
     * 绝不返回 null：
     * - 正常情况读取第一行；
     * - 空表时写入默认行 (work=25, break=5)；
     * - 并发 insert 冲突时回退到重新 select；
     * - select 仍失败则返回内存默认值（id=0），保证前端绝不收到 data:null。
     */
    public TimerConfig getConfig() {
        TimerConfig cfg = mapper.selectOne(Wrappers.<TimerConfig>lambdaQuery().last("LIMIT 1"));
        if (cfg != null) return cfg;

        TimerConfig seed = new TimerConfig();
        seed.setWorkDuration(DEFAULT_WORK);
        seed.setBreakDuration(DEFAULT_BREAK);
        try {
            mapper.insert(seed);
        } catch (Exception ignored) {
            // 并发或约束冲突，尝试重新读取
            TimerConfig again = mapper.selectOne(Wrappers.<TimerConfig>lambdaQuery().last("LIMIT 1"));
            if (again != null) return again;
            // 最终兜底：返回内存默认值（id=0），上层仍可安全访问 workDuration/breakDuration
            seed.setId(0L);
        }
        return seed;
    }

    @Transactional(rollbackFor = Exception.class)
    public TimerConfig updateConfig(Integer workDuration, Integer breakDuration) {
        TimerConfig cfg = getConfig();
        // 兜底：id=0 意味着 seed 并未真正入库（极端路径），先插入
        if (cfg.getId() == null || cfg.getId() <= 0) {
            cfg = new TimerConfig();
            cfg.setWorkDuration(workDuration);
            cfg.setBreakDuration(breakDuration);
            mapper.insert(cfg);
            return cfg;
        }
        cfg.setWorkDuration(workDuration);
        cfg.setBreakDuration(breakDuration);
        mapper.updateById(cfg);
        return cfg;
    }
}
