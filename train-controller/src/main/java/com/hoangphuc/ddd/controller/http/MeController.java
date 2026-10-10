package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.PassengerTicketDTO;
import com.hoangphuc.ddd.application.model.UserDTO;
import com.hoangphuc.ddd.application.service.auth.AuthAppService;
import com.hoangphuc.ddd.application.service.mytickets.MyTicketsAppService;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import com.hoangphuc.ddd.controller.support.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Resources that belong to the caller.
 *
 *   GET /me           who I am — the frontend checks a stored token with it on startup
 *   GET /me/tickets   every ticket I have bought
 *
 * "/me" rather than "/users/{id}": the user is whoever is calling, so there is
 * no id in the URL for anyone to swap for someone else's.
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final MyTicketsAppService myTicketsAppService;
    private final AuthAppService authAppService;

    @GetMapping
    public ResultMessage<UserDTO> me() {
        // A valid token for an account that no longer exists counts as signed out
        return authAppService.getUser(CurrentUser.id())
                .map(ResultUtil::data)
                .orElseGet(() -> ResultUtil.error(401, "Please sign in to continue"));
    }

    @GetMapping("/tickets")
    public ResultMessage<List<PassengerTicketDTO>> tickets() {
        return ResultUtil.data(myTicketsAppService.listTickets(CurrentUser.id()));
    }
}
