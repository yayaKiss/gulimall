package com.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    BigDecimal fare;
    MemberAddressVo memberAddressVo;
}
