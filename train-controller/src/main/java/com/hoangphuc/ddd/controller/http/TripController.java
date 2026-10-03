package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.CarriageDTO;
import com.hoangphuc.ddd.application.model.TripDTO;
import com.hoangphuc.ddd.application.service.trip.SeatMapAppService;
import com.hoangphuc.ddd.application.service.trip.TripAppService;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/trips")
@Slf4j
@RequiredArgsConstructor
public class TripController {

    private final TripAppService tripAppService;
    private final SeatMapAppService seatMapAppService;

    /**
     * GET /api/trips?from=HNO&to=SGO&date=2027-01-27
     *
     * The frontend's "passengers" is not used here: the number of passengers
     * only matters when HOLDING, it does not change the trip list. Accepting
     * and ignoring it would make lower layers carry a meaningless parameter —
     * so it is left out of the signature.
     */
    @GetMapping
    public ResultMessage<List<TripDTO>> search(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam("date") LocalDate date) {

        List<TripDTO> trips = tripAppService.search(from, to, date);
        log.info("[CONTROLLER] search trips | {} -> {} on {} | found {}", from, to, date, trips.size());
        return ResultUtil.data(trips);
    }

    /**
     * GET /api/trips/12/carriages?seatClass=BERTH_4&from=HNO&to=SGO
     *
     * Why from/to must be sent again here, right after the search step: the
     * price of EACH SEAT depends on the passenger's distance, and a trip does
     * not know where the passenger gets off. The only way to drop these two
     * parameters would be storing the journey in a server-side session —
     * trading a URL parameter for stateful sessions is not worth it.
     *
     * Only carriages of the chosen seat class are returned. Returning all 15
     * and letting the frontend filter would make the browser download ~1500
     * seats to draw 300.
     */
    @GetMapping("/{tripId}/carriages")
    public ResultMessage<List<CarriageDTO>> carriages(
            @PathVariable("tripId") Long tripId,
            @RequestParam("seatClass") String seatClass,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {

        Optional<List<CarriageDTO>> carriages =
                seatMapAppService.carriages(tripId, seatClass, from, to);

        if (carriages.isEmpty()) {
            return ResultUtil.error(404, "Trip not found");
        }
        log.info("[CONTROLLER] seat map | trip={} class={} | {} carriages",
                tripId, seatClass, carriages.get().size());
        return ResultUtil.data(carriages.get());
    }
}
