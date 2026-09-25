package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.util.List;

/** Mot toa cua mot chuyen, kem so do cho. */
@Data
public class CarriageDTO {

    private String id;

    /** Toa so may — khach doc con so nay tren ve. */
    private int number;

    /** SOFT_SEAT | BERTH_4 | BERTH_6 */
    private String seatClass;

    /** seat-2-2 | berth-4 | berth-6 — frontend dung de chon cach ve. */
    private String layout;

    /** Toa ngoi: so hang ghe. Toa nam: so khoang. */
    private int rows;

    private List<SeatDTO> seats;

    /** Con bao nhieu cho trong — de tab chon toa hien ngay, khoi dem lai. */
    private int available;
}
