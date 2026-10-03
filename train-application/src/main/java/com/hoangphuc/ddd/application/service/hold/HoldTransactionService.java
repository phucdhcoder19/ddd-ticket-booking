package com.hoangphuc.ddd.application.service.hold;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.repository.HoldPassengerRepository;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The transactions of the hold lifecycle. Kept apart from HoldAppService for
 * the two reasons written down in OrderTransactionService, which still apply:
 *
 *   1. @Transactional only works when called FROM OUTSIDE the class (through
 *      the Spring proxy). Calling this.releaseOne(...) inside the same class
 *      silently loses the transaction.
 *   2. A transactional method must NOT swallow exceptions with try/catch — if
 *      it does, Spring thinks everything is fine and COMMITs. Catching errors
 *      is the caller's job.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HoldTransactionService {

    private final HoldRepository holdRepository;
    private final SeatRepository seatRepository;
    private final HoldPassengerRepository holdPassengerRepository;
    private final JourneyPricingService journeyPricingService;

    /**
     * WRITE THE HOLD + CLAIM SEATS in one transaction.
     *
     * The hold must be written FIRST: the seat UPDATE needs the holdId to put
     * into seat.hold_id, and the holdId only exists after the INSERT.
     *
     * Three steps, and step 2 is the one that decides:
     *
     *   ① INSERT hold — just a piece of paper, nobody's seat is taken yet
     *   ② UPDATE ... WHERE status = 0 — claim seats, returns how many were claimed
     *   ③ missing even one seat -> THROW -> roll back both ① and ②
     *
     * Why one missing seat ruins the whole hold: a group of three picks three
     * berths in the same compartment. Holding two of them and saying "one is
     * missing" is something nobody wants to buy, yet those berths would stay
     * locked for 10 minutes and unsellable to anyone. All or nothing.
     *
     * @throws SeatUnavailableException when someone else was faster
     */
    @Transactional(rollbackFor = Exception.class)
    public Hold createSeatHold(HoldCommand command, Journey journey,
                               String holdCode, LocalDateTime expireAt) {
        LocalDateTime now = LocalDateTime.now();

        Hold hold = holdRepository.save(new Hold()
                .setHoldCode(holdCode)
                .setUserId(command.userId())
                .setTripId(command.tripId())
                .setFromCode(command.fromCode())
                .setToCode(command.toCode())
                .setSeatCount(command.seatIds().size())
                .setTotalAmount(0L)
                .setStatus(Hold.STATUS_HOLDING)
                .setExpireAt(expireAt)
                .setCreatedAt(now)
                .setUpdatedAt(now));

        int claimed = seatRepository.claimForHold(command.tripId(), command.seatIds(), hold.getId());
        if (claimed != command.seatIds().size()) {
            throw new SeatUnavailableException(
                    "Requested " + command.seatIds().size() + " seats, claimed only " + claimed);
        }

        // The total is computed FROM THE REAL SEATS just claimed, not summed
        // from the list the client sent: only the seat in the DB knows its
        // berth level, and the level is a factor of the price.
        List<Seat> seats = seatRepository.findByHold(hold.getId());
        long total = 0L;
        for (Seat seat : seats) {
            total += journeyPricingService.fareOf(journey, seat);
        }
        hold.setTotalAmount(total);
        return holdRepository.save(hold);
    }

    /**
     * WRITE THE PASSENGER LIST — only while the hold is alive.
     *
     *   ① lockIfHolding(): if still valid, lock the hold row until the transaction ends
     *   ② delete the old list, write the new one
     *
     * Checking "still valid" with if (hold.isHolding(now)) in the layer above
     * is NOT ENOUGH: between the read and the write, the job may release it,
     * or another tab of the same customer may press pay. Folding the check into
     * the UPDATE makes MySQL check the latest value and hold the lock until COMMIT.
     *
     * @return the list just written, or null if the hold can no longer be used
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HoldPassenger> replacePassengers(Hold hold, List<HoldPassenger> passengers) {
        int locked = holdRepository.lockIfHolding(hold.getId(), LocalDateTime.now());
        if (locked == 0) {
            return null;
        }
        return holdPassengerRepository.replaceForHold(hold.getId(), passengers);
    }

    /**
     * RETURN THE SEATS of a hold. Shared by both paths:
     * the user cancelling, and the expiry job.
     *
     * ORDER MATTERS — mark FIRST, free the seats AFTER:
     *
     *   ① markReleased() has WHERE status = 0
     *      -> 0 rows means someone else already handled it; back off and
     *         DO NOT touch the seats. This is what prevents releasing twice.
     *   ② only whoever changed 1 row may free the seats.
     *
     * Done the other way round (free seats first, mark after), a crash in
     * between leaves the seats free while the hold still has status = 0 — the
     * next scan runs again, and this time the seats may belong to someone else.
     *
     * releaseByHold() has one more guard: WHERE seat.status = 1. A sold seat
     * (status = 2) is never pulled back to free.
     *
     * @return true if THIS CALL released the seats
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseOne(Hold hold) {
        int changed = holdRepository.markReleased(hold.getId(), LocalDateTime.now());
        if (changed == 0) {
            log.debug("[HOLD] skipped, already handled | holdCode={}", hold.getHoldCode());
            return false;
        }
        int freed = seatRepository.releaseByHold(hold.getId());
        log.debug("[HOLD] returned {} seats | holdCode={}", freed, hold.getHoldCode());
        return true;
    }
}
