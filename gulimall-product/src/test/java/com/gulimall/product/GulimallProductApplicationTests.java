package com.gulimall.product;


import com.gulimall.product.entity.BrandEntity;
import com.gulimall.product.service.BrandService;
import com.gulimall.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;
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


}
