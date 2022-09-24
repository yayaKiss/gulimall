package com.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {
    private Long addrId; //收货地址id
    private Integer payType; //支付方式
    private String orderToken; //防重令牌
    private BigDecimal payPrice; //支付价格
    private String note; //订单备注
}
