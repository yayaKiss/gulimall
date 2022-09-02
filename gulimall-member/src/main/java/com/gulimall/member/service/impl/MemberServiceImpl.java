package com.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gulimall.common.utils.HttpUtils;
import com.gulimall.member.dao.MemberLevelDao;
import com.gulimall.member.entity.MemberLevelEntity;
import com.gulimall.member.exception.PhoneExistException;
import com.gulimall.member.exception.UserNameExistException;
import com.gulimall.member.vo.MemberLoginVo;
import com.gulimall.member.vo.MemberRegisterVo;
import com.gulimall.member.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.member.dao.MemberDao;
import com.gulimall.member.entity.MemberEntity;
import com.gulimall.member.service.MemberService;



@Service("memberService")
@Slf4j
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        //设置基本信息
        //1、检测用户名和手机号是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        //检测通过，设置基本信息
        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());
        //密码进行加密再进行存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        memberEntity.setCreateTime(new Date());

        //设置会员登记信息
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(levelEntity.getId());

        this.save(memberEntity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        Long count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count > 0 ){
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistException{
        Long count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(count > 0 ){
            throw new UserNameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", vo.getLoginAcct()).or().eq("mobile", vo.getLoginAcct()));
        if (memberEntity == null){
            //没有查到该数据
            return null;
        }else{
            String passwordDb = memberEntity.getPassword();
            String password = vo.getPassword();

            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if(matches){
                return memberEntity;
            }else{
                //查到了该数据，但密码错误
                return null;
            }

        }
    }

    @Override
    public MemberEntity oauthlogin(SocialUser socialUser) throws Exception {
        String uid = socialUser.getUid();
        MemberEntity entity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(entity == null) {
            //给用户创建一个账号
            MemberEntity memberEntity = new MemberEntity();
            //获取用户的微博信息
            try {
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUser.getAccess_token());
                query.put("uid", socialUser.getUid());
                HttpResponse userResponse = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if (userResponse.getStatusLine().getStatusCode() == 200) {
                    log.info("userResponse信息：{}", userResponse);
                    //查询成功
                    String s = EntityUtils.toString(userResponse.getEntity());
                    JSONObject jsonObject = JSON.parseObject(s);
                    String name = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");

                    //设置信息
                    memberEntity.setNickname(name);
                    memberEntity.setGender("m".equalsIgnoreCase(gender) ? 1 : 0);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            memberEntity.setCreateTime(new Date());
            memberEntity.setSocialUid(socialUser.getUid());
            this.baseMapper.insert(memberEntity);
            return memberEntity;
        }
        return entity;
    }

}