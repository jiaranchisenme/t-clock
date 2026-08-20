package com.focusly.pomodoro;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Focusly 番茄时钟后端启动类。
 * - @MapperScan 扫描 mapper 接口
 * - @EnableTransactionManagement 显式开启事务（默认即开启，写明便于阅读）
 */
@SpringBootApplication
@MapperScan("com.focusly.pomodoro.mapper")
@EnableTransactionManagement
public class FocuslyApplication {
    public static void main(String[] args) {
        SpringApplication.run(FocuslyApplication.class, args);
    }
}
