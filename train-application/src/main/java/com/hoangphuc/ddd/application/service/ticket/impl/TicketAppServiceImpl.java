package com.hoangphuc.ddd.application.service.ticket.impl;

import com.hoangphuc.ddd.application.mapper.TicketMapper;
import com.hoangphuc.ddd.application.model.TicketDetailDTO;
import com.hoangphuc.ddd.application.service.ticket.TicketAppService;
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

        // TUYẾN PHÒNG THỦ 1 (MySQL) — hiện là tuyến duy nhất.
        // BƯỚC 6 sẽ thêm TUYẾN PHÒNG THỦ 2 (Redis + Lua) chặn TRƯỚC dòng này,
        // và khi đó mới cần bù trừ Redis nếu DB fail.
        boolean ok = ticketDetailDomainService.decreaseStock(ticketId, quantity);
        if (!ok) {
            log.info("[APP] buyTicket HET_VE | ticketId={}", ticketId);
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
