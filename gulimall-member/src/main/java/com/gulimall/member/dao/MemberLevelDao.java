package com.gulimall.member.dao;

import com.gulimall.member.entity.MemberLevelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级
 * 
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 13:02:20
 */
@Mapper
public interface MemberLevelDao extends BaseMapper<MemberLevelEntity> {


    MemberLevelEntity getDefaultLevel();
}
