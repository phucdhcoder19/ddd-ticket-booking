package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.Hold;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HoldJPAMapper extends JpaRepository<Hold, Long> {

    Optional<Hold> findByHoldCode(String holdCode);

    /**
     * ĐÓNG LƯỢT GIỮ CHỖ — hai câu dưới đây là toàn bộ bài 18.
     *
     * Mệnh đề "AND h.status = 0" chính là cái khoá. InnoDB khoá dòng khi
     * UPDATE, nên hai bên cùng đụng vào một dòng phải xếp hàng; người đến sau
     * đọc giá trị MỚI NHẤT, thấy status đã khác 0, điều kiện sai, sửa 0 dòng.
     *
     * Không cần distributed lock, không cần Redisson. Một mệnh đề WHERE làm hết.
     *
     * MỐC THỜI GIAN do APP truyền xuống, không dùng CURRENT_TIMESTAMP.
     * CURRENT_TIMESTAMP là đồng hồ của MySQL — container chạy UTC trong khi
     * app chạy +07, nên cùng một dòng sẽ có expire_at và updated_at lệch nhau
     * 7 tiếng. Cả hệ thống phải xem giờ trên CÙNG MỘT đồng hồ.
     */
    @Modifying
    @Query("UPDATE Hold h SET h.status = 2, h.updatedAt = :now " +
           "WHERE h.id = :holdId AND h.status = 0")
    int markReleased(@Param("holdId") Long holdId, @Param("now") LocalDateTime now);

    @Modifying
    /**
     * Đổi lượt giữ chỗ thành đơn hàng.
     *
     * Có THÊM điều kiện "AND h.expireAt > :now" mà markReleased không cần.
     * Lý do: job quét mỗi 10 giây, nên có khoảng hở giữa lúc hold hết hạn và
     * lúc job dọn tới — trong khoảng đó status vẫn là 0. Chỉ kiểm status thôi
     * thì khách dùng được hold đã quá giờ.
     *
     * Kiểm thời gian phải nằm TRONG câu UPDATE, không phải if ở tầng trên:
     * đọc rồi mới ghi là còn khe hở, gộp vào WHERE thì MySQL khoá dòng và
     * kiểm trên giá trị mới nhất.
     */
    @Query("UPDATE Hold h SET h.status = 1, h.updatedAt = :now " +
           "WHERE h.id = :holdId AND h.status = 0 AND h.expireAt > :now")
    int markUsed(@Param("holdId") Long holdId, @Param("now") LocalDateTime now);

    /**
     * Quét theo lô, không lấy hết một lần. Giờ cao điểm có thể hàng nghìn
     * lượt hết hạn cùng lúc; ôm hết vào RAM rồi xử lý là một transaction dài,
     * khoá nhiều dòng, chặn cả người đang mua.
     */
    @Query("SELECT h FROM Hold h WHERE h.status = 0 AND h.expireAt < :now ORDER BY h.expireAt ASC")
    List<Hold> findExpired(@Param("now") LocalDateTime now, Pageable pageable);
}
