package com.njhtr.marltsc.common.exception;

/**
 * Business rule violation exception.
 */
public class BusinessException extends BaseException {

    private static final long serialVersionUID = 1L;

    public BusinessException(int errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
