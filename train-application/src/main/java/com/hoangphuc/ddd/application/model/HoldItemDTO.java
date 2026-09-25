package com.hoangphuc.ddd.application.model;

import lombok.Data;

/**
 * Mot cho trong luot giu, kem gia.
 *
 * Gia duoc tinh lai tu HANH TRINH DA LUU cua luot giu (cap ga + ngay chay),
 * khong tinh lai tu tham so client gui len. Nho vay khach doi tab, F5, hay
 * polling dong ho dem nguoc bao nhieu lan thi van thay dung con so luc chon.
 */
@Data
public class HoldItemDTO {

    /** Ma cho "C3-12". */
    private String seatId;

    /** So hieu cho "12". */
    private String seatLabel;

    private int carriageNumber;

    private String seatClass;

    private long price;
}
