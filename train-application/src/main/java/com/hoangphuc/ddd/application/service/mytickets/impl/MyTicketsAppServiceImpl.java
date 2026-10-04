package com.hoangphuc.ddd.application.service.mytickets.impl;

import com.hoangphuc.ddd.application.model.PassengerTicketDTO;
import com.hoangphuc.ddd.application.service.mytickets.MyTicketsAppService;
import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.Station;
import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.HoldPassengerRepository;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.domain.repository.TicketOrderRepository;
import com.hoangphuc.ddd.domain.repository.TrainRepository;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import com.hoangphuc.ddd.domain.service.StationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "My tickets" — a READ-ONLY view stitched together from six tables:
 *
 *   ticket_order    → which orders the user paid for
 *   hold_passenger  → who travels, and the price each one paid
 *   seat            → carriage and seat number
 *   trip + train    → service date, train code, departure time
 *   station         → station names for the codes stored on the order
 *
 * The number of queries does NOT grow with the number of tickets: orders,
 * passengers and seats are each fetched once with an IN (...) query. Only
 * trips and trains are looked up per distinct id, and a user rarely has
 * more than a handful of different trips.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MyTicketsAppServiceImpl implements MyTicketsAppService {

    /** Cap on orders returned. A ticket list is not a reporting tool; older orders can get paging later. */
    private static final int MAX_ORDERS = 50;

    private final TicketOrderRepository ticketOrderRepository;
    private final HoldPassengerRepository holdPassengerRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;
    private final TrainRepository trainRepository;
    private final StationDomainService stationDomainService;

    @Override
    public List<PassengerTicketDTO> listTickets(Long userId) {
        List<TicketOrder> orders = ticketOrderRepository.findSeatOrdersByUser(userId, MAX_ORDERS);
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(TicketOrder::getId).toList();

        // Passengers grouped by order, keeping the order in which they were entered
        Map<Long, List<HoldPassenger>> passengersByOrder = new LinkedHashMap<>();
        for (HoldPassenger p : holdPassengerRepository.findByOrders(orderIds)) {
            passengersByOrder.computeIfAbsent(p.getOrderId(), k -> new ArrayList<>()).add(p);
        }

        // A seat is identified by (order, seat code): the same code "C7-1" exists on every trip
        Map<String, Seat> seatByKey = new HashMap<>();
        for (Seat s : seatRepository.findByOrders(orderIds)) {
            seatByKey.put(s.getOrderId() + "|" + s.getSeatCode(), s);
        }

        Map<String, String> stationNames = new HashMap<>();
        for (Station s : stationDomainService.listStations()) {
            stationNames.put(s.getCode(), s.getName());
        }

        Map<Long, Optional<Trip>> trips = new HashMap<>();
        Map<Long, Optional<Train>> trains = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        List<PassengerTicketDTO> result = new ArrayList<>();
        for (TicketOrder order : orders) {
            String status = ticketStatus(order);
            if (status == null) {
                continue;   // not paid yet: no ticket has been issued
            }
            Optional<Trip> trip = trips.computeIfAbsent(order.getTripId(), tripRepository::findById);
            Optional<Train> train = trip.flatMap(t -> trains.computeIfAbsent(t.getTrainId(), trainRepository::findById));
            LocalDateTime departAt = trip.isPresent() && train.isPresent()
                    ? trip.get().getServiceDate().atTime(train.get().getDepartHour(), train.get().getDepartMinute())
                    : null;

            List<HoldPassenger> passengers = passengersByOrder.getOrDefault(order.getId(), List.of());
            for (int i = 0; i < passengers.size(); i++) {
                HoldPassenger p = passengers.get(i);
                Seat seat = seatByKey.get(order.getId() + "|" + p.getSeatCode());

                PassengerTicketDTO dto = new PassengerTicketDTO();
                dto.setId(String.valueOf(p.getId()));
                dto.setCode(order.getOrderNumber() + "-" + (i + 1));
                dto.setOrderCode(order.getOrderNumber());
                dto.setTrainCode(train.map(Train::getCode).orElse("?"));
                dto.setFromStation(stationNames.getOrDefault(order.getFromCode(), order.getFromCode()));
                dto.setToStation(stationNames.getOrDefault(order.getToCode(), order.getToCode()));
                dto.setDepartAt(departAt);
                dto.setCarriageNumber(seat != null ? seat.getCarriageNumber() : 0);
                dto.setSeatLabel(seat != null ? seat.getLabel() : p.getSeatCode());
                dto.setSeatClass(seat != null ? seat.getSeatClass() : null);
                dto.setPassengerName(p.getFullName());
                dto.setPassengerId(mask(p.getIdNumber()));
                dto.setPrice(p.getFinalPrice());
                // A paid ticket whose train has already left counts as used
                dto.setStatus("VALID".equals(status) && departAt != null && departAt.isBefore(now) ? "USED" : status);
                dto.setQrPayload(qrPayload(dto));
                result.add(dto);
            }
        }
        log.info("[MY-TICKETS] user={} orders={} tickets={}", userId, orders.size(), result.size());
        return result;
    }

    /** null = the order has not issued tickets (still pending payment). */
    private String ticketStatus(TicketOrder order) {
        return switch (order.getOrderStatus()) {
            case TicketOrder.STATUS_PAID -> "VALID";
            case TicketOrder.STATUS_CANCELLED -> "REFUNDED";
            default -> null;
        };
    }

    /**
     * What the conductor's scanner reads. Deliberately WITHOUT the ID number:
     * the QR ends up in screenshots shared in family group chats, and anyone
     * scanning one would get the passenger's full national ID. The conductor
     * checks the printed name against the ID card instead.
     */
    private String qrPayload(PassengerTicketDTO t) {
        return String.join("|",
                t.getCode(),
                t.getTrainCode(),
                "C" + t.getCarriageNumber(),
                t.getSeatLabel(),
                t.getDepartAt() != null ? t.getDepartAt().toString() : "");
    }

    /** 001203004567 -> 001•••••4567. Same format the frontend uses, so masking twice changes nothing. */
    private String mask(String id) {
        if (id == null || id.length() < 7) {
            return id;
        }
        return id.substring(0, 3) + "•".repeat(id.length() - 7) + id.substring(id.length() - 4);
    }
}
