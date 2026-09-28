package com.hoangphuc.ddd.application.service.ticket;

/**
 * Luot giu cho chua co du ten hanh khach cho tung ghe.
 *
 * Nem tu TRONG transaction doi hold -> don, SAU khi markUsed() da chay: nem
 * ra thi Spring rollback, hold quay ve status 0 va khach van con thoi gian
 * quay lai man nhap thong tin. Tra ve null thi hold da bi danh dau "da dung"
 * ma khong co don nao — khach mat cho, khong mua lai duoc.
 */
public class PassengersMissingException extends RuntimeException {

    public PassengersMissingException(String message) {
        super(message);
    }
}
