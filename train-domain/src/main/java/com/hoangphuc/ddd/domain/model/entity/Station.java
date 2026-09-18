package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Ga tàu trên trục Bắc – Nam.
 *
 * Bảng tra cứu thuần: gần như không bao giờ đổi, không có nghiệp vụ nào
 * ngoài "liệt kê cho người dùng chọn". Đúng loại dữ liệu nên cache mạnh tay.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "station", indexes = @Index(name = "idx_station_order", columnList = "displayOrder"))
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã ga viết tắt dùng trong URL và mã vé: HNO, SGO... */
    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    /** "Bắc" | "Trung" | "Nam" — gom nhóm cho dễ tìm trong danh sách dài */
    @Column(nullable = false, length = 8)
    private String region;

    /** Cây số tính từ ga Hà Nội. Dùng để ước lượng quãng đường và giá vé. */
    private int kmFromHanoi;

    /** Thứ tự ga trên trục Bắc – Nam. Danh sách LUÔN sắp theo cột này,
     *  không sắp theo tên — người đi tàu nghĩ theo thứ tự tuyến. */
    private int displayOrder;
}
