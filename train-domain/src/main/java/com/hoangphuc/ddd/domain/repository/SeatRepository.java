package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Seat;

import java.util.Collection;
import java.util.List;

public interface SeatRepository {

    /** Ghi ca lo ghe khi sinh chuyen moi. */
    List<Seat> saveAll(List<Seat> seats);

    long countByTrip(Long tripId);

    /** Dem ghe con trong theo tung hang cho — nuoi man hinh tim chuyen. */
    List<Object[]> countFreeByClass(Long tripId);

    /** So do ghe cua mot toa. */
    List<Seat> findByTripAndCarriage(Long tripId, int carriageNumber);

    List<Seat> findByTripAndClass(Long tripId, String seatClass);

    List<Seat> findByTripAndCodes(Long tripId, Collection<String> seatCodes);

    /** Nhung ghe mot luot giu cho dang chiem. */
    List<Seat> findByHold(Long holdId);

    /** Nhung ghe mot don hang da mua. */
    List<Seat> findByOrder(Long orderId);

    /**
     * GIANH GHE cho mot luot giu cho.
     *
     * Tra ve SO GHE giu duoc. Tang tren so voi so ghe da xin: thieu mot ghe
     * cung la that bai, vi khach chon 3 cho ngoi canh nhau chu khong phai
     * "3 cho bat ky".
     */
    int claimForHold(Long tripId, Collection<String> seatCodes, Long holdId);

    /** Tra ghe ve kho khi huy hoac het gio. */
    int releaseByHold(Long holdId);

    /** Doi ghe tu "dang giu" sang "da ban" va gan vao don. */
    int sellByHold(Long holdId, Long orderId);
}
