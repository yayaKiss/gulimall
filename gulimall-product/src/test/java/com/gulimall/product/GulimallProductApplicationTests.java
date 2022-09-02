package com.gulimall.product;


import com.gulimall.product.dao.SkuSaleAttrValueDao;
import com.gulimall.product.entity.BrandEntity;
import com.gulimall.product.service.AttrGroupService;
import com.gulimall.product.service.BrandService;
import com.gulimall.product.service.CategoryService;
import com.gulimall.product.service.SkuSaleAttrValueService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.UUID;

@SpringBootTest
class GulimallProductApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BrandService brandService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    void testSaleAttrDao(){
        System.out.println(skuSaleAttrValueDao.getSaleAttrsBySpuId(11L));
    }

    @Test
    void redissonTest(){
        System.out.println(redissonClient);
    }

    @Test
    public void test(){
        System.out.println(attrGroupService.getAttrGroupWithAttrsBySkuId(225L, 100L));
    }

    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("小米");
        brandService.save(brandEntity);
        System.out.println("插入成功");
    }
    @Test
    void testFindPath(){
        Long[] categoryPath = categoryService.getCategoryPath(255L);
        System.out.println(Arrays.asList(categoryPath));
    }

    @Test
    void testRedis(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello","world_" + UUID.randomUUID().toString());

        System.out.println(ops.get("hello"));
    }


}
