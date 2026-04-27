package com.njhtr.marltsc.web.api;

import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.common.result.ApiResult;
import feign.FeignException;
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

    @ExceptionHandler(FeignException.class)
    public ApiResult<Void> handleFeignException(FeignException e) {
        log.warn("Downstream service call failed: {}", e.getMessage());
        return ApiResult.fail(502, "下游服务不可用: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ApiResult.fail(4001, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ApiResult.fail(5000, "Internal server error");
    }
}
