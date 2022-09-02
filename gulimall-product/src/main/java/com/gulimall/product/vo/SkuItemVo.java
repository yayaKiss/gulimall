package com.gulimall.product.vo;

import com.gulimall.product.entity.SkuImagesEntity;
import com.gulimall.product.entity.SkuInfoEntity;
import com.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    //1、sku基本信息
    SkuInfoEntity info;

    //库存
    Boolean hasStock = true;

    //2、sku的图片信息
    List<SkuImagesEntity> images;

    //3、spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    //4、spu的介绍(描述信息，description描述图片)
    SpuInfoDescEntity desc;

    //5、spu的规格参数
    List<SpuItemAttrGroupVo> groupAttrs;


    @Data
    public static class SkuItemSaleAttrVo{
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuId> attrValues;
    }

    @Data
    public static class SpuItemAttrGroupVo{
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    @Data
    public static class SpuBaseAttrVo{
        private String attrName;
        private String attrValue;
    }

    @Data
    public static class AttrValueWithSkuId{
        private String attrValue;
        private String skuIds;
    }
}
