package com.hoangphuc.ddd.application.service.hold.impl;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.model.HoldDTO;
import com.hoangphuc.ddd.application.model.HoldItemDTO;
import com.hoangphuc.ddd.application.model.HoldResult;
import com.hoangphuc.ddd.application.model.PassengerCommand;
import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import com.hoangphuc.ddd.application.service.hold.HoldTransactionService;
import com.hoangphuc.ddd.application.service.hold.SeatUnavailableException;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.application.service.queue.QueueAppService;
import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoldAppServiceImpl implements HoldAppService {

    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final HoldRepository holdRepository;
    private final HoldTransactionService holdTransactionService;
    private final JourneyPricingService journeyPricingService;
    private final TicketDetailDomainService ticketDetailDomainService;
    private final QueueAppService queueAppService;

    /**
     * How long a hold lasts. A Duration rather than a long of minutes, so the
     * config can say "10m" in production and "30s" when testing the job —
     * Spring converts the string to a Duration, no manual arithmetic.
     */
    @Value("${app.hold.duration:10m}")
    private Duration holdDuration;

    /** Max holds released per scan — keeps transactions short. */
    @Value("${app.hold.release-batch:200}")
    private int releaseBatch;

    /**
     * Per-seat hold flow:
     *
     *   ⓪ business gate    - no seat touched yet, free to bail out
     *   ① 1 transaction    - write the hold + claim seats, roll back both on failure
     *
     * Much shorter than the old quantity-based version because there is NO REDIS.
     * In the stock-count model, Redis sits in front of MySQL to turn away
     * thousands of requests fighting over one number. In the per-seat model
     * every seat is its own row, so two customers picking different seats never
     * touch each other — only when they pick the EXACT same seat does someone
     * have to referee, and MySQL's UPDATE ... WHERE status = 0 is enough for
     * that. Adding Redis would only add a second source of truth to keep in sync.
     */
    @Override
    public HoldResult createHold(HoldCommand command) {
        log.info("[HOLD] createHold | trip={} class={} seats={} {}->{}",
                command.tripId(), command.seatClass(), command.seatIds(),
                command.fromCode(), command.toCode());

        // ===== Waiting room gatekeeper =====
        // Checked BEFORE anything else, even before reading MySQL: the whole
        // point of the waiting room is to keep load outside the door, and if
        // people without a pass may still touch the DB, the door holds nothing
        // back. Without this line anyone calling the API directly skips the
        // queue — the waiting room becomes decoration.
        if (!queueAppService.isAdmitted(command.queueToken())) {
            log.info("[HOLD] rejected: not admitted by the waiting room | token={}", command.queueToken());
            return HoldResult.fail(HoldResult.Status.QUEUE_REQUIRED);
        }

        if (command.seatIds() == null || command.seatIds().isEmpty()) {
            return HoldResult.fail(HoldResult.Status.SEAT_TAKEN);
        }

        // ===== Business gate =====
        Optional<Trip> found = tripRepository.findById(command.tripId());
        if (found.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.TRIP_NOT_FOUND);
        }
        Trip trip = found.get();

        Optional<Journey> journey = journeyPricingService.resolve(
                command.fromCode(), command.toCode(), trip.getServiceDate());
        if (journey.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.INVALID_ROUTE);
        }

        LocalDateTime now = LocalDateTime.now();
        // isSaleOpen(), NOT resolveSaleWindow(): the latter prefers the upcoming
        // sale so the home page can count down, so it says "not open" even while
        // the current sale is running. Using it here locks the door right when
        // the crowd arrives.
        if (!ticketDetailDomainService.isSaleOpen(now)) {
            return HoldResult.fail(HoldResult.Status.NOT_ON_SALE);
        }
        // A train that left yesterday has nothing left to sell. This must be
        // checked separately rather than relying on whether seats are free:
        // seats of a trip that already ran are still in the free state.
        if (trip.getServiceDate().isBefore(LocalDate.now())) {
            return HoldResult.fail(HoldResult.Status.SALE_ENDED);
        }

        // ===== One transaction =====
        String holdCode = "HOLD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        LocalDateTime expireAt = now.plus(holdDuration);
        try {
            Hold hold = holdTransactionService.createSeatHold(
                    command, journey.get(), holdCode, expireAt);

            log.info("[HOLD] held {} seats | holdCode={} expires at {}",
                    hold.getSeatCount(), holdCode, expireAt);
            return HoldResult.success(toDTO(hold, journey.get(), now));

        } catch (SeatUnavailableException e) {
            // The transaction HAS ROLLED BACK: the hold row is gone, and seats
            // that were claimed in time are free again. Nothing to clean up by hand.
            log.info("[HOLD] seat already taken | trip={} {}", command.tripId(), e.getMessage());
            return HoldResult.fail(HoldResult.Status.SEAT_TAKEN);

        } catch (Exception e) {
            log.error("[HOLD] error, MySQL rolled back | trip={}", command.tripId(), e);
            return HoldResult.fail(HoldResult.Status.ERROR);
        }
    }

    @Override
    public HoldResult getHold(String holdCode) {
        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.HOLD_NOT_FOUND);
        }
        Hold hold = found.get();
        LocalDateTime now = LocalDateTime.now();

        // Expired but the job has not reached it yet: answer with the TRUTH at
        // the moment of asking, do not wait for the job. Users do not need to
        // know the job runs every 10 seconds.
        if (!hold.isHolding(now)) {
            return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
        }
        return HoldResult.success(toDTO(hold, journeyOf(hold).orElse(null), now));
    }

    /**
     * The "enter passenger details" step between holding and payment.
     *
     *   ⓪ is the hold alive          - plain read, catches most expired cases early
     *   ① matches every seat         - exactly one person per seat, none extra, none missing
     *   ② fix each person's price    - seat fare × discount, computed ON THE SERVER
     *   ③ 1 transaction              - lock the hold, replace the old list with the new one
     */
    @Override
    public HoldResult savePassengers(String holdCode, List<PassengerCommand> passengers) {
        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.HOLD_NOT_FOUND);
        }
        Hold hold = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (!hold.isHolding(now)) {
            return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
        }

        // ① Compare SETS of seat codes, not counts: 2 people for 2 seats who
        // both claim seat C3-1 still match by count while nobody sits in C3-2.
        // With a set, a duplicate code makes the set smaller.
        List<Seat> seats = seatRepository.findByHold(hold.getId());
        Map<String, Seat> seatByCode = new HashMap<>();
        for (Seat seat : seats) {
            seatByCode.put(seat.getSeatCode(), seat);
        }
        Set<String> requested = new HashSet<>();
        for (PassengerCommand p : passengers) {
            requested.add(p.seatId());
        }
        if (requested.size() != passengers.size() || !requested.equals(seatByCode.keySet())) {
            log.info("[HOLD] passengers do not match seats | holdCode={} seats={} sent={}",
                    holdCode, seatByCode.keySet(), requested);
            return HoldResult.fail(HoldResult.Status.PASSENGER_MISMATCH);
        }

        // ② The base price comes from the REAL SEAT in the DB, the discount from
        // the RULE in the domain. The client only says "I am a student" and
        // never sends any amount of money.
        Optional<Journey> journey = journeyOf(hold);
        if (journey.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.ERROR);
        }
        List<HoldPassenger> rows = new ArrayList<>(passengers.size());
        for (PassengerCommand p : passengers) {
            long basePrice = journeyPricingService.fareOf(journey.get(), seatByCode.get(p.seatId()));
            rows.add(new HoldPassenger()
                    .setHoldId(hold.getId())
                    .setSeatCode(p.seatId())
                    .setFullName(p.fullName())
                    .setIdNumber(p.idNumber())
                    .setPhone(p.phone())
                    .setDiscount(p.discount().name())
                    .setBasePrice(basePrice)
                    .setFinalPrice(p.discount().apply(basePrice))
                    .setCreatedAt(now));
        }

        try {
            List<HoldPassenger> saved = holdTransactionService.replacePassengers(hold, rows);
            if (saved == null) {
                // Expired, or turned into an order, in the gap between ⓪ and ③.
                return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
            }
            log.info("[HOLD] saved {} passengers | holdCode={}", saved.size(), holdCode);
            return HoldResult.success(toDTO(hold, journey.get(), now));

        } catch (Exception e) {
            log.error("[HOLD] error saving passengers, MySQL rolled back | holdCode={}", holdCode, e);
            return HoldResult.fail(HoldResult.Status.ERROR);
        }
    }

    @Override
    public HoldResult releaseHold(String holdCode) {
        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.HOLD_NOT_FOUND);
        }

        boolean released = holdTransactionService.releaseOne(found.get());
        if (!released) {
            // 0 rows: the job just cleaned it up, or it has become an order.
            return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
        }

        log.info("[HOLD] cancelled by user | holdCode={}", holdCode);
        return HoldResult.success(null);
    }

    @Override
    public int releaseExpiredHolds() {
        List<Hold> expired = holdRepository.findExpired(LocalDateTime.now(), releaseBatch);
        int count = 0;
        for (Hold hold : expired) {
            try {
                if (holdTransactionService.releaseOne(hold)) {
                    count++;
                    log.info("[HOLD-JOB] released | holdCode={} trip={} seats={}",
                            hold.getHoldCode(), hold.getTripId(), hold.getSeatCount());
                }
            } catch (Exception e) {
                // One failure must not kill the whole batch — the next scan retries.
                log.error("[HOLD-JOB] error while releasing | holdCode={}", hold.getHoldCode(), e);
            }
        }
        return count;
    }

    /** Rebuild the journey stored on the hold, so each seat is priced correctly. */
    private Optional<Journey> journeyOf(Hold hold) {
        return tripRepository.findById(hold.getTripId())
                .flatMap(trip -> journeyPricingService.resolve(
                        hold.getFromCode(), hold.getToCode(), trip.getServiceDate()));
    }

    private HoldDTO toDTO(Hold hold, Journey journey, LocalDateTime now) {
        HoldDTO dto = new HoldDTO();
        dto.setHoldId(hold.getHoldCode());
        dto.setTripId(String.valueOf(hold.getTripId()));
        dto.setFromCode(hold.getFromCode());
        dto.setToCode(hold.getToCode());
        dto.setItems(itemsOf(hold, journey));
        dto.setTotalAmount(hold.getTotalAmount());
        dto.setExpiresAt(hold.getExpireAt());
        dto.setServerNow(now);
        dto.setSecondsLeft(hold.secondsLeft(now));
        dto.setStatus(hold.isHolding(now) ? "HOLDING" : "EXPIRED");
        return dto;
    }

    private List<HoldItemDTO> itemsOf(Hold hold, Journey journey) {
        List<HoldItemDTO> items = new ArrayList<>();
        for (Seat seat : seatRepository.findByHold(hold.getId())) {
            HoldItemDTO item = new HoldItemDTO();
            item.setSeatId(seat.getSeatCode());
            item.setSeatLabel(seat.getLabel());
            item.setCarriageNumber(seat.getCarriageNumber());
            item.setSeatClass(seat.getSeatClass());
            item.setPrice(journey != null ? journeyPricingService.fareOf(journey, seat) : 0L);
            items.add(item);
        }
        return items;
    }
}
