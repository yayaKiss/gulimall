package com.gulimall.coupon.service.impl;

import com.gulimall.common.to.MemberPrice;
import com.gulimall.common.to.SkuReductionTo;
import com.gulimall.coupon.entity.MemberPriceEntity;
import com.gulimall.coupon.entity.SkuLadderEntity;
import com.gulimall.coupon.service.MemberPriceService;
import com.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.coupon.dao.SkuFullReductionDao;
import com.gulimall.coupon.entity.SkuFullReductionEntity;
import com.gulimall.coupon.service.SkuFullReductionService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    private SkuLadderService skuLadderService;
    @Autowired
    private MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        //保存满减信息
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        skuFullReductionEntity.setSkuId(skuReductionTo.getSkuId());
        skuFullReductionEntity.setFullPrice(skuReductionTo.getFullPrice());
        skuFullReductionEntity.setReducePrice(skuReductionTo.getReducePrice());
        skuFullReductionEntity.setAddOther(skuReductionTo.getPriceStatus());
        if(skuFullReductionEntity.getFullPrice().compareTo(new BigDecimal(0)) > 0){
            this.save(skuFullReductionEntity);
        }


        //保存折扣信息
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        if(skuLadderEntity.getFullCount()  > 0){
            skuLadderService.save(skuLadderEntity);
        }


        //保存会员折扣信息
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();

        List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
            MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
            memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
            memberPriceEntity.setMemberLevelId(item.getId());
            memberPriceEntity.setMemberPrice(item.getPrice());
            memberPriceEntity.setMemberLevelName(item.getName());
            memberPriceEntity.setAddOther(1);
            return memberPriceEntity;
        }).filter(item -> {
            return item.getMemberPrice().compareTo(new BigDecimal(0)) > 0;
        }).collect(Collectors.toList());
        memberPriceService.saveBatch(collect);
    }

}