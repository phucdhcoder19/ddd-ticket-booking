package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.HoldDTO;
import com.hoangphuc.ddd.application.model.HoldResult;
import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import com.hoangphuc.ddd.controller.dto.CreateHoldRequest;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Vòng đời một lượt giữ chỗ.
 *
 *   POST   /holds           chọn vé  -> trừ kho, bắt đầu đếm ngược
 *   GET    /holds/{code}    polling  -> còn bao nhiêu giây (theo giờ SERVER)
 *   DELETE /holds/{code}    quay lại -> trả kho ngay
 */
@RestController
@RequestMapping("/holds")
@Slf4j
@RequiredArgsConstructor
public class HoldController {

    private final HoldAppService holdAppService;

    @PostMapping
    public ResultMessage<HoldDTO> create(@Valid @RequestBody CreateHoldRequest request) {
        HoldResult result = holdAppService.createHold(
                request.getTicketId(), request.getUserId(), request.getQuantity());
        return toResponse(result);
    }

    @GetMapping("/{holdCode}")
    public ResultMessage<HoldDTO> get(@PathVariable("holdCode") String holdCode) {
        return toResponse(holdAppService.getHold(holdCode));
    }

    @DeleteMapping("/{holdCode}")
    public ResultMessage<HoldDTO> release(@PathVariable("holdCode") String holdCode) {
        return toResponse(holdAppService.releaseHold(holdCode));
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
            case SUCCESS          -> ResultUtil.data(result.getHold());
            case OUT_OF_STOCK     -> ResultUtil.error(409, "Het ve");
            case NOT_ON_SALE      -> ResultUtil.error(409, "Ve chua mo ban");
            case SALE_ENDED       -> ResultUtil.error(409, "Da het gio ban");
            case TICKET_NOT_FOUND -> ResultUtil.error(404, "Khong tim thay ve");
            case HOLD_NOT_FOUND   -> ResultUtil.error(404, "Khong tim thay luot giu cho");
            case HOLD_EXPIRED     -> ResultUtil.error(410, "Het gio giu cho, moi ban chon lai");
            case ERROR            -> ResultUtil.error(500, "Loi he thong, vui long thu lai");
        };
    }
}
