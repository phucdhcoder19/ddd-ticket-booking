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
 * Waiting room in front of the shop.
 *
 *   POST /queue           ask for a turn
 *   GET  /queue/{token}   is it my turn yet
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

    /**
     * 404 when the token is nowhere anymore (made up, or admitted and left to expire).
     * The frontend catches this 404 and joins again at the back.
     */
    @GetMapping("/{token}")
    public ResultMessage<QueueTicketDTO> status(@PathVariable("token") String token) {
        return queueAppService.status(token)
                .map(ResultUtil::data)
                .orElseGet(() -> ResultUtil.error(404, "Queue ticket not found or expired, please join the queue again"));
    }
}
