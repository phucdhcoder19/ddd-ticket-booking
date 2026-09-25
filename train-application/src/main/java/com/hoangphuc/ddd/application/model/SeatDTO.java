package com.hoangphuc.ddd.application.model;

import lombok.Data;

/** Mot cho tren so do toa. Ten truong khop dung kieu Seat cua frontend. */
@Data
public class SeatDTO {

    /** Ma cho, vi du "C3-12". Frontend dung chinh chuoi nay lam id khi giu cho. */
    private String id;

    /** So hieu in tren ve: "12". */
    private String label;

    private int row;
    private int col;

    /**
     * Toa nam: so khoang. Toa ngoi: NULL.
     *
     * Gui null chu khong gui 0: frontend khai bao compartment?, nghia la
     * "toa nay khong co khai niem khoang". So 0 la mot khoang ten la 0.
     */
    private Integer compartment;

    /** Tang giuong 1..3. Toa ngoi: NULL. */
    private Integer berthLevel;

    /** available | held | sold */
    private String status;

    private long price;
}
