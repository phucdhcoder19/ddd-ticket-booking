package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;

import java.util.List;

public interface HoldPassengerRepository {

    /**
     * Thay TOAN BO danh sach hanh khach cua mot luot giu cho.
     *
     * Xoa het roi ghi lai, khong sua tung dong: PUT nghia la "day la trang
     * thai cuoi cung", goi hai lan cung ket qua. Frontend tu thu lai khi
     * mang cham, nen tinh chat do la bat buoc chu khong chi cho dep.
     */
    List<HoldPassenger> replaceForHold(Long holdId, List<HoldPassenger> passengers);

    List<HoldPassenger> findByHold(Long holdId);

    /** Gan hanh khach cua luot giu cho vao don hang vua tao. */
    int attachToOrder(Long holdId, Long orderId);
}
