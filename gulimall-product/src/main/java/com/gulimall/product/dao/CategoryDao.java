package com.gulimall.product.dao;

import com.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 12:40:48
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
