package com.hoangphuc.ddd.application.service.trip;

import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLockService;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLocker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Provisions a trip when the first person searches for that date.
 *
 * This is THE SAME PROBLEM as the cache stampede in TicketDetailCacheService,
 * except the expensive thing is not a SELECT but writing ~600 seat rows:
 *
 *   1. Ask whether the trip exists
 *   2. No -> take the lock; only one thread may provision
 *   3. Once the lock is held, ASK AGAIN (double-check)
 *   4. Generate the seats, release the lock
 *
 * Without step 3, 5000 people searching at once would provision 5000 times
 * one after another — queued instead of all at once. Result: 3 million
 * duplicate seat rows.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TripProvisionService {

    private static final long LOCK_WAIT_SECONDS = 3;
    /** Generating 600 seats takes ~1s; leave plenty of room for a busy DB. */
    private static final long LOCK_LEASE_SECONDS = 20;

    private final TripRepository tripRepository;
    private final DistributedLockService distributedLockService;
    private final TripCreationService tripCreationService;

    /**
     * Get the trip for (train, date). Provision it if it does not exist.
     *
     * @return a trip ready for sale, or empty if it could not be provisioned
     */
    public Optional<Trip> ensureTrip(Train train, LocalDate serviceDate) {
        // ---- STEP 1: does it exist ----
        Optional<Trip> existing = tripRepository.findByTrainAndDate(train.getId(), serviceDate);
        if (existing.isPresent()) {
            return existing;
        }

        // ---- STEP 2: take the lock ----
        String lockKey = "LOCK:TRIP:" + train.getId() + ":" + serviceDate;
        DistributedLocker locker = distributedLockService.getLock(lockKey);
        boolean locked = false;
        try {
            locked = locker.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                // Someone else is provisioning. Still not done after 3s -> read one last time.
                log.warn("[TRIP] could not acquire the lock | {}", lockKey);
                return tripRepository.findByTrainAndDate(train.getId(), serviceDate);
            }

            // ---- STEP 3: DOUBLE-CHECK ----
            existing = tripRepository.findByTrainAndDate(train.getId(), serviceDate);
            if (existing.isPresent()) {
                return existing;
            }

            // ---- STEP 4: only ONE thread gets here ----
            return Optional.of(tripCreationService.createTripWithSeats(train, serviceDate));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (locked) {
                locker.unlock();
            }
        }
    }

}
