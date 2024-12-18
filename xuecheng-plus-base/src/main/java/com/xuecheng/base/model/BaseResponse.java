package com.xuecheng.base.model;

import com.xuecheng.base.exception.ErrorCode;
import lombok.Data;
import lombok.ToString;

/**
 * 通用结果类型
 */

@Data
@ToString
public class BaseResponse<T> {

    /**
     * 响应编码,0为正常,-1错误
     */
    private int code;

    /**
     * 响应提示信息
     */
    private String message;

    /**
     * 响应内容
     */
    private T data;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

}
