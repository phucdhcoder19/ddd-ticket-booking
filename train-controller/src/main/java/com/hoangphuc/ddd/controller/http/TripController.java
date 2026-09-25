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
     * "passengers" cua frontend khong dung o day: so khach chi anh huong
     * luc GIU CHO, khong doi danh sach chuyen. Nhan vao roi lo di thi tang
     * duoi phai biet mot tham so vo nghia — thua ra khoi chu ky ham luon.
     */
    @GetMapping
    public ResultMessage<List<TripDTO>> search(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam("date") LocalDate date) {

        List<TripDTO> trips = tripAppService.search(from, to, date);
        log.info("[CONTROLLER] search trips | {} -> {} ngay {} | tim thay {}", from, to, date, trips.size());
        return ResultUtil.data(trips);
    }

    /**
     * GET /api/trips/12/carriages?seatClass=BERTH_4&from=HNO&to=SGO
     *
     * Vi sao van phai gui lai from/to o day, du vua gui o buoc tim chuyen:
     * gia TUNG CHO phu thuoc quang duong khach di, ma mot chuyen tau thi
     * khong biet khach xuong ga nao. Cach duy nhat de bo hai tham so nay la
     * luu hanh trinh vao phien tren server — doi mot tham so URL lay mot
     * phien co trang thai, khong dang.
     *
     * Chi tra ve cac toa thuoc dung hang cho khach chon. Tra ca 15 toa roi
     * de frontend loc la bat trinh duyet tai ~1500 ghe de ve 300.
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
            return ResultUtil.error(404, "Khong tim thay chuyen tau");
        }
        log.info("[CONTROLLER] so do cho | chuyen={} hang={} | {} toa",
                tripId, seatClass, carriages.get().size());
        return ResultUtil.data(carriages.get());
    }
}
