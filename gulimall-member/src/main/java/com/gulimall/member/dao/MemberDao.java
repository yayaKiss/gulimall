package com.gulimall.member.dao;

import com.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 13:02:20
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
