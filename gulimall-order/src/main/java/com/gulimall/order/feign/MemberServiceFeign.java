package com.gulimall.order.feign;

import com.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimall-member")
public interface MemberServiceFeign {

    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
    public List<MemberAddressVo> getMemberAddress(@PathVariable("memberId") Long memberId);
}
