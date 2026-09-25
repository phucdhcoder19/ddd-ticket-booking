package com.hoangphuc.ddd.application.service.hold;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.model.HoldResult;

public interface HoldAppService {

    /** Giữ những chỗ khách vừa chọn, kèm hạn chót. */
    HoldResult createHold(HoldCommand command);

    /** Client polling để đồng bộ đồng hồ đếm ngược với giờ server. */
    HoldResult getHold(String holdCode);

    /** User bấm quay lại: trả chỗ ngay, không đợi hết giờ. */
    HoldResult releaseHold(String holdCode);

    /** Job chạy nền gọi. Trả về số lượt đã thu hồi được trong lượt quét này. */
    int releaseExpiredHolds();
}
