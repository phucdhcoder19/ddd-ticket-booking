package com.hoangphuc.ddd.application.model;

import com.hoangphuc.ddd.domain.model.enums.PassengerDiscount;

/**
 * Thong tin mot hanh khach, DA CHUAN HOA o controller: ten da gop khoang
 * trang, so dien thoai da ve dang 0xxxxxxxxx. Tang duoi khong phai lo dinh
 * dang nua, chi lo nghiep vu.
 */
public record PassengerCommand(
        String seatId,
        String fullName,
        String idNumber,
        String phone,
        PassengerDiscount discount) {
}
