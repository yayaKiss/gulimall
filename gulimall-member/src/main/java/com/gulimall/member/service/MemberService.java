package com.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gulimall.common.utils.PageUtils;
import com.gulimall.member.entity.MemberEntity;
import com.gulimall.member.vo.MemberLoginVo;
import com.gulimall.member.vo.MemberRegisterVo;
import com.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 13:02:20
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegisterVo vo);

    void checkPhoneUnique(String phone);

    void checkUserNameUnique(String userName);

    MemberEntity login(MemberLoginVo vo);

    MemberEntity oauthlogin(SocialUser socialUser) throws Exception;
}

