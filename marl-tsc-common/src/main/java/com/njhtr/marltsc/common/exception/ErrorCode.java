package com.njhtr.marltsc.common.exception;

import lombok.Getter;

/**
 * Unified error code enumeration.
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),
    PARAM_ERROR(4001, "参数错误"),
    PLAN_NOT_FOUND(4002, "信号方案不存在"),
    SYSTEM_ERROR(5000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
