package com.gulimall.order.vo;

import lombok.Data;

@Data
public class OrderHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
