package com.hoangphuc.ddd.controller.model.vo;

public class ResultUtil {

    public static <T> ResultMessage<T> data(T t) {
        ResultMessage<T> msg = new ResultMessage<>();
        msg.setSuccess(true);
        msg.setCode(200);
        msg.setMessage("success");
        msg.setResult(t);
        return msg;
    }

    public static <T> ResultMessage<T> error(int code, String message) {
        ResultMessage<T> msg = new ResultMessage<>();
        msg.setSuccess(false);
        msg.setCode(code);
        msg.setMessage(message);
        return msg;
    }
}
