package com.hoangphuc.ddd.application.service.queue.impl;

import com.hoangphuc.ddd.application.model.QueueTicketDTO;
import com.hoangphuc.ddd.application.service.queue.QueueAppService;
import com.hoangphuc.ddd.infrastructure.cache.redis.WaitingRoomRedisStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * WAITING ROOM — like the ticket dispenser at a bank.
 *
 *   join()        take a number, stand at the back of the queue
 *   admitNext()   every second let more people in, so the inside never exceeds CAPACITY
 *   isAdmitted()  the POST /holds gatekeeper asks: does this person hold a valid admission
 *   leave()       done buying, step out and give the slot to the next person
 *
 * All state lives in Redis, not in server memory: with 3 servers, all 3 see
 * ONE queue. A Java List would give each server its own queue — someone on
 * server A could not know their place relative to someone on server B.
 *
 * Why a waiting room is needed even with the Tomcat/Hikari pools (lesson 27):
 * pools count REQUESTS and reject when full; the waiting room counts PEOPLE
 * and lets them queue. Pools protect the machine, the waiting room protects
 * the buyers. Both layers exist side by side.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QueueAppServiceImpl implements QueueAppService {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final WaitingRoomRedisStore store;

    /**
     * When disabled, the waiting room becomes an open door again: everyone
     * gets in immediately and the gatekeeper blocks nobody. Useful when
     * JMeter fires straight at /holds.
     */
    @Value("${app.queue.enabled:true}")
    private boolean enabled;

    /** Max number of people "inside" (picking seats / paying) at the same time. */
    @Value("${app.queue.capacity:500}")
    private int capacity;

    /**
     * Max people admitted per job run. Do not let a hundred people in at once
     * even when a hundred slots are free: they would all hit trip search in the
     * same second and turn the load into a single punch. Admitting gradually
     * spreads the load too.
     */
    @Value("${app.queue.max-admit-per-tick:50}")
    private int maxAdmitPerTick;

    /** Once admitted, how long a person has to buy before losing the slot. */
    @Value("${app.queue.admission-window:15m}")
    private Duration admissionWindow;

    /**
     * Estimated time a person spends inside (pick seats + enter names + pay).
     * Only used to show "about X minutes left" to people waiting.
     */
    @Value("${app.queue.estimated-session-seconds:180}")
    private int estimatedSessionSeconds;

    @Override
    public QueueTicketDTO join() {
        String token = "Q-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        if (!enabled) {
            return admitted(token, Instant.now().plus(admissionWindow).toEpochMilli());
        }

        store.enqueue(token);
        // Admit once right away instead of waiting for the job: when it is quiet
        // and there is room inside, the customer walks straight in instead of
        // staring at a pointless waiting screen for a second.
        // Safe with many concurrent requests because admit() is one atomic Lua script.
        admitNext();

        log.info("[QUEUE] joined | token={}", token);
        return status(token).orElseThrow();
    }

    @Override
    public Optional<QueueTicketDTO> status(String token) {
        if (!enabled) {
            return Optional.of(admitted(token, Instant.now().plus(admissionWindow).toEpochMilli()));
        }

        // Admitted and the admission is still valid -> come in
        Long until = store.admittedUntil(token);
        if (until != null && until > System.currentTimeMillis()) {
            return Optional.of(admitted(token, until));
        }

        // Still in the queue -> report the position
        Long rank = store.rankInQueue(token);
        if (rank != null) {
            return Optional.of(waiting(token, rank));
        }

        // Nowhere: a made-up token, or admitted but left to expire.
        // The client will join again at the back.
        return Optional.empty();
    }

    @Override
    public long admitNext() {
        if (!enabled) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long expireAt = now + admissionWindow.toMillis();
        return store.admit(now, capacity, maxAdmitPerTick, expireAt);
    }

    @Override
    public boolean isAdmitted(String token) {
        if (!enabled) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        Long until = store.admittedUntil(token);
        return until != null && until > System.currentTimeMillis();
    }

    @Override
    public void leave(String token) {
        if (!enabled || token == null || token.isBlank()) {
            return;
        }
        store.leave(token);
        log.info("[QUEUE] purchase done, slot freed | token={}", token);
    }

    private QueueTicketDTO waiting(String token, long peopleAhead) {
        QueueTicketDTO dto = new QueueTicketDTO();
        dto.setToken(token);
        dto.setPosition((int) peopleAhead);
        dto.setTotal((int) store.waitingCount());
        dto.setEstimatedWaitSeconds(estimateWaitSeconds(peopleAhead));
        dto.setStatus("WAITING");
        return dto;
    }

    private QueueTicketDTO admitted(String token, long untilMs) {
        QueueTicketDTO dto = new QueueTicketDTO();
        dto.setToken(token);
        dto.setPosition(0);
        dto.setTotal(0);
        dto.setEstimatedWaitSeconds(0);
        dto.setStatus("ADMITTED");
        dto.setAdmissionExpiresAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(untilMs), ZONE));
        return dto;
    }

    /**
     * Rough estimate: there are `capacity` slots inside and each person stays
     * ~3 minutes, so about capacity / 180 slots free up every second. Someone
     * at position N waits about N / (capacity / 180) seconds.
     *
     * A large error is fine — the goal is for people to see the number go down
     * and not press F5, not to promise an exact time.
     */
    private int estimateWaitSeconds(long peopleAhead) {
        double releasedPerSecond = (double) capacity / Math.max(estimatedSessionSeconds, 1);
        return (int) Math.ceil(peopleAhead / Math.max(releasedPerSecond, 0.01));
    }
}
