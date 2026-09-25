package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;

/** Mot luot xep hang vao mua ve. */
@Data
public class QueueTicketDTO {

    /** Ma luot, client giu trong sessionStorage de hoi lai trang thai. */
    private String token;

    /** Con bao nhieu nguoi dung truoc. 0 = toi luot. */
    private int position;

    private int total;

    private int estimatedWaitSeconds;

    /** WAITING | ADMITTED */
    private String status;

    /** Duoc goi roi thi co bao lau de vao mua. NULL khi con dang cho. */
    private LocalDateTime admissionExpiresAt;
}
