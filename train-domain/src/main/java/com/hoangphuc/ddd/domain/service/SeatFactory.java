package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the seat list of one carriage for one trip.
 *
 * A static utility class, NOT an interface + impl like the other domain
 * services in this repo. Reason: it depends on nothing — no repository, no
 * cache, no clock. The same input always gives the same output. There is
 * nothing to swap out, so an interface would only be one more empty file to
 * open every time you read the code.
 */
public final class SeatFactory {

    private SeatFactory() {
    }

    /** Seating carriage: 4 seats per row, 2 on each side of the aisle. */
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
     * Soft seat carriage — numbered continuously from 1.
     *
     *   row 1:  [1] [2] | aisle | [3] [4]
     *   row 2:  [5] [6] | aisle | [7] [8]
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
     * Sleeper carriage — each row is a COMPARTMENT, ordered by level inside it.
     *
     * 4-berth (2 levels, 2 sides):     6-berth (3 levels, 2 sides):
     *   level 1:  [1] [2]                level 1:  [1] [2]
     *   level 2:  [3] [4]                level 2:  [3] [4]
     *                                    level 3:  [5] [6]
     *
     * Level 1 is the lowest and easiest to get into — the most expensive. The
     * price per level is computed by the layer above; here we only record
     * berthLevel.
     *
     * rowNo OF A SLEEPER CARRIAGE IS THE COMPARTMENT NUMBER, not the level. Two
     * places with the same rowNo are places NEXT TO EACH OTHER — that is what
     * "row" means, and it is how the seat map groups places for drawing. Group
     * by level instead and the screen shows three blocks "level 1 / level 2 /
     * level 3" stretching across the whole carriage, while a family of four
     * only wants to know which compartment still has four berths.
     *
     * colNo is the position within the compartment, numbered in drawing order.
     */
    private static List<Seat> buildBerthCar(Long tripId, TrainCarriage carriage,
                                            int berthsPerCompartment, int levels) {
        List<Seat> seats = new ArrayList<>(carriage.seatCount());
        int label = 1;
        int sidesPerLevel = berthsPerCompartment / levels;   // 4-berth -> 2, 6-berth -> 2

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
