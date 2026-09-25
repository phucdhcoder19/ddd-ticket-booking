package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Đoàn tàu — KHUÔN MẪU, không gắn với ngày nào.
 *
 * SE1 chạy 20:25 mỗi ngày. Bản thân "SE1" không phải một chuyến đi cụ thể;
 * nó là lịch. Chuyến đi cụ thể là Trip = Train + ngày.
 *
 * Tách khuôn mẫu khỏi lần chạy là cách tránh nhân bản dữ liệu: đổi giờ chạy
 * của SE1 thì sửa một dòng, không phải sửa 60 dòng của 60 ngày.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "train")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SE1, SE3, TN3... */
    @Column(nullable = false, unique = true, length = 16)
    private String code;

    private int departHour;
    private int departMinute;

    /** Tốc độ trung bình km/h — dùng để ước lượng giờ tới. */
    private int speedKmh;

    /** 0 = ngừng khai thác, 1 = đang chạy */
    private int status;
}
