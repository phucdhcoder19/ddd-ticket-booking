package com.hoangphuc.ddd.application.service.ticket;

import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import com.hoangphuc.ddd.domain.repository.HoldPassengerRepository;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.domain.repository.TicketOrderRepository;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * LESSON 21 — DATA CONSISTENCY.
 *
 * "Deduct MySQL stock" and "create the order" live in the SAME transaction:
 *   - both succeed     -> COMMIT
 *   - either one fails -> ROLLBACK both, stock goes back to its old value
 *
 * A separate class for 2 reasons:
 *   1. @Transactional only works when called FROM OUTSIDE the class (through the Spring proxy).
 *   2. These methods must NOT swallow exceptions with try/catch — if they do,
 *      Spring thinks everything is fine and COMMITs. Catching errors and
 *      restoring Redis is the caller's job.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderTransactionService {

    private final TicketDetailDomainService ticketDetailDomainService;
    private final TicketOrderRepository ticketOrderRepository;
    private final HoldRepository holdRepository;
    private final HoldPassengerRepository holdPassengerRepository;
    private final SeatRepository seatRepository;

    /** Set to true in application.yml to deliberately fail AFTER deducting stock -> watch the rollback. */
    @Value("${app.demo.fail-after-deduct:false}")
    private boolean failAfterDeduct;

    /**
     * @return the order just created, or null if MySQL says there is not enough stock
     * @throws RuntimeException on error — the transaction has been ROLLED BACK
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder deductStockAndCreateOrder(Long ticketId, Long userId,
                                                 int quantity, BigDecimal unitPrice) {
        // ① Deduct MySQL stock (option 1). Not really written yet — "pending" inside the transaction.
        boolean deducted = ticketDetailDomainService.decreaseStock(ticketId, quantity);
        if (!deducted) {
            return null;    // nothing written -> nothing to roll back
        }

        if (failAfterDeduct) {
            log.warn("[TX] DEMO: throwing on purpose AFTER deducting stock, ticketId={}", ticketId);
            throw new IllegalStateException("DEMO rollback: simulated error after stock deduction");
        }

        // ② Create the order — same transaction as ①
        LocalDateTime now = LocalDateTime.now();
        TicketOrder order = new TicketOrder()
                .setOrderNumber(generateOrderNumber())
                .setUserId(userId)
                .setTicketId(ticketId)
                .setQuantity(quantity)
                .setUnitPrice(unitPrice)
                .setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .setOrderStatus(TicketOrder.STATUS_PENDING)
                .setCreatedAt(now)
                .setUpdatedAt(now);

        TicketOrder saved = ticketOrderRepository.save(order);
        log.info("[TX] stock deducted + order created | orderNumber={}", saved.getOrderNumber());
        return saved;
        // normal return -> Spring COMMITs both ① and ②
    }

    /**
     * LESSON 18 — FINAL STEP: turn a hold into an order.
     *
     * NO SEATS ARE CLAIMED AGAIN here. They were taken back at POST /holds.
     * This step only moves ownership of those seats from "temporarily held"
     * to "sold" — a state change, not another claim.
     *
     * markUsed() has WHERE status = 0 AND expireAt > now. It is the race
     * between a customer confirming at second 599 and the job scanning at 600:
     *   - customer wins -> the job sees 0 rows, frees nothing, customer keeps the seats
     *   - job wins      -> we get 0 rows here, NO order is created, the caller reports expiry
     *
     * Without that clause comes the worst case: the customer pays, and the
     * seats still go back to stock and get sold to someone else. One seat,
     * two people holding tickets.
     *
     * The final sold == seatCount check is a safety latch, not redundancy: if
     * for some reason a seat has left this hold, the order would charge for
     * more seats than it can deliver. Better to roll back.
     *
     * @return the order just created, or null if the hold has expired / was already used
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder convertSeatHoldToOrder(Hold hold) {
        LocalDateTime now = LocalDateTime.now();

        int changed = holdRepository.markUsed(hold.getId(), now);
        if (changed == 0) {
            log.info("[TX] hold can no longer be used | holdCode={}", hold.getHoldCode());
            return null;
        }

        // Read the passengers AFTER markUsed(): we now hold the row lock on the
        // hold, so a PUT /passengers is either fully done or has to wait for us
        // — we never read a half-written list.
        List<HoldPassenger> passengers = holdPassengerRepository.findByHold(hold.getId());
        if (passengers.size() != hold.getSeatCount()) {
            throw new PassengersMissingException(
                    "Hold " + hold.getHoldCode() + " has " + hold.getSeatCount()
                            + " seats but only " + passengers.size() + " passengers");
        }
        long payable = 0L;
        for (HoldPassenger p : passengers) {
            payable += p.getFinalPrice();
        }

        TicketOrder order = ticketOrderRepository.save(new TicketOrder()
                .setOrderNumber(generateOrderNumber())
                .setUserId(hold.getUserId())
                .setTripId(hold.getTripId())
                .setFromCode(hold.getFromCode())
                .setToCode(hold.getToCode())
                .setQuantity(hold.getSeatCount())
                // Sum each passenger's FIXED price, NO recomputation: the seat
                // fare was fixed at hold time, the discount when details were
                // entered. Recomputing here opens the door to the customer
                // seeing one price on screen and being charged another.
                .setTotalAmount(BigDecimal.valueOf(payable))
                // No real payment gateway yet, so confirming counts as paid.
                // Lesson 25 inserts a payment step in between: hold -> payment -> order.
                .setOrderStatus(TicketOrder.STATUS_PAID)
                .setPaidAt(now)
                .setCreatedAt(now)
                .setUpdatedAt(now));

        int sold = seatRepository.sellByHold(hold.getId(), order.getId());
        if (sold != hold.getSeatCount()) {
            throw new IllegalStateException(
                    "Order " + order.getOrderNumber() + " needs " + hold.getSeatCount()
                            + " seats but only " + sold + " could be sold");
        }
        holdPassengerRepository.attachToOrder(hold.getId(), order.getId());

        log.info("[TX] hold -> order OK | holdCode={} orderNumber={} seats={}",
                hold.getHoldCode(), order.getOrderNumber(), sold);
        return order;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
