package com.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.gulimall.product.entity.AttrEntity;
import com.gulimall.product.service.AttrService;
import com.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.product.dao.AttrGroupDao;
import com.gulimall.product.entity.AttrGroupEntity;
import com.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    @Autowired
    private AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(!StringUtils.isEmpty(key)){
            wrapper.and(obj -> {
                obj.eq("attr_group_id",key).or().like("attr_group_name",key);

            });
        }
        if (catelogId == 0){
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper
            );
            return new PageUtils(page);
        }else{
            wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),wrapper);
            return new PageUtils(page);
        }
    }

    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        List<AttrGroupWithAttrsVo> collect = groupEntities.stream().map(item -> {
            AttrGroupWithAttrsVo vos = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(item, vos);
            List<AttrEntity> attrs = attrService.getRelationAttr(item.getAttrGroupId());
            vos.setAttrs(attrs);
            return vos;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public List<SkuItemVo.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySkuId(Long catalogId, Long spuId) {
        AttrGroupDao baseMapper = this.getBaseMapper();
        List<SkuItemVo.SpuItemAttrGroupVo> attrGroupVos = baseMapper.getAttrGroupWithAttrsBySkuId(catalogId,spuId);
        return attrGroupVos;
    }

}