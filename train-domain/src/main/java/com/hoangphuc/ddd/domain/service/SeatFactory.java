package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;

import java.util.ArrayList;
import java.util.List;

/**
 * Dựng danh sách ghế của một toa cho một chuyến.
 *
 * Là class tiện ích tĩnh, KHÔNG phải interface + impl như các domain service
 * khác trong repo. Lý do: nó không phụ thuộc vào gì cả — không repository,
 * không cache, không thời gian. Cùng đầu vào luôn cho cùng đầu ra.
 * Thứ như vậy không có gì để thay thế, nên tạo interface chỉ là thêm một
 * file rỗng phải mở ra mỗi lần đọc code.
 */
public final class SeatFactory {

    private SeatFactory() {
    }

    /** Toa ngồi: 4 ghế mỗi hàng, 2 bên lối đi. */
    private static final int SEATS_PER_ROW = 4;

    public static List<Seat> build(Long tripId, TrainCarriage carriage) {
        return switch (carriage.getLayout()) {
            case TrainCarriage.LAYOUT_SEAT_2_2 -> buildSeatCar(tripId, carriage);
            case TrainCarriage.LAYOUT_BERTH_4  -> buildBerthCar(tripId, carriage, 4, 2);
            case TrainCarriage.LAYOUT_BERTH_6  -> buildBerthCar(tripId, carriage, 6, 3);
            default -> List.of();
        };
    }

    /**
     * Toa ngồi mềm — đánh số chạy liền từ 1.
     *
     *   hàng 1:  [1] [2] | lối đi | [3] [4]
     *   hàng 2:  [5] [6] | lối đi | [7] [8]
     */
    private static List<Seat> buildSeatCar(Long tripId, TrainCarriage carriage) {
        List<Seat> seats = new ArrayList<>(carriage.seatCount());
        int label = 1;
        for (int row = 1; row <= carriage.getRowCount(); row++) {
            for (int col = 1; col <= SEATS_PER_ROW; col++) {
                seats.add(newSeat(tripId, carriage, label++, row, col, 0, 0));
            }
        }
        return seats;
    }

    /**
     * Toa nằm — mỗi hàng là một KHOANG, trong khoang xếp theo tầng.
     *
     * Khoang 4 (2 tầng, 2 bên):        Khoang 6 (3 tầng, 2 bên):
     *   tầng 1:  [1] [2]                 tầng 1:  [1] [2]
     *   tầng 2:  [3] [4]                 tầng 2:  [3] [4]
     *                                    tầng 3:  [5] [6]
     *
     * Tầng 1 thấp nhất nên dễ lên xuống — đắt nhất. Giá theo tầng do tầng
     * trên tính, ở đây chỉ ghi lại berthLevel.
     *
     * rowNo CỦA TOA NẰM LÀ SỐ KHOANG, không phải số tầng. Hai chỗ cùng
     * rowNo là hai chỗ NGỒI CẠNH NHAU — đó là ý nghĩa của "hàng", và cũng
     * là cách sơ đồ ghế gom nhóm để vẽ. Nhóm theo tầng thì màn hình hiện ra
     * ba khối "tầng 1 / tầng 2 / tầng 3" trải dài cả toa, trong khi khách
     * đi bốn người chỉ muốn biết khoang nào còn đủ bốn giường.
     *
     * colNo là vị trí trong khoang, đánh số chạy theo đúng thứ tự vẽ.
     */
    private static List<Seat> buildBerthCar(Long tripId, TrainCarriage carriage,
                                            int berthsPerCompartment, int levels) {
        List<Seat> seats = new ArrayList<>(carriage.seatCount());
        int label = 1;
        int sidesPerLevel = berthsPerCompartment / levels;   // khoang 4 -> 2, khoang 6 -> 2

        for (int comp = 1; comp <= carriage.getRowCount(); comp++) {
            int position = 1;
            for (int level = 1; level <= levels; level++) {
                for (int side = 1; side <= sidesPerLevel; side++) {
                    seats.add(newSeat(tripId, carriage, label++, comp, position++, comp, level));
                }
            }
        }
        return seats;
    }

    private static Seat newSeat(Long tripId, TrainCarriage carriage, int label,
                                int rowNo, int colNo, int compartment, int berthLevel) {
        return new Seat()
                .setTripId(tripId)
                .setSeatCode("C" + carriage.getNumber() + "-" + label)
                .setCarriageNumber(carriage.getNumber())
                .setSeatClass(carriage.getSeatClass())
                .setLabel(String.valueOf(label))
                .setRowNo(rowNo)
                .setColNo(colNo)
                .setCompartment(compartment)
                .setBerthLevel(berthLevel)
                .setStatus(Seat.STATUS_FREE)
                .setHoldId(null);
    }
}
