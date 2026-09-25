package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateHoldRequest {

    @NotNull(message = "tripId khong duoc trong")
    private Long tripId;

    @NotBlank(message = "seatClass khong duoc trong")
    private String seatClass;

    /**
     * Danh sach ma cho: ["C11-3", "C11-4"].
     *
     * Toi da 4 cho mot luot — gioi han nghiep vu chong dau co, va cung la
     * gioi han ky thuat: cang nhieu ghe trong mot lan thi xac suat dung do
     * voi nguoi khac cang cao, ma nghiep vu nay hong mot ghe la hong ca luot.
     */
    @NotEmpty(message = "phai chon it nhat 1 cho")
    @Size(max = 4, message = "moi luot giu toi da 4 cho")
    private List<String> seatIds;

    /**
     * Hanh trinh cua khach. Phai gui len vi GIA phu thuoc quang duong:
     * cung mot giuong tang 1 toa 11, Ha Noi - Vinh khac han Ha Noi - Sai Gon.
     */
    @NotBlank(message = "ga di khong duoc trong")
    private String from;

    @NotBlank(message = "ga den khong duoc trong")
    private String to;
}
