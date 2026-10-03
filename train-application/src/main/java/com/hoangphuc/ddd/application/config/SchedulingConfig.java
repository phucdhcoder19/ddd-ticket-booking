package com.hoangphuc.ddd.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled for the whole application. Without this annotation the
 * jobs still compile and still exist as beans, but they NEVER RUN — and
 * nothing reports an error.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
