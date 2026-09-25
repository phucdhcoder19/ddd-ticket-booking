package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TripDTO {

    /** Kieu String vi frontend dung no trong URL. */
    private String id;

    private String trainCode;

    /** Ga di/den la CUA CHUYEN DI CUA KHACH, khong phai cua doan tau.
     *  SE1 chay Ha Noi - Sai Gon, nhung khach co the chi di Hue - Da Nang. */
    private StationDTO fromStation;
    private StationDTO toStation;

    private LocalDateTime departAt;
    private LocalDateTime arriveAt;
    private int durationMinutes;

    private List<SeatClassOfferDTO> classes;

    /** Tong cho con cua ca chuyen — frontend dung cho nhan "Sap het"/"Het ve". */
    private int availableTotal;
}
