package com.hoangphuc.ddd.application.cronjob;

import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thu hồi vé bị giữ khống.
 *
 * Đây là thứ bài 18 sinh ra để giải: khách đóng tab bỏ đi thì KHÔNG AI BÁO
 * GÌ CHO SERVER CẢ. Không có exception, không có request huỷ. Chỉ có cách
 * ngồi đếm giờ mà phát hiện.
 *
 * fixedDelay (không phải fixedRate): chờ N giây SAU KHI lượt trước kết thúc.
 * fixedRate bắn theo nhịp bất kể lượt trước xong chưa -> gặp 5000 hold hết hạn
 * là hai lượt chồng nhau.
 *
 * LƯU Ý khi chạy nhiều instance: job này chạy trên MỌI server, cả hai cùng
 * quét một bảng. Cố ý không thêm distributed lock — mệnh đề
 * "WHERE status = 0" trong markReleased() đã đủ để một bên thua.
 * Xem HoldTransactionService.releaseOne().
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HoldReleaseJob {

    private final HoldAppService holdAppService;

    @Scheduled(fixedDelayString = "${app.hold.scan-interval-ms:10000}")
    public void releaseExpiredHolds() {
        int released = holdAppService.releaseExpiredHolds();
        if (released > 0) {
            log.info("[HOLD-JOB] da thu hoi {} luot giu cho het han", released);
        }
    }
}
