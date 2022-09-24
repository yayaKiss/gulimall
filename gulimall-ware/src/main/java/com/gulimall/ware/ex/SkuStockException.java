package com.gulimall.ware.ex;

public class SkuStockException extends RuntimeException{
    private Long skuId;

    public  SkuStockException(){};
    public SkuStockException(Long skuId){
        System.out.println("商品" + skuId + " ：没有库存了!");
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
