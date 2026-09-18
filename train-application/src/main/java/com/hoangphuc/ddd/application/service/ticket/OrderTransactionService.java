package com.hoangphuc.ddd.application.service.ticket;

import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.repository.TicketOrderRepository;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BÀI 21 — NHẤT QUÁN DỮ LIỆU.
 *
 * "Trừ kho MySQL" và "tạo đơn" nằm trong CÙNG 1 transaction:
 *   - cả hai thành công  -> COMMIT
 *   - bất kỳ cái nào lỗi -> ROLLBACK cả hai, kho tự quay về số cũ
 *
 * Tách thành class riêng vì 2 lý do:
 *   1. @Transactional chỉ chạy khi được gọi TỪ BÊN NGOÀI class (qua proxy của Spring).
 *   2. Method này KHÔNG được try/catch nuốt exception — nuốt thì Spring tưởng
 *      mọi thứ ổn và COMMIT. Việc bắt lỗi + hoàn Redis để tầng gọi nó lo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderTransactionService {

    private final TicketDetailDomainService ticketDetailDomainService;
    private final TicketOrderRepository ticketOrderRepository;
    private final HoldRepository holdRepository;

    /** Bật true trong application.yml để cố tình gây lỗi SAU khi trừ kho -> xem rollback. */
    @Value("${app.demo.fail-after-deduct:false}")
    private boolean failAfterDeduct;

    /**
     * @return đơn vừa tạo, hoặc null nếu MySQL báo không đủ vé
     * @throws RuntimeException nếu có lỗi — transaction đã ROLLBACK
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder deductStockAndCreateOrder(Long ticketId, Long userId,
                                                 int quantity, BigDecimal unitPrice) {
        // ① Trừ kho MySQL (Cách 1). Chưa thật sự ghi — đang "treo" trong transaction.
        boolean deducted = ticketDetailDomainService.decreaseStock(ticketId, quantity);
        if (!deducted) {
            return null;    // chưa ghi gì -> không có gì để rollback
        }

        if (failAfterDeduct) {
            log.warn("[TX] DEMO: co tinh nem loi SAU khi tru kho, ticketId={}", ticketId);
            throw new IllegalStateException("DEMO rollback: loi gia lap sau khi tru kho");
        }

        // ② Tạo đơn — cùng transaction với ①
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
        log.info("[TX] tru kho + tao don OK | orderNumber={}", saved.getOrderNumber());
        return saved;
        // return bình thường -> Spring COMMIT cả ① và ②
    }

    /**
     * BÀI 18 — BƯỚC CUỐI: đổi lượt giữ chỗ thành đơn hàng.
     *
     * KHÔNG TRỪ KHO ở đây. Kho đã bị trừ từ lúc POST /holds rồi. Bước này chỉ
     * chuyển quyền sở hữu mấy chỗ đó từ "đang giữ tạm" sang "đã bán". Trừ
     * thêm lần nữa là bán 1 vé mà mất 2 chỗ — chỗ dễ sai nhất của mô hình này.
     *
     * markUsed() có WHERE status = 0 AND expireAt > now. Nó là cuộc đua giữa
     * khách bấm xác nhận ở giây 599 và job quét ở giây 600:
     *   - khách thắng -> job thấy 0 dòng, không hoàn kho, khách giữ vé
     *   - job thắng   -> ở đây nhận 0 dòng, KHÔNG tạo đơn, tầng trên hoàn tiền
     *
     * Thiếu mệnh đề đó thì tệ nhất: khách trả tiền xong, vé vẫn được trả về
     * kho bán cho người thứ hai. Một chỗ, hai người cầm vé.
     *
     * @return đơn vừa tạo, hoặc null nếu lượt giữ chỗ đã hết hạn / đã dùng
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder convertHoldToOrder(Hold hold, BigDecimal unitPrice) {
        LocalDateTime now = LocalDateTime.now();

        int changed = holdRepository.markUsed(hold.getId(), now);
        if (changed == 0) {
            log.info("[TX] hold khong con dung duoc | holdCode={}", hold.getHoldCode());
            return null;
        }

        TicketOrder order = new TicketOrder()
                .setOrderNumber(generateOrderNumber())
                .setUserId(hold.getUserId())
                .setTicketId(hold.getTicketId())
                .setQuantity(hold.getQuantity())
                .setUnitPrice(unitPrice)
                .setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(hold.getQuantity())))
                // Chua co cong thanh toan that, nen xac nhan la coi nhu da tra tien.
                // Bai 25 se chen buoc thanh toan vao giua: hold -> payment -> order.
                .setOrderStatus(TicketOrder.STATUS_PAID)
                .setCreatedAt(now)
                .setUpdatedAt(now);

        TicketOrder saved = ticketOrderRepository.save(order);
        log.info("[TX] hold -> don hang OK | holdCode={} orderNumber={}",
                hold.getHoldCode(), saved.getOrderNumber());
        return saved;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
