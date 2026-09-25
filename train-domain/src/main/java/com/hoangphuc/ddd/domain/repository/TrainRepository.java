package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;

import java.util.List;
import java.util.Optional;

public interface TrainRepository {

    /** Cac doan tau dang khai thac, sap theo gio khoi hanh. */
    List<Train> findActive();

    Optional<Train> findById(Long trainId);

    /** So do toa cua mot doan tau — khuon mau, khong theo ngay. */
    List<TrainCarriage> findCarriages(Long trainId);
}
