package com.hoangphuc.ddd.application.service.refund.impl;

import com.hoangphuc.ddd.application.model.PassengerTicketDTO;
import com.hoangphuc.ddd.application.model.RefundResult;
import com.hoangphuc.ddd.application.service.mytickets.MyTicketsAppService;
import com.hoangphuc.ddd.application.service.refund.RefundAppService;
import com.hoangphuc.ddd.application.service.refund.RefundTransactionService;
import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.repository.HoldPassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refund one ticket:
 *
 *   ⓪ the ticket exists and belongs to the caller   - read through MyTicketsAppService,
 *                                                     so "refundable" uses the same
 *                                                     status the customer sees on screen
 *   ① not refunded yet, not used, not inside the cut-off before departure
 *   ② fee and amount computed HERE, never taken from the client
 *   ③ 1 transaction: mark refunded + seat back on sale
 *
 * Money: there is no payment gateway yet, so nothing is actually paid back.
 * refundAmount is recorded on the ticket for the payment integration to act on.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefundAppServiceImpl implements RefundAppService {

    private final MyTicketsAppService myTicketsAppService;
    private final HoldPassengerRepository holdPassengerRepository;
    private final RefundTransactionService refundTransactionService;

    /** Share of the ticket price kept as a fee. The frontend confirmation shows the same 20%. */
    @Value("${app.refund.fee-rate:0.2}")
    private double feeRate;

    /**
     * Refunds close this long before departure: a seat freed 10 minutes before
     * the train leaves cannot be sold again, so the railway would just lose it.
     */
    @Value("${app.refund.cutoff:24h}")
    private Duration cutoff;

    @Override
    public RefundResult refund(Long userId, Long ticketId) {
        log.info("[REFUND] request | user={} ticket={}", userId, ticketId);

        // ⓪
        Optional<PassengerTicketDTO> found = myTicketsAppService.findTicket(userId, ticketId);
        if (found.isEmpty()) {
            return RefundResult.fail(RefundResult.Status.TICKET_NOT_FOUND);
        }
        PassengerTicketDTO ticket = found.get();

        // ①
        if ("REFUNDED".equals(ticket.getStatus())) {
            return RefundResult.fail(RefundResult.Status.ALREADY_REFUNDED);
        }
        LocalDateTime departAt = ticket.getDepartAt();
        if (!"VALID".equals(ticket.getStatus()) || departAt == null
                || !LocalDateTime.now().isBefore(departAt.minus(cutoff))) {
            return RefundResult.fail(RefundResult.Status.TOO_LATE);
        }

        // ② Prices are whole thousands of VND, so 20% is exact; round anyway for other rates
        long fee = Math.round(ticket.getPrice() * feeRate);
        long amount = ticket.getPrice() - fee;

        // ③
        try {
            HoldPassenger passenger = holdPassengerRepository.findById(ticketId).orElseThrow();
            if (!refundTransactionService.refund(passenger, fee, amount)) {
                // Another request refunded it between ⓪ and ③
                return RefundResult.fail(RefundResult.Status.ALREADY_REFUNDED);
            }
        } catch (Exception e) {
            // Rolled back: the ticket is still valid and the seat still sold
            log.error("[REFUND] failed | ticket={}", ticketId, e);
            return RefundResult.fail(RefundResult.Status.ERROR);
        }

        log.info("[REFUND] done | ticket={} price={} fee={} amount={}", ticketId, ticket.getPrice(), fee, amount);
        return myTicketsAppService.findTicket(userId, ticketId)
                .map(RefundResult::success)
                .orElseGet(() -> RefundResult.fail(RefundResult.Status.ERROR));
    }
}
