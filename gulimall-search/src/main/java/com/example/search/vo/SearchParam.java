package com.example.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {
    private String keyword;   //页面传递的全文匹配

    private Long catalog3Id; //三级分类Id

    /**
     * 排序条件
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;

    /**
     * 过滤条件：hasStock(是否有货),skuPrice区间，brandId,catalog3Id,attr
     * hasStock=0/1
     * skuPrice=0_500/_500/500_
     * brandId=1&brandId=2&brandId=3
     * attrs=1_4寸:5寸&
     */
    private Integer hasStock;

    private String skuPrice;

    private List<Long> brandId;

    private List<String> attrs;

    private Integer pageNum = 1;

    private String _queryString; //url后面的条件字符串


}
