package com.hoangphuc.ddd.application.service.hold;

import com.hoangphuc.ddd.application.model.HoldResult;

public interface HoldAppService {

    /** Giữ chỗ: trừ kho Redis + MySQL, ghi lượt giữ với hạn chót. */
    HoldResult createHold(Long ticketId, Long userId, int quantity);

    /** Client polling để đồng bộ đồng hồ đếm ngược với giờ server. */
    HoldResult getHold(String holdCode);

    /** User bấm quay lại: trả kho ngay, không đợi hết giờ. */
    HoldResult releaseHold(String holdCode);

    /** Job chạy nền gọi. Trả về số lượt đã thu hồi được trong lượt quét này. */
    int releaseExpiredHolds();
}
