package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatJPAMapper extends JpaRepository<Seat, Long> {

    long countByTripId(Long tripId);

    List<Seat> findByTripIdAndCarriageNumberOrderByRowNoAscColNoAsc(Long tripId, int carriageNumber);

    List<Seat> findByTripIdAndSeatClassOrderByCarriageNumberAscRowNoAscColNoAsc(Long tripId, String seatClass);

    List<Seat> findByTripIdAndSeatCodeIn(Long tripId, Collection<String> seatCodes);

    List<Seat> findByHoldIdOrderByCarriageNumberAscRowNoAscColNoAsc(Long holdId);

    List<Seat> findByOrderIdOrderByCarriageNumberAscRowNoAscColNoAsc(Long orderId);

    /**
     * Dem ghe con trong, gom theo hang cho.
     *
     * Mot cau GROUP BY thay cho N cau dem rieng tung hang. Man hinh tim chuyen
     * hien 7 tau x 3 hang cho = 21 con so; neu dem tung cai la 21 luot xuong DB.
     *
     * Tra ve Object[]{ seatClass, soGheTrong } — day la ranh gioi infrastructure,
     * tang tren se doi sang kieu co nghia.
     */
    @Query("SELECT s.seatClass, COUNT(s) FROM Seat s " +
           "WHERE s.tripId = :tripId AND s.status = 0 GROUP BY s.seatClass")
    List<Object[]> countFreeByClass(@Param("tripId") Long tripId);

    /**
     * GIÀNH GHẾ — đây là toàn bộ phần chống tranh chấp của màn chọn chỗ.
     *
     * Mệnh đề "AND s.status = 0" chính là cái khoá, đúng như markReleased()
     * của Hold: InnoDB khoá dòng khi UPDATE, hai người cùng bấm vào ghế C3-12
     * phải xếp hàng, người đến sau đọc giá trị MỚI NHẤT, thấy status đã là 1,
     * điều kiện sai, sửa 0 dòng.
     *
     * Không cần Redisson ở đây. Distributed lock chỉ cần khi thứ phải bảo vệ
     * nằm NGOÀI database (như lúc sinh chuyến, ghi 600 dòng); còn khi tranh
     * nhau đúng một dòng thì chính database đã là trọng tài rồi.
     *
     * Số dòng trả về là số ghế giành được. Tầng trên so với số ghế đã xin —
     * thiếu một ghế cũng phải ROLLBACK cả lượt, vì khách chọn 3 chỗ liền nhau
     * chứ không phải "3 chỗ bất kỳ còn trống".
     *
     * clearAutomatically: sau câu UPDATE này tầng trên đọc lại chính những
     * dòng vừa sửa. Không xoá persistence context thì Hibernate trả về bản
     * còn nằm trong bộ nhớ phiên — status cũ, holdId rỗng.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 1, s.holdId = :holdId " +
           "WHERE s.tripId = :tripId AND s.seatCode IN :seatCodes AND s.status = 0")
    int claimForHold(@Param("tripId") Long tripId,
                     @Param("seatCodes") Collection<String> seatCodes,
                     @Param("holdId") Long holdId);

    /**
     * TRẢ GHẾ. "AND s.status = 1" để không bao giờ kéo ngược một ghế ĐÃ BÁN
     * về trạng thái trống: job thu hồi chạy sau khi khách vừa thanh toán
     * xong sẽ tìm thấy 0 dòng và bỏ đi, thay vì bán lại chỗ đã có chủ.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 0, s.holdId = null " +
           "WHERE s.holdId = :holdId AND s.status = 1")
    int releaseByHold(@Param("holdId") Long holdId);

    /** Ghế chuyển sang đã bán. Giữ nguyên holdId để tra ngược lại lượt giữ. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 2, s.orderId = :orderId " +
           "WHERE s.holdId = :holdId AND s.status = 1")
    int sellByHold(@Param("holdId") Long holdId, @Param("orderId") Long orderId);
}
