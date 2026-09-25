package com.hoangphuc.ddd.application.service.hold.impl;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.model.HoldDTO;
import com.hoangphuc.ddd.application.model.HoldItemDTO;
import com.hoangphuc.ddd.application.model.HoldResult;
import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import com.hoangphuc.ddd.application.service.hold.HoldTransactionService;
import com.hoangphuc.ddd.application.service.hold.SeatUnavailableException;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.domain.model.entity.Hold;
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
import java.util.List;
import java.util.Optional;
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

    /**
     * Giữ bao lâu. Kiểu Duration chứ không phải long phút, để cấu hình viết
     * được "10m" khi chạy thật và "30s" khi ngồi test job — Spring tự đổi
     * chuỗi sang Duration, không phải tự nhân chia.
     */
    @Value("${app.hold.duration:10m}")
    private Duration holdDuration;

    /** Mỗi lượt quét dọn tối đa bao nhiêu — giữ transaction ngắn. */
    @Value("${app.hold.release-batch:200}")
    private int releaseBatch;

    /**
     * Luồng giữ chỗ theo ghế:
     *
     *   ⓪ cổng nghiệp vụ   - chưa đụng tới ghế nào, thoát tự do
     *   ① 1 transaction    - ghi hold + giành ghế, hỏng thì rollback cả hai
     *
     * Ngắn hơn hẳn bản giữ theo số lượng trước đây, vì KHÔNG CÒN REDIS.
     * Ở mô hình đếm kho, Redis đứng trước MySQL để chặn sớm hàng nghìn lượt
     * tranh nhau một con số. Ở mô hình theo ghế thì mỗi ghế là một dòng
     * riêng, hai khách chọn hai ghế khác nhau không hề đụng nhau — chỉ khi
     * chọn TRÙNG ĐÚNG một ghế mới phải phân xử, và lúc đó câu UPDATE ...
     * WHERE status = 0 của MySQL đã đủ làm trọng tài. Thêm Redis vào chỉ là
     * thêm một nguồn sự thật thứ hai phải giữ cho khớp.
     */
    @Override
    public HoldResult createHold(HoldCommand command) {
        log.info("[HOLD] createHold | trip={} hang={} cho={} {}->{}",
                command.tripId(), command.seatClass(), command.seatIds(),
                command.fromCode(), command.toCode());

        if (command.seatIds() == null || command.seatIds().isEmpty()) {
            return HoldResult.fail(HoldResult.Status.SEAT_TAKEN);
        }

        // ===== Cổng nghiệp vụ =====
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
        // isSaleOpen(), KHÔNG phải resolveSaleWindow(): cái sau ưu tiên đợt
        // sắp tới để trang chủ đếm ngược, nên nó trả "chưa mở" ngay cả khi
        // đợt hiện tại đang bán. Dùng nhầm là khoá cửa đúng lúc đông khách.
        if (!ticketDetailDomainService.isSaleOpen(now)) {
            return HoldResult.fail(HoldResult.Status.NOT_ON_SALE);
        }
        // Tàu chạy hôm qua thì không còn gì để bán. Phải kiểm riêng chứ không
        // dựa vào việc ghế còn trống hay không: ghế của chuyến đã chạy xong
        // vẫn đang ở trạng thái trống.
        if (trip.getServiceDate().isBefore(LocalDate.now())) {
            return HoldResult.fail(HoldResult.Status.SALE_ENDED);
        }

        // ===== Một transaction =====
        String holdCode = "HOLD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        LocalDateTime expireAt = now.plus(holdDuration);
        try {
            Hold hold = holdTransactionService.createSeatHold(
                    command, journey.get(), holdCode, expireAt);

            log.info("[HOLD] giu {} cho OK | holdCode={} het han luc {}",
                    hold.getSeatCount(), holdCode, expireAt);
            return HoldResult.success(toDTO(hold, journey.get(), now));

        } catch (SeatUnavailableException e) {
            // Transaction ĐÃ ROLLBACK: dòng hold biến mất, những ghế vừa kịp
            // giành được cũng trở lại trống. Không phải dọn tay gì cả.
            log.info("[HOLD] cho da co nguoi giu | trip={} {}", command.tripId(), e.getMessage());
            return HoldResult.fail(HoldResult.Status.SEAT_TAKEN);

        } catch (Exception e) {
            log.error("[HOLD] loi, MySQL da rollback | trip={}", command.tripId(), e);
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

        // Quá hạn nhưng job chưa kịp quét tới: trả lời theo SỰ THẬT ngay lúc
        // hỏi, đừng đợi job. Người dùng không cần biết job chạy mỗi 10 giây.
        if (!hold.isHolding(now)) {
            return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
        }
        return HoldResult.success(toDTO(hold, journeyOf(hold).orElse(null), now));
    }

    @Override
    public HoldResult releaseHold(String holdCode) {
        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.HOLD_NOT_FOUND);
        }

        boolean released = holdTransactionService.releaseOne(found.get());
        if (!released) {
            // 0 dòng: job vừa dọn xong, hoặc đã đổi thành đơn hàng.
            return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
        }

        log.info("[HOLD] user huy | holdCode={}", holdCode);
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
                    log.info("[HOLD-JOB] thu hoi | holdCode={} trip={} so cho={}",
                            hold.getHoldCode(), hold.getTripId(), hold.getSeatCount());
                }
            } catch (Exception e) {
                // Một lượt lỗi không được làm chết cả lô — lượt quét sau thử lại.
                log.error("[HOLD-JOB] loi khi thu hoi | holdCode={}", hold.getHoldCode(), e);
            }
        }
        return count;
    }

    /** Dựng lại hành trình đã lưu trên hold, để tính đúng giá từng chỗ. */
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
