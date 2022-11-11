package com.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gulimall.common.utils.PageUtils;
import com.gulimall.product.entity.AttrEntity;
import com.gulimall.product.vo.AttrGroupRelationVo;
import com.gulimall.product.vo.AttrRespVo;
import com.gulimall.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 12:40:49
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId,String type);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    List<AttrEntity> getRelationAttr(Long attrGroupId);

    void deleteRelationAttr(AttrGroupRelationVo[] relationVo);

    PageUtils getNoRelationAttr(Long attrGroupId, Map<String, Object> params);

    /**
     * 在指定的所有attrId中查询出可以检索的attrId
     * @param attrIds
     * @return
     */
    List<Long> getSearchAttrs(List<Long> attrIds);
}

