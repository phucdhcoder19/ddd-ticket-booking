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
        if (quantity <= 0) return false;        // đặt validate ở đây, đừng để rơi xuống SQL
        return ticketDetailRepository.decreaseStock(ticketId, quantity);
    }

    @Override
    public boolean increaseStock(Long ticketId, int quantity) {
        return ticketDetailRepository.increaseStock(ticketId, quantity);
    }

    /**
     * LUẬT NGHIỆP VỤ: đợt mở bán nào đang có hiệu lực.
     *
     * Thứ tự ưu tiên — người dùng quan tâm "còn bao lâu nữa được mua":
     *   ① có vé chưa tới giờ mở  -> đếm ngược tới giờ đó, chưa mở
     *   ② không, nhưng có vé đã mở và còn trong khung bán  -> đang mở
     *   ③ không có gì (hết giờ, hoặc chưa cấu hình lịch)   -> coi như đóng
     *
     * Bước ② dùng lại đúng TicketDetail.isSaleEnded() mà placeOrder() dùng
     * để chặn — một luật, một chỗ định nghĩa.
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
