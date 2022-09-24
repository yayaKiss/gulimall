package com.gulimall.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    private Long id;  //库存工作单的id
    private StockTaskDetailTo detail;  //工作单详情（每一个skuId在哪个wareId锁了num件）
}
