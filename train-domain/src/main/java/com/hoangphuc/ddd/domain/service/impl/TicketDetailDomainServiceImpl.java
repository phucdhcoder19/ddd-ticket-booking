package com.hoangphuc.ddd.domain.service.impl;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.repository.TicketDetailRepository;
import com.hoangphuc.ddd.domain.model.vo.SaleWindow;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketDetailDomainServiceImpl implements TicketDetailDomainService {

    private final TicketDetailRepository ticketDetailRepository;

    @Override
    public TicketDetail getTicketDetailById(Long ticketId) {
        return ticketDetailRepository.findById(ticketId);
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        return ticketDetailRepository.getStockAvailable(ticketId);
    }

    @Override
    public boolean decreaseStock(Long ticketId, int quantity) {
        if (quantity <= 0) return false;        // validate here, do not let it reach SQL
        return ticketDetailRepository.decreaseStock(ticketId, quantity);
    }

    @Override
    public boolean increaseStock(Long ticketId, int quantity) {
        return ticketDetailRepository.increaseStock(ticketId, quantity);
    }

    /**
     * BUSINESS RULE: can people buy right now?
     *
     * Kept fully separate from resolveSaleWindow() even though they sound
     * alike, because they answer different questions:
     *
     *   resolveSaleWindow  "which moment should the screen count down to"
     *   isSaleOpen         "may this person book a seat right now"
     *
     * They disagree in exactly one situation, and it is a normal one, not a
     * rare one: sale 1 is running while sale 2 is scheduled for next month.
     * resolveSaleWindow returns "not open, counting down to sale 2" — right
     * for the clock on the home page. Use it as the gate and every customer
     * of sale 1 gets rejected just because another sale is queued behind it.
     */
    @Override
    public boolean isSaleOpen(LocalDateTime now) {
        return ticketDetailRepository.findLatestOpened(now)
                .filter(ticket -> !ticket.isSaleEnded(now))
                .isPresent();
    }

    /**
     * BUSINESS RULE: which sale window is in effect.
     *
     * Priority order — users care about "how long until I can buy":
     *   ① a ticket has not opened yet           -> count down to it, not open
     *   ② no, but one has opened and is still within its window  -> open
     *   ③ nothing (all ended, or no schedule configured)         -> closed
     *
     * Step ② reuses exactly the TicketDetail.isSaleEnded() that placeOrder()
     * uses as its gate — one rule, defined in one place.
     */
    @Override
    public SaleWindow resolveSaleWindow(LocalDateTime now) {
        Optional<TicketDetail> upcoming = ticketDetailRepository.findNextOpening(now);
        if (upcoming.isPresent()) {
            return new SaleWindow(upcoming.get().getSaleStartTime(), false);
        }

        Optional<TicketDetail> opened = ticketDetailRepository.findLatestOpened(now);
        if (opened.isPresent() && !opened.get().isSaleEnded(now)) {
            return new SaleWindow(opened.get().getSaleStartTime(), true);
        }

        return SaleWindow.closedAt(now);
    }
}
