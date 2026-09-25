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
 * Dựng Journey từ (ga đi, ga đến, ngày) rồi tính giá.
 *
 * FarePolicy là luật thuần — cho nó cây số, nó trả tiền. Nhưng "cây số" và
 * "hệ số cao điểm" phải đi tra cơ sở dữ liệu và đọc cấu hình, mà luật thuần
 * thì không được phép làm hai việc đó. Class này là phần bẩn nằm giữa: nó
 * biết chỗ tra, FarePolicy biết cách tính.
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
     * @return rỗng nếu mã ga không có thật, hoặc đi và đến trùng nhau
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
        // Trị tuyệt đối: đi vào Nam hay ra Bắc thì quãng đường vẫn thế.
        int distanceKm = Math.abs(to.getKmFromHanoi() - from.getKmFromHanoi());
        return Optional.of(new Journey(from, to, distanceKm, surchargeFor(date)));
    }

    /** Giá đúng của MỘT chỗ cụ thể — tầng giường đã nằm sẵn trong ghế. */
    public long fareOf(Journey journey, Seat seat) {
        return FarePolicy.fare(journey.distanceKm(), seat.getSeatClass(),
                seat.getBerthLevel(), journey.surcharge());
    }

    /** Giá của một hạng chỗ ở một tầng giường bất kỳ — dùng cho giá "từ ...đ". */
    public long fareOf(Journey journey, String seatClass, int berthLevel) {
        return FarePolicy.fare(journey.distanceKm(), seatClass, berthLevel, journey.surcharge());
    }

    /**
     * Phụ thu cao điểm Tết.
     *
     * Lấy từ cấu hình chứ không tính âm lịch: ngành đường sắt công bố khung
     * ngày cao điểm bằng văn bản hành chính, không suy ra từ lịch. Và nhúng
     * cả bộ chuyển đổi âm lịch vào backend chỉ để nhân một hệ số là không đáng.
     */
    private double surchargeFor(LocalDate date) {
        try {
            LocalDate start = LocalDate.parse(peakFrom);
            LocalDate end = LocalDate.parse(peakTo);
            boolean inPeak = !date.isBefore(start) && !date.isAfter(end);
            return inPeak ? peakSurcharge : 1.0d;
        } catch (Exception e) {
            log.warn("[GIA] khung ngay cao diem cau hinh sai, dung gia thuong", e);
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
