package com.sky.exception;

/**
 * 商品业务异常
 */
public class ProductBusinessException extends BaseException{
    public ProductBusinessException() {
    }

    public ProductBusinessException(String msg) {
        super(msg);
    }
}
