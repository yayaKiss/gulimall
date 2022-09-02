package com.gulimall.product.web;

import com.gulimall.product.service.SkuInfoService;
import com.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ItemController {

    @Autowired
    private SkuInfoService skuInfoService;

    /**
     * 展示当前页详情
     * @param skuId
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model){

//        System.out.println("详情skuId:"+skuId);
        //展示skuItem详细信息
        SkuItemVo vo = skuInfoService.item(skuId);
        model.addAttribute("item",vo);

        return "item";
    }
}
