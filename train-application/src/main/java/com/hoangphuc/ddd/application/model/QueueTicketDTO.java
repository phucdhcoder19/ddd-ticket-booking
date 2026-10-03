package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;

/** One place in the queue to get in and buy tickets. */
@Data
public class QueueTicketDTO {

    /** Queue token, kept by the client in sessionStorage to ask for its status again. */
    private String token;

    /** How many people are ahead. 0 = your turn. */
    private int position;

    private int total;

    private int estimatedWaitSeconds;

    /** WAITING | ADMITTED */
    private String status;

    /** Once admitted, how long there is to buy. NULL while still waiting. */
    private LocalDateTime admissionExpiresAt;
}
