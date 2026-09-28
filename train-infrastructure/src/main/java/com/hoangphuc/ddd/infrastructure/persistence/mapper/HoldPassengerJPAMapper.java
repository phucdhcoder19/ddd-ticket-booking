package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HoldPassengerJPAMapper extends JpaRepository<HoldPassenger, Long> {

    List<HoldPassenger> findByHoldIdOrderById(Long holdId);

    /**
     * Xoá bằng một câu DELETE, không phải deleteAll(findBy...).
     *
     * Không chỉ để nhanh: câu JPQL chạy XUỐNG DB NGAY, còn remove() của
     * Hibernate thì đợi tới lúc flush — mà Hibernate flush INSERT trước
     * DELETE. Dòng mới cho ghế C3-12 sẽ đụng unique (holdId, seatCode) với
     * chính dòng cũ chưa kịp xoá.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM HoldPassenger p WHERE p.holdId = :holdId")
    int deleteByHold(@Param("holdId") Long holdId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE HoldPassenger p SET p.orderId = :orderId " +
           "WHERE p.holdId = :holdId AND p.orderId IS NULL")
    int attachToOrder(@Param("holdId") Long holdId, @Param("orderId") Long orderId);
}
