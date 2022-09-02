package com.example.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.search.config.ElasticSearchConfig;
import com.example.search.constant.EsConstant;
import com.example.search.feign.ProductFeignService;
import com.example.search.service.MallSearchService;
import com.example.search.vo.AttrResponseVo;
import com.example.search.vo.BrandVo;
import com.example.search.vo.SearchParam;
import com.example.search.vo.SearchResult;
import com.gulimall.common.to.es.SkuEsModel;
import com.gulimall.common.utils.R;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam searchParam) {
        //动态构建DSL语句

        SearchResult result = null;
        //1、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        try {
            //2、执行检索请求
            SearchResponse response = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);

            //3、分析响应数据
            result = buildSearchResult(response,searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 将检索请求返回结果封装成对应Vo
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        SearchHits hits = response.getHits();

        List<SkuEsModel> products = new ArrayList<>();
        if(hits.getHits() != null || hits.getHits().length > 0){
            //设置所有返回的商品信息
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel product = JSON.parseObject(sourceAsString, SkuEsModel.class);
                //是否有关键字搜索，有 -》 高亮,替换skuTitle
                if(!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    product.setSkuTitle(string);
                }
                products.add(product);
            }
        }
        result.setProduct(products);

        //设置所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //设置品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //设置品牌名
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            //设置品牌Img
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);

        //设置所有分类信息
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        for (Terms.Bucket bucket : catalog_agg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //获取分类id
            long catalogId = bucket.getKeyAsNumber().longValue();
            //获取分类名字
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();

            catalogVo.setCatalogId(catalogId);
            catalogVo.setCatalogName(catalogName);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //设置所有属性信息
        List<SearchResult.AttrVo> attrs = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //设置attrId
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            //设置attrName
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            //设置attrValue
            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValues = new ArrayList<>();
            for (Terms.Bucket attr_value_aggBucket : attr_value_agg.getBuckets()) {
                String attrValue = attr_value_aggBucket.getKeyAsString();
                attrValues.add(attrValue);
            }
            attrVo.setAttrValue(attrValues);

            attrs.add(attrVo);
        }

        result.setAttrs(attrs);

        //设置总记录数
        long value = hits.getTotalHits().value;
        result.setTotal(value);
        //设置当前页码
        result.setPageNum(param.getPageNum());
        //设置总页码  --> 计算 = 总记录数/页码大小
        Integer totalPages =  (int) value % EsConstant.PRODUCT_PAGENUMSIZE == 0? (int)value/EsConstant.PRODUCT_PAGENUMSIZE : ((int)value/EsConstant.PRODUCT_PAGENUMSIZE + 1);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //设置面包屑导航功能(传递属性参数：attrs=9_苹果M1  )
        if(param.getAttrs() != null && param.getAttrs().size() > 0){
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                //attrs=2_5寸:6寸
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                result.getAttrIds().add(Long.parseLong(s[0]));
                R r = productFeignService.info(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    String attr1 = JSON.toJSONString(r.get("attr"));
                    AttrResponseVo data = JSON.parseObject(attr1, new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                } else {
                    navVo.setNavName(s[0]);
                }
                //取消了这个面包屑以后，要跳转的页面
                //将当前属性条件去掉
                String replace = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(collect);
        }

        if(param.getBrandId() != null && param.getBrandId().size() > 0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            //远程查询

            R r = productFeignService.brandsInfo(param.getBrandId());
            if(r.getCode() == 0){
                List<BrandVo> brands = r.getData("brands", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer sb = new StringBuffer();
                String replace = "";
                for (BrandVo brand : brands) {
                    sb.append(brand.getName()+";");
                    replace = replaceQueryString(param, brand.getBrandId() + "", "brandId");
                }
                navVo.setNavValue(sb.toString());
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            }
            navs.add(navVo);
            result.setNavs(navs);
        }

        return result;
    }

    private String replaceQueryString(SearchParam param, String attr,String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(attr,"UTF-8");
            encode.replace("+","%20");  //浏览器对空格的编码和Java不一样，差异化处理
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&"+key +"=" + encode, "");
        return replace;
    }

    /**
     * 构建检索请求
     * 模糊匹配，过滤(属性，分类，品牌，价格区间，库存)，排序，分页，高亮，聚合分析
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        //构建DSL语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /**
         * 模糊匹配，过滤(属性，分类，品牌，价格区间，库存)   ---->  查询
         */
        //1、构建bool-query查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1、  must -模糊匹配
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",searchParam.getKeyword()));
        }
        //1.2、filter  //按品牌Id查询
        if(null != searchParam.getBrandId() && searchParam.getBrandId().size() >0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",searchParam.getBrandId()));
        }

        //1.2、filter  //按三级分类查询
        if(searchParam.getCatalog3Id() != null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",searchParam.getCatalog3Id()));
        }

        //1.2、filter  //按价格区间查询
        if(!StringUtils.isEmpty(searchParam.getSkuPrice())){
            //0_500  _500   500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");

            String[] s = searchParam.getSkuPrice().split("_");
            if(s.length == 2){
                rangeQuery.gte(s[0]).lte(s[1]);
            }else if(s.length == 1){
                if(searchParam.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }
                if(searchParam.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }

            boolQuery.filter(rangeQuery);
        }

        //1.2、filter  //按是否有库存查询
        if(searchParam.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",searchParam.getHasStock() == 1));
        }

        //1.2、filter  //按所指定的属性查询
        if(searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0){
            searchParam.getAttrs().forEach(item -> {
                //attrs=1_5寸:8寸&2_16G:8G
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


                //attrs=1_5寸:8寸
                String[] s = item.split("_");
                String attrId=s[0];
                String[] attrValues = s[1].split(":");//这个属性检索用的值
                boolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                boolQueryBuilder.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));

                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs",boolQueryBuilder, ScoreMode.None);
                boolQuery.filter(nestedQueryBuilder);
            });
        }

        sourceBuilder.query(boolQuery);

        /**
         * 排序，分页，高亮
         */
        //2.1、排序
        //   sort=hotScore_asc/desc
        if(!StringUtils.isEmpty(searchParam.getSort())){
            String[] s = searchParam.getSort().split("_");

            SortOrder order = s[1].equalsIgnoreCase("asc")?SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0],order);
        }

        //2.2、分页
        sourceBuilder.from((searchParam.getPageNum()-1) * EsConstant.PRODUCT_PAGENUMSIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGENUMSIZE);

        //2.3、高亮
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style= 'color:red'>");
            highlightBuilder.postTags("</b>");

            sourceBuilder.highlighter(highlightBuilder);
        }



        /**
         * 聚合分析
         */

        //1. 按照品牌进行聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);


        //1.1 品牌的子聚合-品牌名聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg")
                .field("brandName").size(1));
        //1.2 品牌的子聚合-品牌图片聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg")
                .field("brandImg").size(1));

        sourceBuilder.aggregation(brand_agg);

        //2. 按照分类信息进行聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(20);

        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));

        sourceBuilder.aggregation(catalog_agg);

        //2. 按照属性信息进行聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //2.1 按照属性ID进行聚合
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        attr_agg.subAggregation(attr_id_agg);
        //2.1.1 在每个属性ID下，按照属性名进行聚合
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //2.1.1 在每个属性ID下，按照属性值进行聚合
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        sourceBuilder.aggregation(attr_agg);

        System.out.println("构建的DSL语句:" + sourceBuilder.toString());

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder );

        return searchRequest;
    }
}
