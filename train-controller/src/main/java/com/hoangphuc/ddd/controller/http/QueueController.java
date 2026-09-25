package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.QueueTicketDTO;
import com.hoangphuc.ddd.application.service.queue.QueueAppService;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phong cho truoc cua hang.
 *
 *   POST /queue           xin mot luot
 *   GET  /queue/{token}   toi luot chua
 */
@RestController
@RequestMapping("/queue")
@Slf4j
@RequiredArgsConstructor
public class QueueController {

    private final QueueAppService queueAppService;

    @PostMapping
    public ResultMessage<QueueTicketDTO> join() {
        return ResultUtil.data(queueAppService.join());
    }

    @GetMapping("/{token}")
    public ResultMessage<QueueTicketDTO> status(@PathVariable("token") String token) {
        return ResultUtil.data(queueAppService.status(token));
    }
}
