package com.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gulimall.common.to.SkuHasStockVo;
import com.gulimall.common.to.mq.OrderTo;
import com.gulimall.common.to.mq.StockLockedTo;
import com.gulimall.common.utils.PageUtils;
import com.gulimall.ware.entity.WareSkuEntity;
import com.gulimall.ware.vo.LockStockResult;
import com.gulimall.ware.vo.OrderVo;
import com.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 13:25:18
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    boolean orderLockStock(WareSkuLockVo skuLockVo);

    void unLockStock(StockLockedTo stockLockedTo);

    void unLockStock(OrderTo orderTo);
}

