package com.hoangphuc.ddd.application.service.trip;

import com.hoangphuc.ddd.application.model.CarriageDTO;

import java.util.List;
import java.util.Optional;

public interface SeatMapAppService {

    /**
     * So do cho cua mot chuyen, chi lay cac toa thuoc hang cho khach da chon.
     *
     * Phai co ca ga di va ga den: gia tung cho phu thuoc quang duong khach
     * di, ma chuyen tau thi khong biet khach xuong ga nao.
     *
     * @return rong neu khong co chuyen do, hoac ma ga khong hop le
     */
    Optional<List<CarriageDTO>> carriages(Long tripId, String seatClass, String fromCode, String toCode);
}
