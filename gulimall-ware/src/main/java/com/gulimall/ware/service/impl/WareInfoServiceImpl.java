package com.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.gulimall.common.utils.R;
import com.gulimall.ware.feign.MemberServiceFeign;
import com.gulimall.ware.vo.FareVo;
import com.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.ware.dao.WareInfoDao;
import com.gulimall.ware.entity.WareInfoEntity;
import com.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    private MemberServiceFeign memberServiceFeign;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("id",key)
                    .or().like("name",key)
                    .or().like("address",key)
                    .or().like("areacode",key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {
        R r = memberServiceFeign.addInfo(addrId);
        FareVo fareVo = new FareVo();
        MemberAddressVo addressInfo = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>(){});
        if(addressInfo != null){
            String phone = addressInfo.getPhone();
            String fare = phone.substring(phone.length() - 1);
            fareVo.setFare(new BigDecimal(fare));
            fareVo.setMemberAddressVo(addressInfo);
            return fareVo;
        }
       return null;
    }
}