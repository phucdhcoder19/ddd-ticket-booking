package com.hoangphuc.ddd.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Mot hang cho tren mot chuyen: gia re nhat, con bao nhieu, tong bao nhieu. */
@Data
@AllArgsConstructor
public class SeatClassOfferDTO {
    /** SOFT_SEAT | BERTH_4 | BERTH_6 */
    private String code;
    /** Gia THAP NHAT cua hang nay — man hinh tim chuyen hien "tu ... d". */
    private long price;
    private int available;
    private int total;
}
