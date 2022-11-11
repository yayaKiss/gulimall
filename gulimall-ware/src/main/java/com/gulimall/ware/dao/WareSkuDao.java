package com.gulimall.ware.dao;

import com.gulimall.common.to.SkuHasStockVo;
import com.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 13:25:18
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);


    Long selectSkuHasStock(Long skuId);

    List<Long> getWaresBySkuId(@Param("skuId") Long skuId);

    Long lockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("count") Integer count);

    void unLockStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId,@Param("skuNum") Integer skuNum);
}
