package com.hoangphuc.ddd.application.service.hold;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.model.HoldResult;
import com.hoangphuc.ddd.application.model.PassengerCommand;

import java.util.List;

public interface HoldAppService {

    /** Hold the seats the customer just picked, with a deadline. */
    HoldResult createHold(HoldCommand command);

    /** Client polling to keep the countdown in sync with the server clock. */
    HoldResult getHold(String holdCode);

    /** Record who sits in each seat. Can be called repeatedly; each call replaces the previous one. */
    HoldResult savePassengers(String holdCode, List<PassengerCommand> passengers);

    /** The user pressed back: return the seats now, do not wait for expiry. */
    HoldResult releaseHold(String holdCode);

    /** Called by the background job. Returns how many holds were released in this scan. */
    int releaseExpiredHolds();
}
