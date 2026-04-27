package com.njhtr.marltsc.coopt.api;

import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.common.result.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResult.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ApiResult.fail(500, "Internal server error: " + e.getMessage());
    }
}
