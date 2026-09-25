package com.hoangphuc.ddd.application.model;

import java.util.List;

/**
 * Yeu cau giu cho, da gom thanh mot kieu thay vi sau tham so roi.
 *
 * Sau tham so lien tiep trong do co ba chuoi thi doi cho hai cai bat ky la
 * code van bien dich, van chay, chi sai ga. Mot record thi goi sai ten
 * truong la bao loi ngay.
 */
public record HoldCommand(
        Long tripId,
        String seatClass,
        List<String> seatIds,
        String fromCode,
        String toCode,
        Long userId) {
}
