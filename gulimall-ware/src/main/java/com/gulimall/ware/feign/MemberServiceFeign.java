package com.gulimall.ware.feign;

import com.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-member")
public interface MemberServiceFeign {

    @RequestMapping("/member/memberreceiveaddress/info/{id}")
    public R addInfo(@PathVariable("id") Long id);
}
