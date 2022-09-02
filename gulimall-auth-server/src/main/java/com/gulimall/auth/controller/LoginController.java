package com.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;

import com.gulimall.auth.feign.MemberServiceFeign;
import com.gulimall.auth.feign.ThirdPartFeignService;
import com.gulimall.auth.vo.UserLoginVo;
import com.gulimall.auth.vo.UserRegisterVo;
import com.gulimall.common.constant.AuthServerConstant;
import com.gulimall.common.exception.BizCodeEnum;
import com.gulimall.common.utils.R;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MemberServiceFeign memberServiceFeign;

    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone){
        //TODO 60s内防止再刷
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)){
            Long l =Long.parseLong(redisCode.split("_")[1]) ;
            if(System.currentTimeMillis() - l < 60000){
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        String code = UUID.randomUUID().toString().substring(0, 5) + "_" +System.currentTimeMillis();
        thirdPartFeignService.sendCode(phone,code.split("_")[0]);

        //redis进行缓存
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone,code,10, TimeUnit.MINUTES);
        return R.ok();
    }

    /**
     *
     * @param vo
     * @param result
     * @param model
     * @param redirectAttributes 重定向携带数据，利用session原理
     * @return
     */
    @PostMapping("/regist")
    @Transactional
    public String regist(@Valid UserRegisterVo vo, BindingResult result, Model model, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(fieldError -> {
                return fieldError.getField();
            }, fieldError -> {
                return fieldError.getDefaultMessage();
            }));
//            model.addAttribute("errors",errors);
            redirectAttributes.addFlashAttribute("errors",errors);

            //检测出错，转发到注册页(路径映射都是get请求的),直接渲染
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //真正注册(校验通过，开始判断校验码的正确性)，调用远程服务
        //1、校验验证码
        String code = vo.getCode();
        String s = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            String redisCode = s.split("_")[0];
            if(code.equals(redisCode)){
                //验证码通过,redis删除验证码
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //远程调用会员服务进行保存信息
                R r = memberServiceFeign.regist(vo);
                if(r.getCode() == 0){
                    //注册成功，重定向到首页
                    return "redirect:http://auth.gulimall.com/login.html";
                }else{
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            }else{
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                //检测出错，转发到注册页(路径映射都是get请求的),直接渲染
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }
        //验证码长时间不用，过期
        else{
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            //检测出错，转发到注册页(路径映射都是get请求的),直接渲染
            return "redirect:http://auth.gulimall.com/reg.html";
        }

    }

    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes){
        R r = memberServiceFeign.login(vo);
        if(r.getCode() != 0){
            Map<String,String> errors = new HashMap<>();
            errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
        return "redirect:http://gulimall.com";
    }

}
