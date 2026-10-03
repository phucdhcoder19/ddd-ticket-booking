package com.hoangphuc.ddd.application.service.pricing;

import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.Station;
import com.hoangphuc.ddd.domain.service.FarePolicy;
import com.hoangphuc.ddd.domain.service.StationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a Journey from (departure station, arrival station, date) and prices it.
 *
 * FarePolicy is a pure rule — give it kilometres, it gives back money. But
 * "kilometres" and "peak factor" require a database lookup and reading the
 * config, which a pure rule must not do. This class is the impure part in
 * between: it knows where to look things up, FarePolicy knows how to compute.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JourneyPricingService {

    private final StationDomainService stationDomainService;

    @Value("${app.pricing.peak-from:2027-01-25}")
    private String peakFrom;

    @Value("${app.pricing.peak-to:2027-02-05}")
    private String peakTo;

    @Value("${app.pricing.peak-surcharge:1.35}")
    private double peakSurcharge;

    /**
     * @return empty if a station code does not exist, or departure equals arrival
     */
    public Optional<Journey> resolve(String fromCode, String toCode, LocalDate date) {
        if (fromCode == null || toCode == null || fromCode.equals(toCode)) {
            return Optional.empty();
        }
        Map<String, Station> stations = indexStations();
        Station from = stations.get(fromCode);
        Station to = stations.get(toCode);
        if (from == null || to == null) {
            return Optional.empty();
        }
        // Absolute value: southbound or northbound, the distance is the same.
        int distanceKm = Math.abs(to.getKmFromHanoi() - from.getKmFromHanoi());
        return Optional.of(new Journey(from, to, distanceKm, surchargeFor(date)));
    }

    /** The exact price of ONE specific seat — the berth level is already on the seat. */
    public long fareOf(Journey journey, Seat seat) {
        return FarePolicy.fare(journey.distanceKm(), seat.getSeatClass(),
                seat.getBerthLevel(), journey.surcharge());
    }

    /** Price of a seat class at a given berth level — used for "from ... VND" prices. */
    public long fareOf(Journey journey, String seatClass, int berthLevel) {
        return FarePolicy.fare(journey.distanceKm(), seatClass, berthLevel, journey.surcharge());
    }

    /**
     * Lunar New Year peak surcharge.
     *
     * Read from config rather than computed from the lunar calendar: the
     * railway publishes the peak dates in an official notice, they are not
     * derived from the calendar. And embedding a lunar calendar converter in
     * the backend just to multiply by one factor is not worth it.
     */
    private double surchargeFor(LocalDate date) {
        try {
            LocalDate start = LocalDate.parse(peakFrom);
            LocalDate end = LocalDate.parse(peakTo);
            boolean inPeak = !date.isBefore(start) && !date.isAfter(end);
            return inPeak ? peakSurcharge : 1.0d;
        } catch (Exception e) {
            log.warn("[PRICE] peak date range is misconfigured, using the normal fare", e);
            return 1.0d;
        }
    }

    private Map<String, Station> indexStations() {
        Map<String, Station> map = new HashMap<>();
        for (Station s : stationDomainService.listStations()) {
            map.put(s.getCode(), s);
        }
        return map;
    }
}
