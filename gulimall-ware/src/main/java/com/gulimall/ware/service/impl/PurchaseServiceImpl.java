package com.gulimall.ware.service.impl;

import com.gulimall.common.constant.WareConstant;
import com.gulimall.ware.entity.PurchaseDetailEntity;
import com.gulimall.ware.feign.ProductFeign;
import com.gulimall.ware.service.PurchaseDetailService;
import com.gulimall.ware.service.WareSkuService;
import com.gulimall.ware.vo.MergeVo;
import com.gulimall.ware.vo.PurchaseItemDoneVo;
import com.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.ware.dao.PurchaseDao;
import com.gulimall.ware.entity.PurchaseEntity;
import com.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {
    @Autowired
    private PurchaseDetailService purchaseDetailService;
    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", WareConstant.PurchaseStatusEnum.CREATE.getCode())
                        .or().eq("status",WareConstant.PurchaseStatusEnum.ASSIGNED.getCode())
        );

        return new PageUtils(page);
    }

    @Override
    public void mergePurchase(MergeVo vo) {
        Long purchaseId = vo.getPurchaseId();
        if(purchaseId == null){
            //?????????????????????
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATE.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);

            //??????????????????id
            purchaseId = purchaseEntity.getId();
        }
        //??????????????????id
        List<Long> items = vo.getItems();
        //???????????????????????????0,1?????????????????????
        List<PurchaseDetailEntity> detailEntities = purchaseDetailService.listByIds(items);
        List<Long> collect1 = detailEntities.stream().filter(item -> {
            return item.getStatus() == WareConstant.PurchaseDetailStatusEnum.CREATE.getCode()
                    || item.getStatus() == WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode();
        }).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());


        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = collect1.stream().map(i -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(i);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());

            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        purchaseDetailService.updateBatchById(collect);

        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Transactional
    @Override
    public void received(List<Long> ids) {
        if(ids != null){
            //??????????????????????????????????????????????????????
            List<PurchaseEntity> purchaseEntities = this.listByIds(ids);
            //1?????????    2???????????????????????????????????????
            List<PurchaseEntity> collect = purchaseEntities.stream().filter(item -> {
                return item.getStatus() == WareConstant.PurchaseStatusEnum.CREATE.getCode()
                        || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
            }).map(item -> {
                item.setUpdateTime(new Date());
                item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
                return item;
            }).collect(Collectors.toList());

            //????????????????????????
            this.updateBatchById(collect);

            //????????????????????????
            collect.forEach(item -> {
                List<PurchaseDetailEntity> detailEntities = purchaseDetailService.listDetailByPurchaseId(item.getId());
                detailEntities.forEach(entity -> {
                    entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                });
                purchaseDetailService.updateBatchById(detailEntities);
            });
        }

    }

    @Override
    @Transactional
    public void done(PurchaseDoneVo doneVo) {
        Long purchaseId = doneVo.getId();

        //2???????????????????????????
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();

        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.ERROR.getCode()){
                flag = false;
                detailEntity.setStatus(item.getStatus());
            }else{
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                ////3?????????????????????????????????
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum());

            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }

        purchaseDetailService.updateBatchById(updates);

        //1????????????????????????
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode():WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }


}