package com.hoangphuc.ddd.application.service.hold;

import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    private final TicketDetailDomainService ticketDetailDomainService;
    private final HoldRepository holdRepository;

    /**
     * TRỪ KHO + GHI LƯỢT GIỮ CHỖ trong cùng 1 transaction.
     * Lỗi ở bất kỳ đâu -> ROLLBACK cả hai, kho tự về số cũ.
     *
     * @return hold vừa tạo, hoặc null nếu MySQL báo không đủ vé
     */
    @Transactional(rollbackFor = Exception.class)
    public Hold deductStockAndCreateHold(Long ticketId, Long userId, int quantity,
                                         String holdCode, LocalDateTime expireAt) {
        boolean deducted = ticketDetailDomainService.decreaseStock(ticketId, quantity);
        if (!deducted) {
            return null;      // chưa ghi gì -> không có gì để rollback
        }

        LocalDateTime now = LocalDateTime.now();
        Hold hold = new Hold()
                .setHoldCode(holdCode)
                .setTicketId(ticketId)
                .setUserId(userId)
                .setQuantity(quantity)
                .setStatus(Hold.STATUS_HOLDING)
                .setExpireAt(expireAt)
                .setCreatedAt(now)
                .setUpdatedAt(now);

        return holdRepository.save(hold);
    }

    /**
     * TRẢ KHO cho một lượt giữ chỗ. Dùng chung cho cả hai đường:
     * user bấm huỷ, và job quét hết hạn.
     *
     * THỨ TỰ LÀ QUAN TRỌNG — đánh dấu TRƯỚC, hoàn kho SAU:
     *
     *   ① markReleased() có điều kiện WHERE status = 0
     *      -> 0 dòng nghĩa là người khác đã xử lý xong rồi, mình rút lui,
     *         KHÔNG đụng vào kho. Đây là thứ chặn hoàn kho hai lần.
     *   ② chỉ người sửa được 1 dòng mới có quyền cộng kho.
     *
     * Làm ngược lại (hoàn kho trước, đánh dấu sau) thì crash ở giữa sẽ để lại
     * kho đã cộng mà hold vẫn status=0 -> lượt quét sau cộng thêm lần nữa ->
     * kho tự sinh ra vé không tồn tại.
     *
     * @return true nếu CHÍNH LẦN GỌI NÀY trả được kho
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseOne(Hold hold) {
        int changed = holdRepository.markReleased(hold.getId(), LocalDateTime.now());
        if (changed == 0) {
            log.debug("[HOLD] bo qua, da co nguoi xu ly | holdCode={}", hold.getHoldCode());
            return false;
        }
        ticketDetailDomainService.increaseStock(hold.getTicketId(), hold.getQuantity());
        return true;
    }
}
