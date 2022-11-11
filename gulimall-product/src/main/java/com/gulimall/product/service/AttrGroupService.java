package com.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gulimall.common.utils.PageUtils;
import com.gulimall.product.entity.AttrGroupEntity;
import com.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.gulimall.product.vo.SkuItemVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 12:40:49
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);


    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catalogId);

    List<SkuItemVo.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySkuId(Long catalogId, Long spuId);
}

