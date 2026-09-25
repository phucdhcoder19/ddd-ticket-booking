package com.hoangphuc.ddd.application.service.hold;

/**
 * It nhat mot trong nhung cho khach chon da bi nguoi khac giu mat.
 *
 * La RuntimeException chu khong phai gia tri tra ve, vi no duoc nem TU TRONG
 * transaction: chi co nem ra Spring moi ROLLBACK. Tra ve null thi transaction
 * commit binh thuong, de lai mot luot giu cho giu duoc 2/3 ghe — dung cai
 * ma nghiep vu nay khong cho phep.
 */
public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
