package com.hoangphuc.ddd.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Đợt mở bán hiện tại — nuôi cái đồng hồ đếm ngược ở trang chủ.
 */
@Data
public class SaleWindowDTO {

    /** Thời điểm mở bán. Nếu đã mở rồi thì đây là mốc đã qua. */
    private LocalDateTime opensAt;

    private String label;

    /**
     * BẪY LOMBOK + JACKSON: field "isOpen" kiểu boolean sinh getter isOpen(),
     * Jackson cắt tiền tố "is" và đặt tên JSON thành "open" — frontend đọc
     * res.isOpen sẽ ra undefined mà không có lỗi nào báo.
     * @JsonProperty ép giữ đúng tên.
     */
    @JsonProperty("isOpen")
    private boolean isOpen;

    /**
     * Giờ của SERVER. Đồng hồ đếm ngược phải trừ theo mốc này, không theo
     * giờ máy khách — máy khách chỉnh được giờ, và có thể ngủ rồi thức dậy.
     */
    private LocalDateTime serverNow;
}
