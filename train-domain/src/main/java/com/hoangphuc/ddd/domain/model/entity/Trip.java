package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Một lượt chạy cụ thể: đoàn tàu nào, ngày nào.
 *
 * SINH LƯỜI (lazy): chuyến chỉ được tạo khi có người đầu tiên tìm tới ngày đó.
 *
 * Vì sao không sinh sẵn 60 ngày: mỗi chuyến kéo theo ~600 ghế.
 *   7 tàu × 60 ngày × 600 ghế = 252.000 dòng
 * Trong khi đa số ngày chẳng ai tìm. Sinh lười thì chỉ trả tiền cho ngày
 * thật sự có khách.
 *
 * Đổi lại: người đầu tiên tìm phải chờ lâu hơn, và 5000 người cùng tìm một
 * lúc thì phải chặn không cho 5000 lượt cùng sinh — xem TripProvisionService.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "trip",
    uniqueConstraints = @UniqueConstraint(name = "uk_trip_train_date", columnNames = {"trainId", "serviceDate"}),
    indexes = @Index(name = "idx_trip_date", columnList = "serviceDate")
)
public class Trip {

    public static final int STATUS_READY = 1;   // đã sinh đủ ghế, bán được

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trainId;

    /** Ngày tàu chạy (giờ khởi hành lấy từ Train). */
    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private int status;

    private LocalDateTime createdAt;
}
