package com.gulimall.ware.feign;

import com.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-order")
public interface OrderServiceFeign {

    @GetMapping("/order/order/getOrder/{orderSn}")
    public R getOrderByOrderSn(@PathVariable("orderSn") String orderSn);
}
