package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Hold;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HoldRepository {

    Hold save(Hold hold);

    Optional<Hold> findByCode(String holdCode);

    /**
     * Giành quyền đóng lượt giữ chỗ.
     *
     * Trả về số dòng bị ảnh hưởng, KHÔNG phải boolean "thành công":
     *   1 = mình là người đổi được trạng thái -> mình có quyền hoàn kho
     *   0 = người khác đã xử lý trước -> rút lui, không đụng vào kho
     *
     * Đây là cách chặn cả hai bẫy: job chạy trùng trên 2 server, và job
     * cướp lượt giữ chỗ của người đang thanh toán.
     */
    int markReleased(Long holdId, LocalDateTime now);

    int markUsed(Long holdId, LocalDateTime now);

    /** Các lượt giữ chỗ đã quá hạn mà chưa ai dọn. */
    List<Hold> findExpired(LocalDateTime now, int limit);
}
