package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.HoldCommand;
import com.hoangphuc.ddd.application.model.HoldDTO;
import com.hoangphuc.ddd.application.model.HoldResult;
import com.hoangphuc.ddd.application.model.PassengerCommand;
import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import com.hoangphuc.ddd.controller.dto.CreateHoldRequest;
import com.hoangphuc.ddd.controller.dto.SavePassengersRequest;
import com.hoangphuc.ddd.domain.model.enums.PassengerDiscount;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Vòng đời một lượt giữ chỗ.
 *
 *   POST   /holds           chọn chỗ -> giành ghế, bắt đầu đếm ngược
 *   GET    /holds/{code}    polling  -> còn bao nhiêu giây (theo giờ SERVER)
 *   PUT    /holds/{code}/passengers   ghi tên người ngồi từng ghế, chốt giảm giá
 *   DELETE /holds/{code}    quay lại -> trả ghế ngay
 */
@RestController
@RequestMapping("/holds")
@Slf4j
@RequiredArgsConstructor
public class HoldController {

    /**
     * Chưa có đăng nhập. Mọi lượt giữ chỗ đều ghi về một người dùng giả.
     *
     * Để hằng số ở đây, KHÔNG nhận userId từ body: client gửi userId nào
     * cũng được nghĩa là ai cũng đặt vé hộ người khác, và tệ hơn, xem được
     * vé của người khác khi có màn "vé của tôi". Khi cắm đăng nhập vào thì
     * chỗ cần sửa đúng là một dòng này.
     */
    private static final Long DEMO_USER_ID = 1L;

    private final HoldAppService holdAppService;

    @PostMapping
    public ResultMessage<HoldDTO> create(@Valid @RequestBody CreateHoldRequest request) {
        HoldCommand command = new HoldCommand(
                request.getTripId(),
                request.getSeatClass(),
                distinct(request.getSeatIds()),
                request.getFrom(),
                request.getTo(),
                DEMO_USER_ID);

        return toResponse(holdAppService.createHold(command));
    }

    @GetMapping("/{holdCode}")
    public ResultMessage<HoldDTO> get(@PathVariable("holdCode") String holdCode) {
        return toResponse(holdAppService.getHold(holdCode));
    }

    /**
     * PUT chứ không POST: body là TOÀN BỘ danh sách hành khách, gửi lại bao
     * nhiêu lần cũng ra cùng một trạng thái. Frontend tự thử lại khi mạng
     * chậm, và khách bấm "Quay lại" sửa tên rồi gửi lần nữa là chuyện thường.
     */
    @PutMapping("/{holdCode}/passengers")
    public ResultMessage<HoldDTO> savePassengers(@PathVariable("holdCode") String holdCode,
                                                 @Valid @RequestBody SavePassengersRequest request) {
        List<PassengerCommand> passengers = request.getPassengers().stream()
                .map(this::toCommand)
                .toList();
        return toResponse(holdAppService.savePassengers(holdCode, passengers));
    }

    @DeleteMapping("/{holdCode}")
    public ResultMessage<HoldDTO> release(@PathVariable("holdCode") String holdCode) {
        return toResponse(holdAppService.releaseHold(holdCode));
    }

    /**
     * Bỏ mã chỗ trùng nhau trước khi xuống tầng dưới.
     *
     * Câu UPDATE giành ghế đếm số DÒNG sửa được, nên xin ["C3-1","C3-1"] sẽ
     * sửa 1 dòng trong khi tầng trên chờ 2 — thành ra báo "chỗ đã có người
     * giữ" cho chính cái ghế vừa giữ được. Lọc ở biên thay vì bắt tầng dưới
     * phải đề phòng dữ liệu bẩn.
     */
    private List<String> distinct(List<String> seatIds) {
        return seatIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(seatIds));
    }

    /**
     * Chuẩn hoá ở biên, để DB chỉ có MỘT dạng cho mỗi thứ: tra vé theo số
     * điện thoại mà trong bảng lẫn "0912 345 678" với "+84912345678" thì
     * câu WHERE phone = ? không bao giờ tìm đủ.
     */
    private PassengerCommand toCommand(SavePassengersRequest.PassengerRequest p) {
        String phone = p.getPhone().replaceAll("[\\s.]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        PassengerDiscount discount = p.getDiscount() == null
                ? PassengerDiscount.NONE
                : PassengerDiscount.valueOf(p.getDiscount());
        return new PassengerCommand(
                p.getSeatId().trim(),
                p.getFullName().trim().replaceAll("\\s+", " "),
                p.getIdNumber().trim(),
                phone,
                discount);
    }

    /**
     * Chỉ controller mới biết tới con số HTTP. Tầng dưới trả về enum, vì nếu
     * mai luồng này chạy qua Kafka thay vì HTTP thì enum vẫn dùng được còn
     * số 409 thì vô nghĩa.
     *
     * 410 Gone cho hold hết hạn, không dùng 404: 404 nghĩa là "chưa bao giờ
     * tồn tại", 410 nghĩa là "từng có, giờ mất rồi" — client phân biệt được
     * để hiện đúng câu "Hết giờ giữ chỗ, mời bạn chọn lại".
     */
    private ResultMessage<HoldDTO> toResponse(HoldResult result) {
        return switch (result.getStatus()) {
            case SUCCESS        -> ResultUtil.data(result.getHold());
            case SEAT_TAKEN     -> ResultUtil.error(409, "Cho ban chon vua co nguoi giu, moi chon cho khac");
            case NOT_ON_SALE    -> ResultUtil.error(409, "Chua toi gio mo ban");
            case SALE_ENDED     -> ResultUtil.error(409, "Chuyen nay da chay, khong con ban ve");
            case TRIP_NOT_FOUND -> ResultUtil.error(404, "Khong tim thay chuyen tau");
            case INVALID_ROUTE  -> ResultUtil.error(400, "Ga di hoac ga den khong hop le");
            case HOLD_NOT_FOUND -> ResultUtil.error(404, "Khong tim thay luot giu cho");
            case HOLD_EXPIRED   -> ResultUtil.error(410, "Het gio giu cho, moi ban chon lai");
            case PASSENGER_MISMATCH -> ResultUtil.error(400, "Moi cho dang giu can dung mot hanh khach");
            case ERROR          -> ResultUtil.error(500, "Loi he thong, vui long thu lai");
        };
    }
}
