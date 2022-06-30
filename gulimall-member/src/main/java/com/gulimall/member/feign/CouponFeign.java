package com.gulimall.member.feign;

import com.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 使用openfeign步骤：
 *      1）引入openfeign的依赖
 *      2）声明一个远程接口调用 @FeignClient（“服务名”），@RequestMapping的路径要是服务中的全路径
 *      3）由于nacos不再集成ribbon，所以导入loadbalance依赖，不然报错
 */
@FeignClient("gulimall-coupon")
public interface CouponFeign {

    @RequestMapping("/coupon/coupon/member/list")
    public R memberCoupons();
}
