package com.hoangphuc.ddd.application.service.queue;

import com.hoangphuc.ddd.application.model.QueueTicketDTO;

import java.util.Optional;

public interface QueueAppService {

    /** Ask for a turn to buy: take a number, stand at the back of the queue. */
    QueueTicketDTO join();

    /** Check whether it is your turn yet. Empty = unknown token, or the admission has expired. */
    Optional<QueueTicketDTO> status(String token);

    /** Called by the job: let more people in if there is room inside. Returns how many were admitted. */
    long admitNext();

    /** The gatekeeper: does this token hold a valid admission. */
    boolean isAdmitted(String token);

    /** Done buying: free the slot inside for the next person. */
    void leave(String token);
}
