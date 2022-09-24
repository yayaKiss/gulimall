package com.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class WareSkuLockVo {
    //订单号
    private String orderSn;
    List<OrderItemVo> locks;//需要锁住的库存信息（skuId，count）
}
