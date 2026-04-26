package com.njhtr.marltsc.common.exception;

/**
 * Invalid parameter exception.
 */
public class ParamException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ParamException(String message) {
        super(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    public ParamException(String message, Throwable cause) {
        super(ErrorCode.PARAM_ERROR.getCode(), message, cause);
    }
}
