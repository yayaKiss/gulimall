package com.gulimall.common.exception;

/**
 *   10:通用：
 *      001： 参数格式
 *      002： 验证码发送频率太高
 *   11:商品上架
 *   12:订单
 *   13:购物车
 *   14:物流
 *   15:用户
 *   16:库存
 */
public enum BizCodeEnum {
    UNKNOWN_EXCEPTION(10000,"系统未知异常"),
    VALID_EXCEPTION(10001,"参数格式校验异常"),
    SMS_CODE_EXCEPTION(10002,"验证码频繁发送，请稍后重试"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),
    PHONE_EXIST_EXCEPTION(15001,"手机号已存在"),
    USERNAME_EXIST_EXCEPTION(15002,"用户名已存在"),
    USERNAME_PASSWORD_ERROR(15003,"用户名或密码错误"),
    NO_STOCK_EXCEPTION(16000,"库存不足");

    private Integer code;
    private String msg;

    BizCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
