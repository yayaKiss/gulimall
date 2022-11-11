package com.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;


import com.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.gulimall.product.entity.SpuInfoEntity;
import com.gulimall.product.service.SpuInfoService;
import com.gulimall.common.utils.PageUtils;
import com.gulimall.common.utils.R;



/**
 * spu信息
 *
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 12:40:48
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    @GetMapping("/skuId/{skuId}")
    public R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId){
        SpuInfoEntity spuInfo = spuInfoService.getSpuInfoBySkuId(skuId);
        return R.ok().put("data",spuInfo);
    }

//    /product/spuinfo/{spuId}/up
    @PostMapping("/{spuId}/up")
    //@RequiresPermissions("product:spuinfo:list")
    public R list(@PathVariable("spuId") Long spuId){

        spuInfoService.up(spuId);

        return R.ok();
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:spuinfo:list")
    public R list(@RequestParam Map<String, Object> params){
//        PageUtils page = spuInfoService.queryPage(params);
        PageUtils page = spuInfoService.queryPageByCondition(params);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("product:spuinfo:info")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:spuinfo:save")
    public R save(@RequestBody SpuSaveVo vo){
//		spuInfoService.save(spuInfo);
        spuInfoService.saveSpuInfo(vo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:spuinfo:update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:spuinfo:delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
