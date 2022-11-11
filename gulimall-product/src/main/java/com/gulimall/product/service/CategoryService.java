package com.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gulimall.common.utils.PageUtils;
import com.gulimall.product.entity.CategoryEntity;
import com.gulimall.product.vo.Catelog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 12:40:48
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);

    Long[] getCategoryPath(Long categoryId);

    void updateDetail(CategoryEntity category);

    /**
     * 查找一级分类
     * @return
     */
    List<CategoryEntity> getFirstCategory();

    Map<String,List<Catelog2Vo>> getCatalogJson();

    Map<String, List<Catelog2Vo>> getCatalogJson2();
}

