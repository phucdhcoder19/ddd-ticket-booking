package com.hoangphuc.ddd.application.service.refund;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.repository.HoldPassengerRepository;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Refund = two writes that must happen together:
 *   ① mark the ticket refunded     (hold_passenger)
 *   ② put its seat back on sale    (seat)
 *
 * ① without ② : the customer gets the money back AND the seat stays sold — lost stock.
 * ② without ① : the seat is resold while the old ticket is still valid — two people, one seat.
 *
 * Separate class for the same reason as OrderTransactionService:
 * @Transactional only works through the Spring proxy, and exceptions must
 * escape so Spring rolls back.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefundTransactionService {

    private final HoldPassengerRepository holdPassengerRepository;
    private final SeatRepository seatRepository;

    /**
     * @return true = refunded by this call, false = already refunded (lost the race)
     * @throws IllegalStateException when the seat is not sold to this order — rolls ① back
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(HoldPassenger passenger, long fee, long amount) {
        // ① The conditional UPDATE decides who wins: a second request changes 0 rows
        int marked = holdPassengerRepository.markRefunded(passenger.getId(), fee, amount, LocalDateTime.now());
        if (marked == 0) {
            return false;
        }

        // ② Exactly one seat must go back. Anything else means the data is
        // inconsistent (seat not sold to this order): refuse rather than pay
        // money back for a seat we cannot take back.
        int released = seatRepository.releaseSold(passenger.getOrderId(), passenger.getSeatCode());
        if (released != 1) {
            throw new IllegalStateException("Seat " + passenger.getSeatCode()
                    + " is not sold to order " + passenger.getOrderId() + ", released=" + released);
        }
        log.info("[REFUND-TX] ticket={} seat={} back on sale", passenger.getId(), passenger.getSeatCode());
        return true;
    }
}
