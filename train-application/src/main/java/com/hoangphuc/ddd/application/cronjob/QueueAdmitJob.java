package com.hoangphuc.ddd.application.cronjob;

import com.hoangphuc.ddd.application.service.queue.QueueAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The waiting room's NUMBER CALLER: every second, let more people in if there is room inside.
 *
 * Running on every server at once is fine, like HoldReleaseJob: "count free
 * slots, then admit people" lives entirely in one Lua script, so two servers
 * can never admit the same person, nor both see "20 slots free" and admit 40.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class QueueAdmitJob {

    private final QueueAppService queueAppService;

    @Scheduled(fixedDelayString = "${app.queue.admit-interval-ms:1000}")
    public void admit() {
        try {
            long n = queueAppService.admitNext();
            if (n > 0) {
                log.info("[QUEUE-JOB] admitted {} more people", n);
            }
        } catch (Exception e) {
            // If Redis hiccups for one tick, the next tick retries; do not let the
            // exception make Spring log a long stack trace every second and bury real errors.
            log.warn("[QUEUE-JOB] error while admitting: {}", e.getMessage());
        }
    }
}
