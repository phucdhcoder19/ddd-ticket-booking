package com.hoangphuc.ddd.application.cronjob;

import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Releases seats held by people who walked away.
 *
 * This is the problem lesson 18 exists to solve: when a customer closes the
 * tab, NOBODY TELLS THE SERVER ANYTHING. No exception, no cancel request. The
 * only way to find out is to keep watching the clock.
 *
 * fixedDelay (not fixedRate): wait N seconds AFTER the previous run finishes.
 * fixedRate fires on schedule whether or not the previous run is done -> with
 * 5000 expired holds two runs would overlap.
 *
 * NOTE with multiple instances: this job runs on EVERY server, all scanning
 * the same table. No distributed lock on purpose — the "WHERE status = 0"
 * clause in markReleased() is enough to make one side lose.
 * See HoldTransactionService.releaseOne().
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HoldReleaseJob {

    private final HoldAppService holdAppService;

    @Scheduled(fixedDelayString = "${app.hold.scan-interval-ms:10000}")
    public void releaseExpiredHolds() {
        int released = holdAppService.releaseExpiredHolds();
        if (released > 0) {
            log.info("[HOLD-JOB] released {} expired holds", released);
        }
    }
}
