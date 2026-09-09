package com.hoangphuc.ddd.controller.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class ResultMessage<T> implements Serializable {
    private boolean success;
    private String message;
    private Integer code;
    private long timestamp = System.currentTimeMillis();
    private T result;
}
