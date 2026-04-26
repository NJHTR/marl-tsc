package com.njhtr.marltsc.common.exception;

/**
 * External service call failure exception.
 */
public class ServiceException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ServiceException(int errorCode, String message) {
        super(errorCode, message);
    }

    public ServiceException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
