package com.gulimall.coupon.dao;

import com.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 15:56:50
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
