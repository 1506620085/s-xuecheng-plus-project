package com.xuecheng.base.exception;

import com.xuecheng.base.model.BaseResponse;
import com.xuecheng.base.model.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * @author Hangz
 * @version 1.0
 * @description TODO
 * @date 2024/10/12 17:01
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //对项目的自定义异常类型进行处理
    @ExceptionHandler(BusinessException.class)
    /* @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
     * // 在异常捕获类中使用这个注解时，它的作用是告诉 Spring，当这个异常被抛出时，
     * // HTTP 响应应返回指定的状态码,用于简化和规范异常处理逻辑，
     * // 特别适合在 RESTful 风格的 API 中使用，提升代码的可维护性和一致性
     */
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        //记录异常
        log.error("BusinessException: {}", e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }


    @ExceptionHandler(RuntimeException.class)
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> exception(RuntimeException e) {
        //记录异常
        log.error("RuntimeException: {}", e.getMessage(), e);
        return ResultUtils.error(ErrorCode.UNKOWN_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<?> handleMaxSizeException(MaxUploadSizeExceededException e) {
        //记录异常
        log.error("MaxUploadSizeExceededException: {}", e.getMessage(), e);
        //解析出异常信息
        return ResultUtils.error(ErrorCode.FILE_MAX);
    }

}
