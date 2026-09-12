package com.hoangphuc.ddd.application.service.ticket.impl;

import com.hoangphuc.ddd.application.mapper.TicketMapper;
import com.hoangphuc.ddd.application.model.TicketDetailDTO;
import com.hoangphuc.ddd.application.service.ticket.TicketAppService;
import com.hoangphuc.ddd.application.service.ticket.cache.StockCacheService;
import com.hoangphuc.ddd.application.service.ticket.cache.TicketDetailCacheService;
import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketAppServiceImpl implements TicketAppService {

    private final TicketDetailDomainService ticketDetailDomainService;
    private final TicketDetailCacheService ticketDetailCacheService;
    private final StockCacheService stockCacheService;

    @Override
    public TicketDetailDTO getTicketDetail(Long ticketId) {
        log.info("[APP] getTicketDetail | ticketId={}", ticketId);

        // ĐIỀU PHỐI: đi qua cache, KHÔNG gọi thẳng domain service nữa.
        // Cache tự lo phần xuống MySQL khi miss.
        TicketDetail entity = ticketDetailCacheService.getTicketDetail(ticketId);

        return TicketMapper.toDTO(entity);
    }

    @Override
    public String buyTicket(Long ticketId, int quantity) {
        log.info("[APP] buyTicket | ticketId={} qty={}", ticketId, quantity);

        // ===== TUYẾN PHÒNG THỦ 2 (Redis + Lua) — chặn sớm, không đụng DB =====
        int redisResult = stockCacheService.deduct(ticketId, quantity);

        if (redisResult == -1) {
            // Redis chưa có key (app vừa restart / key bị xoá) -> nạp từ DB rồi thử lại
            log.info("[APP] buyTicket: Redis chua co stock, warm-up | ticketId={}", ticketId);
            if (!stockCacheService.warmUp(ticketId)) {
                return "KHONG_TIM_THAY_VE";
            }
            redisResult = stockCacheService.deduct(ticketId, quantity);
        }

        if (redisResult == 0) {
            // Hết vé — chặn ngay tại Redis, MySQL KHÔNG hề bị đụng tới
            log.info("[APP] buyTicket HET_VE (chan o Redis) | ticketId={}", ticketId);
            return "HET_VE";
        }

        // ===== TUYẾN PHÒNG THỦ 1 (MySQL) — lưới an toàn cuối cùng =====
        boolean ok = ticketDetailDomainService.decreaseStock(ticketId, quantity);
        if (!ok) {
            // Redis đã trừ nhưng DB từ chối -> phải HOÀN LẠI Redis,
            // nếu không số vé trong Redis sẽ hụt dần và bán thiếu.
            stockCacheService.restore(ticketId, quantity);
            log.warn("[APP] buyTicket: DB tu choi, da hoan Redis | ticketId={}", ticketId);
            return "HET_VE";
        }

        // Kho vừa đổi -> cache đang giữ số cũ -> phải xoá, nếu không
        // người dùng tiếp theo sẽ thấy số vé sai suốt 10 phút.
        ticketDetailCacheService.evict(ticketId);

        // ĐIỀU PHỐI — các bước sau sẽ mọc thêm ở đây:
        //   insert đơn hàng, ghi outbox, gửi Kafka...
        log.info("[APP] buyTicket OK | ticketId={} qty={}", ticketId, quantity);
        return "OK";
    }
}
