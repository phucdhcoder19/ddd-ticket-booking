package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.model.HoldDTO;
import com.hoangphuc.ddd.application.model.HoldResult;
import com.hoangphuc.ddd.application.model.PassengerCommand;
import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import com.hoangphuc.ddd.controller.dto.CreateHoldRequest;
import com.hoangphuc.ddd.controller.dto.SavePassengersRequest;
import com.hoangphuc.ddd.domain.model.enums.PassengerDiscount;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Lifecycle of a seat hold.
 *
 *   POST   /holds           pick seats -> claim them, start the countdown
 *   GET    /holds/{code}    polling    -> seconds left (by the SERVER clock)
 *   PUT    /holds/{code}/passengers   record who sits in each seat, fix discounts
 *   DELETE /holds/{code}    go back    -> return the seats right away
 */
@RestController
@RequestMapping("/holds")
@Slf4j
@RequiredArgsConstructor
public class HoldController {

    /**
     * No login yet. Every hold is recorded against one demo user.
     *
     * A constant here, userId is NOT read from the body: accepting any userId
     * from the client means anyone can book on behalf of anyone else and,
     * worse, see other people's tickets once "my tickets" exists. When login
     * is plugged in, this is exactly the one line to change.
     */
    private static final Long DEMO_USER_ID = 1L;

    public static final String QUEUE_TOKEN_HEADER = "X-Queue-Token";

    private final HoldAppService holdAppService;

    /**
     * The admission token travels in a HEADER, not in the body: it is not data
     * of the hold but the caller's "pass" — the same kind of thing as
     * Authorization. In a header, the frontend attaches it once for every
     * request instead of adding it to each body.
     */
    @PostMapping
    public ResultMessage<HoldDTO> create(@Valid @RequestBody CreateHoldRequest request,
                                         @RequestHeader(value = QUEUE_TOKEN_HEADER, required = false) String queueToken) {
        HoldCommand command = new HoldCommand(
                request.getTripId(),
                request.getSeatClass(),
                distinct(request.getSeatIds()),
                request.getFrom(),
                request.getTo(),
                DEMO_USER_ID,
                queueToken);

        return toResponse(holdAppService.createHold(command));
    }

    @GetMapping("/{holdCode}")
    public ResultMessage<HoldDTO> get(@PathVariable("holdCode") String holdCode) {
        return toResponse(holdAppService.getHold(holdCode));
    }

    /**
     * PUT, not POST: the body is the WHOLE passenger list, so sending it any
     * number of times ends in the same state. The frontend retries on slow
     * networks, and customers pressing "Back" to fix a name and resending is normal.
     */
    @PutMapping("/{holdCode}/passengers")
    public ResultMessage<HoldDTO> savePassengers(@PathVariable("holdCode") String holdCode,
                                                 @Valid @RequestBody SavePassengersRequest request) {
        List<PassengerCommand> passengers = request.getPassengers().stream()
                .map(this::toCommand)
                .toList();
        return toResponse(holdAppService.savePassengers(holdCode, passengers));
    }

    @DeleteMapping("/{holdCode}")
    public ResultMessage<HoldDTO> release(@PathVariable("holdCode") String holdCode) {
        return toResponse(holdAppService.releaseHold(holdCode));
    }

    /**
     * Drop duplicate seat codes before going down a layer.
     *
     * The claim UPDATE counts the ROWS it changed, so asking for
     * ["C3-1","C3-1"] changes 1 row while the layer above expects 2 — and the
     * customer is told "seat already taken" about the very seat they just got.
     * Filter at the boundary instead of making lower layers guard against dirty input.
     */
    private List<String> distinct(List<String> seatIds) {
        return seatIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(seatIds));
    }

    /**
     * Normalise at the boundary so the DB holds ONE form of each value: if the
     * table mixes "0912 345 678" and "+84912345678", a lookup with
     * WHERE phone = ? never finds them all.
     */
    private PassengerCommand toCommand(SavePassengersRequest.PassengerRequest p) {
        String phone = p.getPhone().replaceAll("[\\s.]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        PassengerDiscount discount = p.getDiscount() == null
                ? PassengerDiscount.NONE
                : PassengerDiscount.valueOf(p.getDiscount());
        return new PassengerCommand(
                p.getSeatId().trim(),
                p.getFullName().trim().replaceAll("\\s+", " "),
                p.getIdNumber().trim(),
                phone,
                discount);
    }

    /**
     * Only the controller knows about HTTP codes. Lower layers return an enum,
     * because if this flow ran over Kafka instead of HTTP tomorrow, the enum
     * would still make sense while the number 409 would not.
     *
     * 410 Gone for an expired hold, not 404: 404 means "never existed", 410
     * means "existed, now gone" — the client can tell them apart and show the
     * right "Your hold has expired, please pick again" message.
     */
    private ResultMessage<HoldDTO> toResponse(HoldResult result) {
        return switch (result.getStatus()) {
            case SUCCESS        -> ResultUtil.data(result.getHold());
            // 403: we know who you are but you are NOT ALLOWED in yet — unlike 401 (not logged in)
            case QUEUE_REQUIRED -> ResultUtil.error(403, "Please queue in the waiting room before picking seats");
            case SEAT_TAKEN     -> ResultUtil.error(409, "Someone just took a seat you picked, please choose another");
            case NOT_ON_SALE    -> ResultUtil.error(409, "The sale has not opened yet");
            case SALE_ENDED     -> ResultUtil.error(409, "This train has already departed, tickets are no longer sold");
            case TRIP_NOT_FOUND -> ResultUtil.error(404, "Trip not found");
            case INVALID_ROUTE  -> ResultUtil.error(400, "Invalid departure or arrival station");
            case HOLD_NOT_FOUND -> ResultUtil.error(404, "Hold not found");
            case HOLD_EXPIRED   -> ResultUtil.error(410, "Your hold has expired, please pick your seats again");
            case PASSENGER_MISMATCH -> ResultUtil.error(400, "Each held seat needs exactly one passenger");
            case ERROR          -> ResultUtil.error(500, "System error, please try again");
        };
    }
}
