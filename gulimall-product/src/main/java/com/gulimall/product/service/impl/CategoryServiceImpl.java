package com.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.gulimall.product.service.CategoryBrandRelationService;
import com.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.product.dao.CategoryDao;
import com.gulimall.product.entity.CategoryEntity;
import com.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //??????????????????
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //????????????????????????
        List<CategoryEntity> level1Menus = categoryEntities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((menu) -> {
                    menu.setChildren(getChildren(menu, categoryEntities));
                    return menu;
                }).sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] getCategoryPath(Long categoryId) {
        List<Long> list = new ArrayList<>();
        if (categoryId == 0) {
            return new Long[0];
        }
        return getParentPath(categoryId, list);
    }

//    @CacheEvict(value = "category",key = "'getFirstCategory'")
//    @Caching(evict = {
//            @CacheEvict(value = "category",key = "'getFirstCategory'"),
//            @CacheEvict(value = "category",key = "'getCatalogJson'")
//    })
    @CacheEvict(value = "category", allEntries = true)
    @Override
    @Transactional
    public void updateDetail(CategoryEntity category) {
        this.updateById(category);
        if (!StringUtils.isEmpty(category.getName())) {
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
        }
    }

    @Override
    @Cacheable(value = "category",key = "#root.method.name",sync = true)
    public List<CategoryEntity> getFirstCategory() {
        System.out.println("getLevel1Category.......");
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
    }

    /**
     * @CacheEvict ??? ??????????????????????????????????????????????????????
     * @return
     */
    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        System.out.println("??????????????????");

        //???????????????????????????????????????
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //1?????????????????????
        //1???1???????????????????????????
        List<CategoryEntity> level1Categorys = selectCategory(selectList, 0L);

        //????????????
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1???????????????????????????,???????????????????????????????????????
            List<CategoryEntity> categoryEntities = selectCategory(selectList, v.getCatId());

            //2????????????????????????
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());

                    //1????????????????????????????????????????????????vo
                    List<CategoryEntity> level3Catelog = selectCategory(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2????????????????????????
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(category3Vos);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        return parentCid;
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        //???????????????
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        //???????????????????????????????????????,?????????????????????

        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("??????????????????.....???????????????....");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
            return catalogJsonFromDb;
        }

        Map<String, List<Catelog2Vo>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return stringListMap;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {
        //??????????????????
        RLock lock = redissonClient.getLock("catalogJson-lock");
        Map<String, List<Catelog2Vo>> dateFromDb = null;
        lock.lock();
        try{
            dateFromDb = getDateFromDb();
        }finally {
            lock.unlock();
        }
        return dateFromDb;
    }


    private Map<String, List<Catelog2Vo>> getDateFromDb() {
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        if(!StringUtils.isEmpty(catalogJson)){
            //???????????????????????????
            return JSON.parseObject(catalogJson,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        }
        System.out.println("??????????????????....");
        //1???1???????????????????????????
        List<CategoryEntity> level1Categories = getFirstCategory();

        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //????????????
        Map<String, List<Catelog2Vo>> parentCid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1???????????????????????????,???????????????????????????????????????
            List<CategoryEntity> level2Categories = selectCategory(categoryEntities, v.getCatId());

            //2????????????????????????
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Categories != null) {
                catelog2Vos = level2Categories.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());
                    //1????????????????????????????????????????????????vo
                    List<CategoryEntity> level3Catelog = selectCategory(categoryEntities, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2????????????????????????
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(category3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        String s = JSON.toJSONString(parentCid);
        stringRedisTemplate.opsForValue().set("catalogJson", s,1,TimeUnit.DAYS);
        return parentCid;
    }

    private List<CategoryEntity> selectCategory(List<CategoryEntity> categoryEntities, Long i) {
        return categoryEntities.stream().filter(item -> item.getParentCid() == i).collect(Collectors.toList());
    }


    private Long[] getParentPath(Long categoryId, List<Long> list) {
        list.add(categoryId);
        CategoryEntity entity = this.getById(categoryId);
        if (entity.getParentCid() != 0) {
            getParentPath(entity.getParentCid(), list);
        }
        Collections.reverse(list);
        return list.toArray(new Long[list.size()]);
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        //?????????????????????
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return root.getCatId().equals(categoryEntity.getParentCid());
        }).map((categoryEntity) -> {
            //????????????????????????????????????
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}