package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * PUT /holds/{holdCode}/passengers
 *
 * Luat kiem tra o day CHEP DUNG luat cua frontend (lib/validate.ts): cai gi
 * frontend cho qua thi server cung phai cho qua, neu khong khach dien dung
 * het ma van bi tu choi. Frontend kiem de bao loi dep, server kiem vi khong
 * bao gio duoc tin client.
 */
@Data
public class SavePassengersRequest {

    @NotEmpty(message = "phai co it nhat 1 hanh khach")
    @Size(max = 4, message = "moi luot giu toi da 4 cho")
    @Valid
    private List<PassengerRequest> passengers;

    @Data
    public static class PassengerRequest {

        @NotBlank(message = "seatId khong duoc trong")
        private String seatId;

        /** It nhat hai tu (ho + ten), chi chu cai, dau cach va dau nhay don. */
        @NotBlank(message = "ho ten khong duoc trong")
        @Size(max = 100, message = "ho ten toi da 100 ky tu")
        @Pattern(regexp = "^\\s*[\\p{L}']+(\\s+[\\p{L}']+)+\\s*$",
                 message = "ho ten gom ca ho va ten, chi chu cai")
        private String fullName;

        /** CCCD 12 so, hoac CMND cu 9 so. */
        @NotBlank(message = "so CCCD khong duoc trong")
        @Pattern(regexp = "^\\s*(\\d{12}|\\d{9})\\s*$",
                 message = "so CCCD gom 12 chu so (hoac 9 voi CMND cu)")
        private String idNumber;

        /** 0912345678, 0912 345 678, 0912.345.678, +84912345678 deu hop le. */
        @NotBlank(message = "so dien thoai khong duoc trong")
        @Pattern(regexp = "^\\s*(\\+84|0)([\\s.]*\\d){9}[\\s.]*$",
                 message = "so dien thoai 10 chu so, bat dau bang 0")
        private String phone;

        @Pattern(regexp = "NONE|STUDENT|CHILD|SENIOR",
                 message = "discount chi nhan NONE, STUDENT, CHILD, SENIOR")
        private String discount;
    }
}
