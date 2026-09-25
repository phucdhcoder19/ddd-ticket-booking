package com.hoangphuc.ddd.application.service.hold;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hai transaction của vòng đời giữ chỗ. Tách khỏi HoldAppService vì hai lý do
 * đã ghi ở OrderTransactionService, và chúng vẫn đúng ở đây:
 *
 *   1. @Transactional chỉ ăn khi được gọi TỪ NGOÀI class (qua proxy Spring).
 *      Gọi this.releaseOne(...) trong cùng class là mất transaction, lặng lẽ.
 *   2. Method trong transaction KHÔNG được try/catch nuốt exception — nuốt
 *      thì Spring tưởng mọi thứ ổn và COMMIT. Bắt lỗi là việc của tầng gọi.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HoldTransactionService {

    private final HoldRepository holdRepository;
    private final SeatRepository seatRepository;
    private final JourneyPricingService journeyPricingService;

    /**
     * GHI LƯỢT GIỮ + GIÀNH GHẾ trong cùng 1 transaction.
     *
     * Thứ tự bắt buộc là ghi hold TRƯỚC: câu UPDATE ghế cần có holdId để ghi
     * vào cột seat.hold_id, mà holdId chỉ có sau khi INSERT xong.
     *
     * Ba bước, và bước 2 là chỗ quyết định:
     *
     *   ① INSERT hold — mới chỉ là một tờ giấy, chưa chiếm chỗ của ai
     *   ② UPDATE ... WHERE status = 0 — giành ghế, trả về số ghế giành được
     *   ③ thiếu dù chỉ một ghế -> NÉM LỖI -> rollback cả ① lẫn ②
     *
     * Vì sao thiếu một ghế là hỏng cả lượt: khách đi ba người chọn ba giường
     * cùng khoang. Giữ cho họ hai giường rồi báo "còn thiếu một" là thứ không
     * ai muốn mua, mà ba giường đó vẫn bị khoá 10 phút không bán được cho ai.
     * Hoặc đủ, hoặc trả lại hết.
     *
     * @throws SeatUnavailableException khi có người nhanh tay hơn
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
                    "Xin " + command.seatIds().size() + " cho, chi gianh duoc " + claimed);
        }

        // Tổng tiền tính TỪ GHẾ THẬT vừa giành được, không cộng từ danh sách
        // client gửi lên: chỉ ghế trong DB mới biết mình ở tầng mấy, mà tầng
        // giường là một thừa số của giá.
        List<Seat> seats = seatRepository.findByHold(hold.getId());
        long total = 0L;
        for (Seat seat : seats) {
            total += journeyPricingService.fareOf(journey, seat);
        }
        hold.setTotalAmount(total);
        return holdRepository.save(hold);
    }

    /**
     * TRẢ CHỖ cho một lượt giữ. Dùng chung cho cả hai đường:
     * user bấm huỷ, và job quét hết hạn.
     *
     * THỨ TỰ LÀ QUAN TRỌNG — đánh dấu TRƯỚC, trả ghế SAU:
     *
     *   ① markReleased() có điều kiện WHERE status = 0
     *      -> 0 dòng nghĩa là người khác đã xử lý xong rồi, mình rút lui,
     *         KHÔNG đụng vào ghế. Đây là thứ chặn trả chỗ hai lần.
     *   ② chỉ người sửa được 1 dòng mới có quyền thả ghế.
     *
     * Làm ngược lại (thả ghế trước, đánh dấu sau) thì crash ở giữa sẽ để lại
     * ghế đã trống mà hold vẫn status = 0 — lượt quét sau chạy lại lần nữa,
     * và lần này ghế có thể đã thuộc về khách khác.
     *
     * releaseByHold() còn một lớp chắn nữa: WHERE seat.status = 1. Ghế đã
     * bán (status = 2) không bao giờ bị kéo ngược về trống.
     *
     * @return true nếu CHÍNH LẦN GỌI NÀY trả được chỗ
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseOne(Hold hold) {
        int changed = holdRepository.markReleased(hold.getId(), LocalDateTime.now());
        if (changed == 0) {
            log.debug("[HOLD] bo qua, da co nguoi xu ly | holdCode={}", hold.getHoldCode());
            return false;
        }
        int freed = seatRepository.releaseByHold(hold.getId());
        log.debug("[HOLD] tra lai {} cho | holdCode={}", freed, hold.getHoldCode());
        return true;
    }
}
