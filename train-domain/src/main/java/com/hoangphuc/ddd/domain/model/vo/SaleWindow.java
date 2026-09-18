package com.hoangphuc.ddd.domain.model.vo;

import java.time.LocalDateTime;

/**
 * Value object: đợt mở bán đang có hiệu lực.
 *
 * Không có id, không lưu vào bảng nào — nó được TÍNH RA từ các vé đang
 * ACTIVE. Hai SaleWindow cùng giá trị thì là một; đó là dấu hiệu của value
 * object, khác với entity (Station, TicketDetail) vốn phân biệt nhau bằng id.
 *
 * @param opensAt giờ mở bán của đợt đang xét
 * @param open    ngay lúc hỏi thì đã mở bán chưa
 */
public record SaleWindow(LocalDateTime opensAt, boolean open) {

    /** Không có vé nào được cấu hình lịch bán — coi như chưa mở. */
    public static SaleWindow closedAt(LocalDateTime now) {
        return new SaleWindow(now, false);
    }
}
