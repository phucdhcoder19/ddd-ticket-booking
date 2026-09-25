package com.hoangphuc.ddd.application.service.queue.impl;

import com.hoangphuc.ddd.application.model.QueueTicketDTO;
import com.hoangphuc.ddd.application.service.queue.QueueAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PHÒNG CHỜ — hiện là CỬA MỞ, ai tới cũng cho qua ngay.
 *
 * Vì sao vẫn có endpoint dù chưa xếp hàng thật: phòng chờ là một cái CỬA,
 * và cửa phải nằm sẵn trên đường đi từ trước. Ngày cần bật hàng đợi thật
 * (giờ mở bán vé Tết, mấy vạn người ùa vào cùng lúc) thì chỉ đổi phần ruột
 * của hai hàm dưới đây — frontend, đường dẫn, luồng bấm đều không phải sửa.
 *
 * Làm ngược lại, tức là để tới lúc quá tải mới chèn phòng chờ vào giữa, thì
 * phải sửa cả luồng điều hướng đúng lúc hệ thống đang cháy.
 *
 * Phần còn thiếu khi làm thật: một sorted set trên Redis giữ thứ tự tới
 * trước, một bộ nhả theo tốc độ cho phép, và quan trọng nhất — POST /holds
 * phải TỪ CHỐI request không mang token đã được gọi. Thiếu vế cuối thì
 * phòng chờ chỉ là một màn hình trang trí, ai gọi thẳng API vẫn mua được.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QueueAppServiceImpl implements QueueAppService {

    /** Được gọi rồi thì có bao lâu để vào mua trước khi mất lượt. */
    @Value("${app.queue.admission-window:15m}")
    private Duration admissionWindow;

    @Override
    public QueueTicketDTO join() {
        String token = "Q-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("[QUEUE] cho qua ngay | token={}", token);
        return admitted(token);
    }

    @Override
    public QueueTicketDTO status(String token) {
        // Cửa đang mở nên không cần tra token có thật hay không: câu trả lời
        // cho mọi token đều là "mời vào". Khi có hàng đợi thật thì đây là chỗ
        // tra Redis, và token lạ phải trả 404.
        return admitted(token);
    }

    private QueueTicketDTO admitted(String token) {
        QueueTicketDTO dto = new QueueTicketDTO();
        dto.setToken(token);
        dto.setPosition(0);
        dto.setTotal(0);
        dto.setEstimatedWaitSeconds(0);
        dto.setStatus("ADMITTED");
        dto.setAdmissionExpiresAt(LocalDateTime.now().plus(admissionWindow));
        return dto;
    }
}
