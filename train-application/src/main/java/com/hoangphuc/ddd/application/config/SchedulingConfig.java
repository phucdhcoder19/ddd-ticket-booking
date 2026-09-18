package com.hoangphuc.ddd.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật @Scheduled cho toàn ứng dụng. Thiếu annotation này thì job vẫn biên
 * dịch được, vẫn có bean, nhưng KHÔNG BAO GIỜ CHẠY — và không có lỗi nào báo.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
