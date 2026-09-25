package com.hoangphuc.ddd.application.service.trip;

import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLockService;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLocker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Sinh chuyến tàu khi có người đầu tiên tìm tới ngày đó.
 *
 * Đây là CÙNG MỘT BÀI TOÁN với cache stampede ở TicketDetailCacheService,
 * chỉ khác là thứ đắt tiền không phải một câu SELECT mà là ghi ~600 dòng ghế:
 *
 *   1. Hỏi xem chuyến có chưa
 *   2. Chưa -> giành khoá; chỉ một luồng được sinh
 *   3. Sau khi có khoá, HỎI LẠI LẦN NỮA (double-check)
 *   4. Sinh ghế, nhả khoá
 *
 * Thiếu bước 3 thì 5000 người cùng tìm sẽ lần lượt sinh 5000 lần — chỉ khác
 * là xếp hàng thay vì ùa cùng lúc. Kết quả: 3 triệu dòng ghế trùng.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TripProvisionService {

    private static final long LOCK_WAIT_SECONDS = 3;
    /** Sinh 600 ghế mất ~1s, cho rộng tay phòng lúc DB bận. */
    private static final long LOCK_LEASE_SECONDS = 20;

    private final TripRepository tripRepository;
    private final DistributedLockService distributedLockService;
    private final TripCreationService tripCreationService;

    /**
     * Lấy chuyến của (tàu, ngày). Chưa có thì sinh.
     *
     * @return chuyến đã sẵn sàng bán, hoặc rỗng nếu không sinh được
     */
    public Optional<Trip> ensureTrip(Train train, LocalDate serviceDate) {
        // ---- BƯỚC 1: đã có chưa ----
        Optional<Trip> existing = tripRepository.findByTrainAndDate(train.getId(), serviceDate);
        if (existing.isPresent()) {
            return existing;
        }

        // ---- BƯỚC 2: giành khoá ----
        String lockKey = "LOCK:TRIP:" + train.getId() + ":" + serviceDate;
        DistributedLocker locker = distributedLockService.getLock(lockKey);
        boolean locked = false;
        try {
            locked = locker.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                // Người khác đang sinh. Chờ 3s rồi mà chưa xong -> thử đọc lần cuối.
                log.warn("[TRIP] khong gianh duoc khoa | {}", lockKey);
                return tripRepository.findByTrainAndDate(train.getId(), serviceDate);
            }

            // ---- BƯỚC 3: DOUBLE-CHECK ----
            existing = tripRepository.findByTrainAndDate(train.getId(), serviceDate);
            if (existing.isPresent()) {
                return existing;
            }

            // ---- BƯỚC 4: chỉ MỘT luồng tới được đây ----
            return Optional.of(tripCreationService.createTripWithSeats(train, serviceDate));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (locked) {
                locker.unlock();
            }
        }
    }

}
