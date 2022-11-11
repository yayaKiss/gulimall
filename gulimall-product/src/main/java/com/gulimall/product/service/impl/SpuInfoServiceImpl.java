package com.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.gulimall.common.constant.ProductConstant;
import com.gulimall.common.to.MemberPrice;
import com.gulimall.common.to.SkuHasStockVo;
import com.gulimall.common.to.SkuReductionTo;
import com.gulimall.common.to.SpuBoundsTo;
import com.gulimall.common.to.es.SkuEsModel;
import com.gulimall.common.utils.R;
import com.gulimall.product.entity.*;
import com.gulimall.product.feign.CouponFeignService;
import com.gulimall.product.feign.SearchFeignService;
import com.gulimall.product.feign.WareFeignService;
import com.gulimall.product.service.*;
import com.gulimall.product.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存spu相关信息
     * @param vo
     */

    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1、保存spu基本信息  pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.save(spuInfoEntity);
        //2、保存spu描述图片  pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",", decript));
        spuInfoDescService.save(spuInfoDescEntity);
        //3、保存spu图片集   pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId(),images);

        //4、保存spu的属性（规格参数）  pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        productAttrValueService.saveProductAttr(spuInfoEntity.getId(),baseAttrs);

        //5、保存积分信息   sms_spu_bounds
        SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
        BeanUtils.copyProperties(vo.getBounds(),spuBoundsTo);
        spuBoundsTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundsTo);
        if(r.getCode() != 0){
            log.error("远程调用积分服务失败!");
        }

        //6、保存spu的所有sku信息
        List<Skus> skus = vo.getSkus();
        if(skus != null || skus.size() > 0){
            skus.forEach(item -> {
                //6.1）、保存sku基本信息  pms_sku_info
                String DefaultImg = "";
                for(Images img : item.getImages()){
                    if(img.getDefaultImg() == 1){
                        DefaultImg = img.getImgUrl();
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item,skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSkuDefaultImg(DefaultImg);

                skuInfoService.saveSkuInfo(skuInfoEntity);

                //6.2）、保存sku图片信息  pms_sku_images
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(image.getImgUrl());
                    skuImagesEntity.setDefaultImg(image.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity -> {
                    return !StringUtils.isEmpty(entity.getImgUrl());
                        }).collect(Collectors.toList());
                skuImagesService.saveBatch(imagesEntities);
                //TODO 没有图片url，不进行保存

                //6.3）、保存sku销售属性信息  pms_sku_sale_attr_value
                List<Attr> attrs = item.getAttr();
                List<SkuSaleAttrValueEntity> collect = attrs.stream().map(attr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr,skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(collect);

                //6.4）、sku的优惠、满减等信息 ： 打折-> sms_sku_ladder  满减-> sms_sku_full_reduction 会员价-> sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                skuReductionTo.setSkuId(skuId);
                BeanUtils.copyProperties(item,skuReductionTo);
                List<MemberPrice> list = new ArrayList<>();
                item.getMemberPrice().forEach(memberPrice -> {
                    MemberPrice memberPrice1 = new MemberPrice();
                    BeanUtils.copyProperties(memberPrice,memberPrice1);
                    list.add(memberPrice1);
                });
                skuReductionTo.setMemberPrice(list);

                if(skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal(0)) == 1){
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode() != 0){
                        log.error("远程调用保存sku满减、折扣、会员信息服务失败!");
                    }
                }
            });
        }

    }

    /**
     * spu管理 条件查询
     * @param params
     * @return
     *  key: 检索关键字
     *  catelogId: 三级分类id
     *  brandId: 品牌id
     *  status: 商品状态
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w -> {
               w.eq("id",key).or().like("spu_name",key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equals(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equals(brandId)){
            wrapper.eq("brand_id",brandId);
        }

        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status) && !"0".equals(status)){
            wrapper.eq("publish_status",status);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void up(Long spuId) {

        //1、查询出当前spuId对应的所有sku的信息
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        //TODO 4、查询出当前sku所有可以被检索的规格参数
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        List<Long> searchAttrIds = attrService.getSearchAttrs(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs);
            return attrs;
        }).collect(Collectors.toList());

        Map<Long, Boolean> stockMap = null;
        try{
            R hasStock = wareFeignService.getSkuHasStock(skuIdList);
            Object data = hasStock.get("data");//将json数据转为map
            String s = JSON.toJSONString(data);
            List<SkuHasStockVo> list = JSON.parseObject(s, new TypeReference<List<SkuHasStockVo>>() {
            });
            stockMap = list.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));

        }catch (Exception e){
            log.error("远程调用失败,原因:" + e);
        }

        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProduct = skus.stream().map(sku -> {
            //组装需要的数据
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku,esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            //hasStock,hotScore
            //TODO 1、远程调用查看是否有库存
            if(finalStockMap == null){
                esModel.setHasStock(true);
            }else{
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }

            //TODO 2、热度评分  0
            esModel.setHotScore(0L);

            //TODO 3、查询品牌的分类和名字信息
            BrandEntity brand = brandService.getById(sku.getBrandId());
            CategoryEntity category = categoryService.getById(sku.getCatalogId());
            esModel.setBrandImg(brand.getLogo());
            esModel.setBrandName(brand.getName());
            esModel.setCatalogName(category.getName());

            //设置检索属性
            esModel.setAttrs(attrsList);

            return esModel;
        }).collect(Collectors.toList());

        //5、TODO 将数据发送给es进行保存
        R r = searchFeignService.productStatusUp(upProduct);
        if(r.getCode() == 0){
            //远程调用成功
            baseMapper.updateSpuStatus(spuId, ProductConstant.SpuStatus.SPU_UP.getCode());
        }else{
            //远程调用失败
            //TODO  重复调用？ 接口幂等性问题

        }

    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
        Long spuId = skuInfo.getSpuId();

        return this.getById(spuId);
    }

}