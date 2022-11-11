package com.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;


import com.gulimall.common.exception.BizCodeEnum;
import com.gulimall.member.exception.PhoneExistException;
import com.gulimall.member.exception.UserNameExistException;
import com.gulimall.member.feign.CouponFeign;
import com.gulimall.member.vo.MemberLoginVo;
import com.gulimall.member.vo.MemberRegisterVo;
import com.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.gulimall.member.entity.MemberEntity;
import com.gulimall.member.service.MemberService;
import com.gulimall.common.utils.PageUtils;
import com.gulimall.common.utils.R;



/**
 * 会员
 *
 * @author lijie
 * @email lijie@gmail.com
 * @date 2022-06-20 13:02:20
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeign couponFeign;


    /**
     * 登录
     */
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegisterVo vo){
        try{
            memberService.regist(vo);
        }catch (UserNameExistException e){
            return R.error(BizCodeEnum.USERNAME_EXIST_EXCEPTION.getCode(), BizCodeEnum.USERNAME_EXIST_EXCEPTION.getMsg());
        }catch (PhoneExistException e){
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }

        return R.ok();
    }

    @PostMapping("/auth2/oauthlogin")
    public R oauthlogin(@RequestBody SocialUser socialUser) throws Exception {
        MemberEntity entity = memberService.oauthlogin(socialUser);
        if(entity == null){
            return R.error(BizCodeEnum.USERNAME_PASSWORD_ERROR.getCode(), BizCodeEnum.USERNAME_PASSWORD_ERROR.getMsg());
        }
        R r = new R();
        r.put("data",entity);
        return r;
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){
        MemberEntity entity = memberService.login(vo);
        if(entity == null){
            return R.error(BizCodeEnum.USERNAME_PASSWORD_ERROR.getCode(), BizCodeEnum.USERNAME_PASSWORD_ERROR.getMsg());
        }
        return R.ok();
    }


    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R memberCoupons = couponFeign.memberCoupons();
        return R.ok().put("member",memberEntity).put("coupons",memberCoupons);
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
